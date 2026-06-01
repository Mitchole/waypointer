package com.waypointer.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;

// Low-level atomic-write + .bak recipe for a single JSON file. Callers own path resolution
// (profile slots, fixed names) and any codec/corruption logic; this only performs the I/O:
// temp-write -> ATOMIC_MOVE (with non-atomic fallback) -> refresh backup, and a raw read.
@Slf4j
final class AtomicJsonFile
{
    private AtomicJsonFile() {}

    // Writes payload to tmp, atomically renames it onto primary, then refreshes backup as a
    // best-effort copy. On failure the temp file is cleaned up and false is returned.
    static boolean write(Path tmp, Path primary, Path backup, String payload)
    {
        try
        {
            Files.writeString(tmp, payload, StandardCharsets.UTF_8);
            try
            {
                Files.move(tmp, primary,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(tmp, primary, StandardCopyOption.REPLACE_EXISTING);
            }
            try
            {
                Files.copy(primary, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                log.warn("Could not refresh backup {}", backup, e);
            }
            return true;
        }
        catch (IOException e)
        {
            log.warn("Failed to write {}", primary, e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            return false;
        }
    }

    // Reads a file as UTF-8, or returns null if it is absent or cannot be read.
    static String tryRead(Path f)
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
}
