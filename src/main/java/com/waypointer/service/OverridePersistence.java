package com.waypointer.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;

// Atomic-rename + .bak persistence for a single JSON file. Mirrors WaypointStorePersistence,
// but the caller supplies an already-serialized payload so this class stays codec-agnostic.
@Slf4j
public class OverridePersistence
{
    private final Path dir;
    private final String fileName;

    public OverridePersistence(Path dir, String fileName)
    {
        this.dir = dir;
        this.fileName = fileName;
        try { Files.createDirectories(dir); }
        catch (IOException e) { log.warn("Could not create override dir {}", dir, e); }
    }

    public Path primary() { return dir.resolve(fileName); }
    public Path backup()  { return dir.resolve(fileName + ".bak"); }

    // Reads primary, falls back to .bak. Returns "" if neither exists or both are unreadable.
    public String loadOrEmpty()
    {
        String primary = tryRead(primary());
        if (primary != null) return primary;
        String backup = tryRead(backup());
        if (backup != null)
        {
            log.warn("Loaded override from backup {}", backup());
            return backup;
        }
        return "";
    }

    private String tryRead(Path f)
    {
        if (!Files.exists(f)) return null;
        try
        {
            return Files.readString(f, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            log.warn("IO failure reading {}", f, e);
            return null;
        }
    }

    // Writes payload to a temp file, atomically renames into place, then refreshes .bak.
    public boolean writeBlocking(String payload)
    {
        Path tmp = dir.resolve(fileName + ".tmp");
        try
        {
            Files.writeString(tmp, payload, StandardCharsets.UTF_8);
            try
            {
                Files.move(tmp, primary(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(tmp, primary(), StandardCopyOption.REPLACE_EXISTING);
            }
            try
            {
                Files.copy(primary(), backup(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                log.warn("Could not refresh backup {}", backup(), e);
            }
            return true;
        }
        catch (IOException e)
        {
            log.warn("Failed to write override file {}", primary(), e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            return false;
        }
    }
}
