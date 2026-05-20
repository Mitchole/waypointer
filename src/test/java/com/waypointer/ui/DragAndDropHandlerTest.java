package com.waypointer.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class DragAndDropHandlerTest
{
    @Test
    public void moveInsertsBeforeTarget()
    {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(a, b, c, d);
        List<UUID> moved = DragAndDropHandler.move(new ArrayList<>(ids), a, c);
        assertEquals(Arrays.asList(b, a, c, d), moved);
    }

    @Test
    public void moveBackwardInsertsBeforeTarget()
    {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(a, b, c, d);
        List<UUID> moved = DragAndDropHandler.move(new ArrayList<>(ids), d, b);
        assertEquals(Arrays.asList(a, d, b, c), moved);
    }

    @Test
    public void moveTwoMiddleEntries()
    {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        // Drag b onto c: b is removed from position 1, then inserted before c (now at index 1).
        // Result: [a, b, c, d] -> remove b -> [a, c, d] -> insert before c (index 1) -> [a, b, c, d].
        // That means dragging b before c when b is already just before c is a no-movement case.
        // Instead test: drag c onto b (insert c before b): [a, b, c, d] -> [a, c, b, d].
        List<UUID> result = DragAndDropHandler.move(new ArrayList<>(Arrays.asList(a, b, c, d)), c, b);
        assertEquals(Arrays.asList(a, c, b, d), result);
    }

    @Test
    public void moveFirstBeforeLast()
    {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        // Drag a onto c (insert a before c): [a, b, c] -> remove a -> [b, c] -> insert before c (index 1) -> [b, a, c].
        List<UUID> result = DragAndDropHandler.move(new ArrayList<>(Arrays.asList(a, b, c)), a, c);
        assertEquals(Arrays.asList(b, a, c), result);
    }

    @Test
    public void moveSameAsTargetReturnsNull()
    {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        List<UUID> result = DragAndDropHandler.move(new ArrayList<>(Arrays.asList(a, b)), a, a);
        assertNull(result);
    }

    @Test
    public void missingIdReturnsNullSentinelForCallerToNoOp()
    {
        UUID present = UUID.randomUUID();
        UUID absent = UUID.randomUUID();
        assertNull(DragAndDropHandler.move(new ArrayList<>(Arrays.asList(present)), present, absent));
        assertNull(DragAndDropHandler.move(new ArrayList<>(Arrays.asList(present)), absent, present));
    }

    @Test
    public void inputListNotMutated()
    {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        List<UUID> input = new ArrayList<>(Arrays.asList(a, b));
        DragAndDropHandler.move(new ArrayList<>(input), a, b);
        // Verify original list is untouched.
        assertEquals(a, input.get(0));
        assertEquals(b, input.get(1));
    }
}
