package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class AreaPreviewOverlay extends Overlay
{
	private static final Color FILL = new Color(
		ColorScheme.BRAND_ORANGE.getRed(),
		ColorScheme.BRAND_ORANGE.getGreen(),
		ColorScheme.BRAND_ORANGE.getBlue(), 60);
	private static final Color OUTLINE = ColorScheme.BRAND_ORANGE;

	private final Client client;
	private final WaypointerConfig config;
	private volatile boolean active = false;
	private volatile int size = 3;

	@Inject
	public AreaPreviewOverlay(Client client, WaypointerConfig config)
	{
		this.client = client;
		this.config = config;
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	public void setActive(boolean active) { this.active = active; }
	public void setSize(int size) { this.size = size; }
	public boolean isActive() { return active; }

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!active || !config.devModeEnabled()) return null;
		Player p = client.getLocalPlayer();
		if (p == null) return null;
		WorldPoint base = p.getWorldLocation();
		if (base == null) return null;

		Polygon poly = polyFor(client, base, size);
		if (poly == null) return null;

		g.setColor(FILL);
		g.fillPolygon(poly);
		g.setColor(OUTLINE);
		g.drawPolygon(poly);
		return null;
	}

	private static Polygon polyFor(Client client, WorldPoint base, int size)
	{
		WorldPoint nw = base;
		WorldPoint ne = new WorldPoint(base.getX() + size - 1, base.getY(), base.getPlane());
		WorldPoint se = new WorldPoint(base.getX() + size - 1, base.getY() - (size - 1), base.getPlane());
		WorldPoint sw = new WorldPoint(base.getX(), base.getY() - (size - 1), base.getPlane());
		LocalPoint lnw = LocalPoint.fromWorld(client, nw);
		LocalPoint lne = LocalPoint.fromWorld(client, ne);
		LocalPoint lse = LocalPoint.fromWorld(client, se);
		LocalPoint lsw = LocalPoint.fromWorld(client, sw);
		if (lnw == null || lne == null || lse == null || lsw == null) return null;
		Point pnw = Perspective.localToCanvas(client, lnw, base.getPlane());
		Point pne = Perspective.localToCanvas(client, lne, base.getPlane());
		Point pse = Perspective.localToCanvas(client, lse, base.getPlane());
		Point psw = Perspective.localToCanvas(client, lsw, base.getPlane());
		if (pnw == null || pne == null || pse == null || psw == null) return null;
		Polygon poly = new Polygon();
		poly.addPoint(pnw.getX(), pnw.getY());
		poly.addPoint(pne.getX(), pne.getY());
		poly.addPoint(pse.getX(), pse.getY());
		poly.addPoint(psw.getX(), psw.getY());
		return poly;
	}
}
