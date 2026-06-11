package com.waypointer.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UndoBufferTest
{
    @Test
    public void hasUndoableReflectsArmAndClear()
    {
        UndoBuffer u = new UndoBuffer();
        assertFalse(u.hasUndoable());
        u.arm(() -> {});
        assertTrue(u.hasUndoable());
        u.clear();
        assertFalse(u.hasUndoable());
    }

    @Test
    public void runAndClearRunsInverseThenClears()
    {
        UndoBuffer u = new UndoBuffer();
        AtomicInteger ran = new AtomicInteger();
        u.arm(ran::incrementAndGet);
        u.runAndClear();
        assertEquals(1, ran.get());
        assertFalse(u.hasUndoable());
    }

    @Test
    public void runAndClearClearsBeforeRunningSoInverseCanRearm()
    {
        UndoBuffer u = new UndoBuffer();
        // An inverse that itself arms a fresh undo (mimics an op whose inverse fires notifyChanged
        // -> clear, then arms its own). After runAndClear only the re-armed slot survives.
        u.arm(() -> u.arm(() -> {}));
        u.runAndClear();
        assertTrue("inverse re-armed a fresh slot", u.hasUndoable());
    }

    @Test
    public void runAndClearOnEmptyIsNoOp()
    {
        UndoBuffer u = new UndoBuffer();
        u.runAndClear();
        assertFalse(u.hasUndoable());
    }
}
