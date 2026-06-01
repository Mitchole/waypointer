package com.waypointer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

// Atomic-rename + .bak persistence for a single JSON file. The caller supplies an
// already-serialized payload so this class stays codec-agnostic. Delegates the write/read
// recipe to AtomicJsonFile.
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
        String primary = AtomicJsonFile.tryRead(primary());
        if (primary != null) return primary;
        String backup = AtomicJsonFile.tryRead(backup());
        if (backup != null)
        {
            log.warn("Loaded override from backup {}", backup());
            return backup;
        }
        return "";
    }

    // Writes payload to a temp file, atomically renames into place, then refreshes .bak.
    public boolean writeBlocking(String payload)
    {
        return AtomicJsonFile.write(dir.resolve(fileName + ".tmp"), primary(), backup(), payload);
    }
}
