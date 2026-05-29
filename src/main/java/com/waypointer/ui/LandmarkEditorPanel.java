package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.LandmarkType;
import com.waypointer.util.Listeners.Subscription;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;

@Singleton
public class LandmarkEditorPanel extends JPanel
{
    private final BboxIndex bboxIndex;
    private final LandmarkOverrides overrides;
    private final JComboBox<LandmarkType> typePicker = new JComboBox<>(LandmarkType.values());
    private final PlaceholderTextField searchField = new PlaceholderTextField("Search by name");
    private final JPanel body = new JPanel();

    private Subscription bboxSub;

    @Inject
    public LandmarkEditorPanel(BboxIndex bboxIndex, LandmarkOverrides overrides)
    {
        this.bboxIndex = bboxIndex;
        this.overrides = overrides;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        Styles.combo(typePicker);
        Styles.textField(searchField);
        header.add(typePicker);
        header.add(searchField);
        add(header, BorderLayout.NORTH);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JScrollPane scroll = new JScrollPane(body,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(scroll, BorderLayout.CENTER);

        typePicker.addActionListener(e -> rebuild());
        searchField.getDocument().addDocumentListener(Styles.documentListener(this::rebuild));

        bboxSub = bboxIndex.subscribe(() -> SwingUtilities.invokeLater(this::rebuild));
        rebuild();
    }

    public void dispose()
    {
        if (bboxSub != null) { bboxSub.close(); bboxSub = null; }
    }

    private void rebuild()
    {
        body.removeAll();
        LandmarkType type = (LandmarkType) typePicker.getSelectedItem();
        if (type == null) { body.revalidate(); body.repaint(); return; }
        String query = searchField.getText().toLowerCase();
        for (BboxIndex.Entry e : bboxIndex.bundledOfType(type))
        {
            if (!query.isEmpty() && !e.name.toLowerCase().contains(query)) continue;
            LandmarkRow row = new LandmarkRow(type, e, this::onDelete);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            body.add(row);
        }
        body.revalidate();
        body.repaint();
    }

    private void onDelete(BboxIndex.Entry e)
    {
        overrides.deleteBundledEntry(
            ((LandmarkType) typePicker.getSelectedItem()).name(),
            e.name, e.x1, e.y1, e.x2, e.y2, e.plane);
    }
}
