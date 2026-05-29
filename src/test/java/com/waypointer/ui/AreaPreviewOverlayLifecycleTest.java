package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertNull;

public class AreaPreviewOverlayLifecycleTest
{
	@Test
	public void inactiveOverlayDoesNotRender()
	{
		Client client = Mockito.mock(Client.class);
		WaypointerConfig cfg = Mockito.mock(WaypointerConfig.class);
		Mockito.when(cfg.devModeEnabled()).thenReturn(true);
		AreaPreviewOverlay o = new AreaPreviewOverlay(client, cfg);

		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Dimension d = o.render(g);
		assertNull(d);
		Mockito.verify(client, Mockito.never()).getLocalPlayer();
	}

	@Test
	public void activeButDevOffDoesNotRender()
	{
		Client client = Mockito.mock(Client.class);
		WaypointerConfig cfg = Mockito.mock(WaypointerConfig.class);
		Mockito.when(cfg.devModeEnabled()).thenReturn(false);
		AreaPreviewOverlay o = new AreaPreviewOverlay(client, cfg);
		o.setActive(true);

		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Dimension d = o.render(img.createGraphics());
		assertNull(d);
		Mockito.verify(client, Mockito.never()).getLocalPlayer();
	}

	@Test
	public void activeAndDevOnButNullPlayerEarlyReturns()
	{
		Client client = Mockito.mock(Client.class);
		WaypointerConfig cfg = Mockito.mock(WaypointerConfig.class);
		Mockito.when(cfg.devModeEnabled()).thenReturn(true);
		Mockito.when(client.getLocalPlayer()).thenReturn(null);
		AreaPreviewOverlay o = new AreaPreviewOverlay(client, cfg);
		o.setActive(true);

		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Dimension d = o.render(img.createGraphics());
		assertNull(d);
	}
}
