package com.waypointer.service;

/**
 * Single-slot undo buffer holding the inverse of the most recent destructive op. The store arms it
 * on destructive mutations and clears it on every other mutation, so "no undo" is the default.
 */
final class UndoBuffer
{
    private Runnable slot;

    void arm(Runnable inverse) { this.slot = inverse; }

    void clear() { this.slot = null; }

    boolean hasUndoable() { return slot != null; }

    /** Clears the slot first, then runs the inverse, so an inverse that re-fires cannot re-clear itself. */
    void runAndClear()
    {
        Runnable u = slot;
        slot = null;
        if (u != null) u.run();
    }
}
