package com.waypointer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.waypointer.codec.SnapshotCodec;
import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OverridesStoreCoalesceTest
{
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    // Minimal concrete store over String so the test does not depend on a domain snapshot type.
    private static final class StringOverrides extends OverridesStore<String>
    {
        StringOverrides(Path dir, ScheduledExecutorService scheduler)
        {
            super(dir, "coalesce-test.json", identityCodec(), scheduler, "");
        }

        @Override protected String deepCopy(String src) { return src; }

        void triggerSave() { scheduleSave(); }

        private static SnapshotCodec<String> identityCodec()
        {
            return new SnapshotCodec<String>()
            {
                @Override public String encode(String snapshot) { return snapshot; }
                @Override public String decode(String json) { return json; }
            };
        }
    }

    @Test
    public void burstOfSavesSchedulesExactlyOneFlush() throws Exception
    {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        // Return null and never run the task, so the dirty flag stays set across the burst.
        when(scheduler.schedule(any(Runnable.class), anyLong(), any())).thenReturn(null);

        StringOverrides store = new StringOverrides(tmp.newFolder().toPath(), scheduler);
        for (int i = 0; i < 5; i++) store.triggerSave();

        verify(scheduler, times(1)).schedule(any(Runnable.class), anyLong(), any());
    }
}
