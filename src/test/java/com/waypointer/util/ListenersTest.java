package com.waypointer.util;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ListenersTest
{
    @Test public void addAndFireInvokesEachListener()
    {
        Listeners listeners = new Listeners();
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        listeners.subscribe(a::incrementAndGet);
        listeners.subscribe(b::incrementAndGet);

        listeners.fire();
        listeners.fire();

        assertEquals(2, a.get());
        assertEquals(2, b.get());
    }

    @Test public void subscribeRegistersAndCloseUnregisters()
    {
        Listeners listeners = new Listeners();
        AtomicInteger calls = new AtomicInteger();
        Listeners.Subscription sub = listeners.subscribe(calls::incrementAndGet);

        listeners.fire();
        assertEquals("registered listener fires", 1, calls.get());

        sub.close();
        listeners.fire();
        assertEquals("closed Subscription stops listener firing", 1, calls.get());
    }

    @Test public void throwingListenerDoesNotStopOthers()
    {
        Listeners listeners = new Listeners();
        AtomicInteger after = new AtomicInteger();
        listeners.subscribe(() -> { throw new RuntimeException("boom"); });
        listeners.subscribe(after::incrementAndGet);

        listeners.fire();

        assertEquals("listener after the throwing one still fires", 1, after.get());
    }

    @Test public void closeIsIdempotent()
    {
        Listeners listeners = new Listeners();
        AtomicInteger calls = new AtomicInteger();
        Listeners.Subscription sub = listeners.subscribe(calls::incrementAndGet);

        sub.close();
        sub.close();  // Second close is a no-op (remove on an already-removed lambda).

        listeners.fire();
        assertEquals(0, calls.get());
    }
}
