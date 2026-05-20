package com.waypointer.ui;

import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;

// Eagerly loads pin.png and exposes pre-scaled BufferedImages at the sizes the plugin uses.
// Used as the default waypoint-row icon and the sidebar nav button. Resource load failures
// throw at class init (fail-fast). Asset license: Apache 2.0 (Google Noto Emoji); see LICENSE.
@Slf4j
public final class PinIcon
{
    private static final BufferedImage NATIVE =
        ImageUtil.loadImageResource(PinIcon.class, "/com/waypointer/pin.png");

    private static final BufferedImage SIZE_16 = ImageUtil.resizeImage(NATIVE, 16, 16);
    private static final BufferedImage SIZE_32 = ImageUtil.resizeImage(NATIVE, 32, 32);

    private PinIcon() {}

    public static BufferedImage getNative() { return NATIVE; }
    public static BufferedImage getSize16() { return SIZE_16; }
    public static BufferedImage getSize32() { return SIZE_32; }
}
