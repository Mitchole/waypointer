package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import org.junit.Test;

public class PinIconTest
{
    @Test
    public void nativeImageLoads()
    {
        BufferedImage img = PinIcon.getNative();
        assertNotNull("pin.png resource must be on classpath", img);
        assertEquals("native width should be 128", 128, img.getWidth());
        assertEquals("native height should be 128", 128, img.getHeight());
    }

    @Test
    public void size16IsScaled()
    {
        BufferedImage img = PinIcon.getSize16();
        assertNotNull(img);
        assertEquals(16, img.getWidth());
        assertEquals(16, img.getHeight());
    }

    @Test
    public void size32IsScaled()
    {
        BufferedImage img = PinIcon.getSize32();
        assertNotNull(img);
        assertEquals(32, img.getWidth());
        assertEquals(32, img.getHeight());
    }

    @Test
    public void cachedReferences()
    {
        // Repeated calls must return the same cached BufferedImage instance.
        assertNotNull(PinIcon.getSize16());
        BufferedImage a = PinIcon.getSize16();
        BufferedImage b = PinIcon.getSize16();
        // identity check: caller must not get a fresh scaled image each time
        org.junit.Assert.assertSame(a, b);
    }
}
