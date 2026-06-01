package com.waypointer.service;

/**
 * A persistence target that {@link DebouncedSaver} can drive without knowing the concrete model.
 * {@code serialize} runs on the caller's (mutation-owning) thread; {@code writeBlocking} runs on
 * the scheduler thread; {@code saveBlocking} does both back-to-back for the flush path.
 */
interface JsonSnapshotSink<T>
{
    String serialize(T value);

    boolean writeBlocking(String json);

    boolean saveBlocking(T value);
}
