package com.waypointer.testsupport;

import java.awt.GraphicsEnvironment;
import org.junit.Assume;

/** Shared JUnit assumption for tests that realize Swing windows (pack/setVisible). */
public final class Headless
{
    private Headless() { }

    /** Skips the calling test when there is no display (e.g. headless CI). */
    public static void assumeDisplay()
    {
        Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
    }
}
