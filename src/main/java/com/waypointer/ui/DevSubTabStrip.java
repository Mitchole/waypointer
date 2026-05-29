package com.waypointer.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

final class DevSubTabStrip extends JPanel
{
	enum SubTab { LANDMARKS, PRESETS }

	private final Map<SubTab, JLabel> labels = new EnumMap<>(SubTab.class);
	private final Consumer<SubTab> onSelect;
	private SubTab active = SubTab.LANDMARKS;

	DevSubTabStrip(Consumer<SubTab> onSelect)
	{
		this.onSelect = onSelect;
		setLayout(new GridLayout(1, 2, 0, 0));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		labels.put(SubTab.LANDMARKS, build("Landmarks", SubTab.LANDMARKS));
		labels.put(SubTab.PRESETS,   build("Presets",   SubTab.PRESETS));
		for (JLabel l : labels.values()) add(l);
		applyStyles();
	}

	SubTab getActive() { return active; }
	JLabel labelFor(SubTab t) { return labels.get(t); }

	void setActive(SubTab t)
	{
		if (t == active) return;
		active = t;
		applyStyles();
	}

	private JLabel build(String text, SubTab t)
	{
		JLabel l = new JLabel(text, SwingConstants.CENTER);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		l.setOpaque(true);
		l.setBackground(ColorScheme.DARK_GRAY_COLOR);
		l.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { onSelect.accept(t); }
		});
		return l;
	}

	private void applyStyles()
	{
		for (Map.Entry<SubTab, JLabel> e : labels.entrySet())
		{
			JLabel l = e.getValue();
			boolean isActive = e.getKey() == active;
			l.setForeground(isActive ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			Color underline = isActive
				? ColorScheme.BRAND_ORANGE
				: ColorScheme.DARKER_GRAY_COLOR;
			l.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, underline),
				BorderFactory.createEmptyBorder(4, 0, 4, 0)));
		}
	}
}
