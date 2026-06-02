package com.waypointer.ui;

import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;

/**
 * Shared Point/Area + size + name controls for the landmark Add and Edit panels. Owns the
 * NxN label wiring and the area corner arithmetic; the parent panel supplies its own button
 * row and commit lambda. Slider bounds (2-10) match across both call sites so point/area
 * capture behaves identically.
 */
final class LandmarkCaptureControls
{
    private static final int MIN_SIZE = 2;
    private static final int MAX_SIZE = 10;

    private final JTextField nameField;
    private final JRadioButton pointBtn;
    private final JRadioButton areaBtn;
    private final JSlider sizeSlider;
    private final JLabel sizeLabel;
    private final JPanel header;

    LandmarkCaptureControls(String initialName, boolean initiallyArea, int initialSize)
    {
        int size = clampSize(initialSize);
        nameField = new JTextField(initialName == null ? "" : initialName);
        pointBtn = new JRadioButton("Point", !initiallyArea);
        areaBtn = new JRadioButton("Area", initiallyArea);
        pointBtn.setOpaque(false);
        areaBtn.setOpaque(false);
        sizeSlider = new JSlider(MIN_SIZE, MAX_SIZE, size);
        sizeSlider.setOpaque(false);
        sizeLabel = new JLabel(size + "x" + size);

        ButtonGroup g = new ButtonGroup();
        g.add(pointBtn);
        g.add(areaBtn);

        header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        header.add(pointBtn);
        header.add(areaBtn);
        header.add(sizeSlider);
        header.add(sizeLabel);

        Styles.textField(nameField);
    }

    /** Wires the live NxN label and forwards the effective size (0 in Point mode) to {@code listener}. */
    void addSizeListeners(Consumer<Integer> listener)
    {
        sizeSlider.addChangeListener(e -> {
            int n = sizeSlider.getValue();
            sizeLabel.setText(n + "x" + n);
            listener.accept(areaBtn.isSelected() ? n : 0);
        });
        pointBtn.addActionListener(e -> listener.accept(0));
        areaBtn.addActionListener(e -> listener.accept(sizeSlider.getValue()));
    }

    /** {x1,y1,x2,y2} for a capture anchored at (x,y): a point collapses, an area extends by size-1. */
    int[] cornersFor(int x, int y)
    {
        if (!areaBtn.isSelected()) return new int[] {x, y, x, y};
        int n = sizeSlider.getValue();
        return new int[] {x, y, x + n - 1, y + n - 1};
    }

    JPanel header() { return header; }
    JTextField nameField() { return nameField; }
    String getName() { return nameField.getText(); }
    boolean isArea() { return areaBtn.isSelected(); }
    int getSize() { return sizeSlider.getValue(); }

    private static int clampSize(int n)
    {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, n));
    }
}
