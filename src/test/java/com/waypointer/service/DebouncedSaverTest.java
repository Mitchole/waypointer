package com.waypointer.service;

import com.waypointer.model.Library;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DebouncedSaverTest
{
    private WaypointStorePersistence persistence;
    private ScheduledExecutorService scheduler;

    @Before
    public void setUp()
    {
        persistence = mock(WaypointStorePersistence.class);
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @After
    public void tearDown()
    {
        scheduler.shutdownNow();
    }

    @Test
    public void flushWritesCurrentLibrarySynchronously()
    {
        Library lib = new Library();
        when(persistence.saveBlocking(lib)).thenReturn(true);
        DebouncedSaver saver = new DebouncedSaver(
            persistence, scheduler, Duration.ofMillis(500), () -> lib);

        saver.flush();

        verify(persistence).saveBlocking(lib);
        verify(persistence, never()).serialize(any());
    }

    @Test
    public void scheduleSerializesOnCallingThreadThenWritesAfterDelay()
    {
        Library lib = new Library();
        when(persistence.serialize(lib)).thenReturn("JSON");
        when(persistence.writeBlocking("JSON")).thenReturn(true);
        DebouncedSaver saver = new DebouncedSaver(
            persistence, scheduler, Duration.ofMillis(20), () -> lib);

        saver.schedule();
        // The snapshot is taken on the calling thread; the disk write is deferred.
        verify(persistence).serialize(lib);
        verify(persistence, never()).writeBlocking(anyString());

        verify(persistence, timeout(1000)).writeBlocking("JSON");
    }

    @Test
    public void rapidSchedulesCollapseToOneWriteOfTheLastSnapshot()
    {
        Library lib = new Library();
        when(persistence.serialize(lib)).thenReturn("A", "B", "C");
        when(persistence.writeBlocking(anyString())).thenReturn(true);
        DebouncedSaver saver = new DebouncedSaver(
            persistence, scheduler, Duration.ofMillis(40), () -> lib);

        saver.schedule();
        saver.schedule();
        saver.schedule();

        // Each schedule cancels the prior pending write, so only the last snapshot lands.
        verify(persistence, timeout(1000)).writeBlocking("C");
        verify(persistence, never()).writeBlocking("A");
        verify(persistence, never()).writeBlocking("B");
    }

    @Test
    public void cancelPendingPreventsTheDeferredWrite() throws InterruptedException
    {
        Library lib = new Library();
        when(persistence.serialize(lib)).thenReturn("JSON");
        DebouncedSaver saver = new DebouncedSaver(
            persistence, scheduler, Duration.ofMillis(60), () -> lib);

        saver.schedule();
        saver.cancelPending();

        // Let the (cancelled) window elapse; the write must never fire.
        Thread.sleep(150);
        verify(persistence, never()).writeBlocking(anyString());
    }
}
