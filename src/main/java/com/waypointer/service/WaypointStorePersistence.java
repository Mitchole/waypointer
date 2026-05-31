package com.waypointer.service;

import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Library;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

// Atomic load/save for the library JSON. Falls back to a .bak on primary corruption.
// If both files are unreadable, returns an empty library and refuses to overwrite either
// file until the caller explicitly resets. Save splits into serialize() (snapshot on the
// caller's thread) and writeBlocking() (I/O, safe on a scheduler thread). saveBlocking()
// does both back-to-back.
@Slf4j
@Singleton
public class WaypointStorePersistence
{
    static final String LIBRARY_FILENAME = "library.json";
    static final String BACKUP_FILENAME = "library.json.bak";

    private final Path dir;
    private final LibraryJsonCodec codec;
    private volatile boolean refuseSavesUntilReset = false;
    private volatile String activeProfileKey = null; // null == default slot (library.json)

    @Inject
    public WaypointStorePersistence(LibraryJsonCodec codec)
    {
        this(RuneLite.RUNELITE_DIR.toPath().resolve("waypointer"), codec);
    }

    public WaypointStorePersistence(Path dir, LibraryJsonCodec codec)
    {
        this.dir = dir;
        this.codec = codec;
        try { Files.createDirectories(dir); }
        catch (IOException e) { log.warn("Could not create waypointer dir {}", dir, e); }
    }

    public Path getDir() { return dir; }
    public String getActiveProfileKey() { return activeProfileKey; }
    public Path libraryFile() { return dir.resolve(fileNameFor(activeProfileKey)); }
    public Path backupFile() { return dir.resolve(fileNameFor(activeProfileKey) + ".bak"); }

    // Default slot keeps the legacy filename; per-account slots are library-<sanitized-key>.json.
    static String fileNameFor(String key)
    {
        return key == null ? LIBRARY_FILENAME : "library-" + sanitizeKey(key) + ".json";
    }

    // Dots are intentionally preserved: real RS profile keys contain them (e.g. rsprofile.12345.STANDARD).
    static String sanitizeKey(String key)
    {
        return key.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    // Retarget which slot subsequent loads/saves use. Clears the corrupt-state freeze so a bad
    // file in one slot never blocks saves in another; the new slot re-evaluates on its own load.
    public void switchProfile(String key)
    {
        this.activeProfileKey = key;
        this.refuseSavesUntilReset = false;
    }

    // Copy the default slot's files into the active slot when the active slot has no primary file
    // yet. No-op for the default slot, when the slot already exists, or when the default is absent
    // (brand-new user) -> the slot then loads empty.
    public void seedFromDefault()
    {
        if (activeProfileKey == null) return;
        Path slot = libraryFile();
        if (Files.exists(slot)) return;
        Path defaultPrimary = dir.resolve(LIBRARY_FILENAME);
        if (!Files.exists(defaultPrimary)) return;
        try
        {
            Files.copy(defaultPrimary, slot, StandardCopyOption.REPLACE_EXISTING);
            Path defaultBackup = dir.resolve(BACKUP_FILENAME);
            if (Files.exists(defaultBackup))
            {
                Files.copy(defaultBackup, backupFile(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            log.warn("Could not seed profile library from default slot", e);
        }
    }

    public boolean isRefusingSaves() { return refuseSavesUntilReset; }

    public void allowSavesAfterReset()
    {
        refuseSavesUntilReset = false;
    }

    // Loads primary; on parse failure tries backup; on both failing returns empty and flags
    // refuseSavesUntilReset so we don't clobber the bad files. Transient IO errors do not
    // trigger refuse-saves; only confirmed parse corruption does.
    public Library loadOrEmpty()
    {
        LoadAttempt primary = tryLoad(libraryFile());
        if (primary.lib != null) return primary.lib;

        log.warn("Primary library file unreadable; trying backup at {}", backupFile());
        LoadAttempt backup = tryLoad(backupFile());
        if (backup.lib != null)
        {
            log.warn("Loaded library from backup; will overwrite primary on next save");
            return backup.lib;
        }

        // Only freeze saves on parse corruption. Transient IO errors get a clean empty
        // library and a normal save path on the next mutation.
        if (primary.outcome == Outcome.CORRUPT || backup.outcome == Outcome.CORRUPT)
        {
            log.error("Library files have parse corruption; refusing further saves until reset");
            refuseSavesUntilReset = true;
        }
        return new Library();
    }

    private LoadAttempt tryLoad(Path f)
    {
        if (!Files.exists(f)) return LoadAttempt.missing();
        try
        {
            String json = Files.readString(f, StandardCharsets.UTF_8);
            return LoadAttempt.ok(codec.decode(json));
        }
        catch (IOException e)
        {
            log.warn("IO failure reading {}", f, e);
            return LoadAttempt.io();
        }
        catch (RuntimeException e)
        {
            log.warn("Parse failure reading {}: {}", f, e.getMessage());
            return LoadAttempt.corrupt();
        }
    }

    private enum Outcome { OK, MISSING, IO_ERROR, CORRUPT }

    private static final class LoadAttempt
    {
        final Library lib;
        final Outcome outcome;
        private LoadAttempt(Library lib, Outcome outcome) { this.lib = lib; this.outcome = outcome; }
        static LoadAttempt ok(Library l) { return new LoadAttempt(l, Outcome.OK); }
        static LoadAttempt missing() { return new LoadAttempt(null, Outcome.MISSING); }
        static LoadAttempt io() { return new LoadAttempt(null, Outcome.IO_ERROR); }
        static LoadAttempt corrupt() { return new LoadAttempt(null, Outcome.CORRUPT); }
    }

    // Pure transform, no I/O. Caller must be on the thread that owns the library (typically
    // the EDT) so iteration sees a consistent view.
    public String serialize(Library lib)
    {
        return codec.encode(lib);
    }

    // Synchronous write of an already-serialized JSON payload. Atomic where possible. Refuses
    // to write if corruption was detected on load and not yet acknowledged via reset.
    public boolean writeBlocking(String json)
    {
        if (refuseSavesUntilReset)
        {
            log.warn("Save refused: library files are in a corrupt state pending user reset");
            return false;
        }
        // Snapshot the active slot once so a concurrent switchProfile cannot move this save's
        // temp file into a different slot's primary or backup mid-write.
        final String slotKey = activeProfileKey;
        Path tmp = dir.resolve(fileNameFor(slotKey) + ".tmp");
        Path primary = dir.resolve(fileNameFor(slotKey));
        Path backup = dir.resolve(fileNameFor(slotKey) + ".bak");
        try
        {
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try
            {
                Files.move(tmp, primary,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(tmp, primary, StandardCopyOption.REPLACE_EXISTING);
            }
            // Best-effort backup copy after successful primary write.
            try
            {
                Files.copy(primary, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                log.warn("Could not refresh backup file", e);
            }
            return true;
        }
        catch (IOException e)
        {
            log.warn("Failed to save library", e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            return false;
        }
    }

    // Serialize + writeBlocking back-to-back. Only safe on the thread that owns the library.
    // Used by tests and the shutdown flush path.
    public boolean saveBlocking(Library lib)
    {
        return writeBlocking(serialize(lib));
    }
}
