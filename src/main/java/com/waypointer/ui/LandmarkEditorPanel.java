package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointCapture;
import com.waypointer.util.Listeners.Subscription;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
    private final WaypointCapture capture;
    private final AreaPreviewOverlay areaOverlay;
    private final JComboBox<LandmarkType> typePicker = new JComboBox<>(LandmarkType.values());
    private final PlaceholderTextField searchField = new PlaceholderTextField("Search by name");
    private final JButton addBtn = new JButton("+ Add entry");
    private final JButton exportBtn = new JButton("Export changes");
    private final com.waypointer.codec.LandmarkOverridesCodec landmarkOverridesCodec;
    private final JPanel body = new JPanel();
    private JPanel activeInline = null;

    private Subscription bboxSub;

    @Inject
    public LandmarkEditorPanel(BboxIndex bboxIndex, LandmarkOverrides overrides,
        WaypointCapture capture, AreaPreviewOverlay areaOverlay,
        com.waypointer.codec.LandmarkOverridesCodec landmarkOverridesCodec)
    {
        this.bboxIndex = bboxIndex;
        this.overrides = overrides;
        this.capture = capture;
        this.areaOverlay = areaOverlay;
        this.landmarkOverridesCodec = landmarkOverridesCodec;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        Styles.combo(typePicker);
        Styles.textField(searchField);
        Styles.secondaryButton(addBtn);
        Styles.secondaryButton(exportBtn);
        header.add(typePicker);
        header.add(searchField);
        header.add(addBtn);
        header.add(exportBtn);
        add(header, BorderLayout.NORTH);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JScrollPane scroll = new JScrollPane(body,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(scroll, BorderLayout.CENTER);

        typePicker.addActionListener(e -> { closeInline(); rebuild(); });
        searchField.getDocument().addDocumentListener(Styles.documentListener(this::rebuild));
        addBtn.addActionListener(e -> openAdd());

        exportBtn.addActionListener(e -> {
            String json = landmarkOverridesCodec.encode(overrides.getSnapshot());
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(json), null);
            javax.swing.JOptionPane.showMessageDialog(this,
                "Override snapshot copied to clipboard.", "Waypointer",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });

        bboxSub = bboxIndex.subscribe(() -> SwingUtilities.invokeLater(this::rebuild));
        rebuild();
    }

    public void dispose()
    {
        if (bboxSub != null) { bboxSub.close(); bboxSub = null; }
        areaOverlay.setActive(false);
    }

    private void rebuild()
    {
        body.removeAll();
        if (activeInline != null) body.add(activeInline);
        LandmarkType type = (LandmarkType) typePicker.getSelectedItem();
        if (type == null) { body.revalidate(); body.repaint(); return; }
        String query = searchField.getText().toLowerCase();
        for (BboxIndex.Entry e : bboxIndex.bundledOfType(type))
        {
            if (!query.isEmpty() && !e.name.toLowerCase().contains(query)) continue;
            LandmarkRow row = new LandmarkRow(type, e, this::openEdit, this::onDelete);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            body.add(row);
        }
        body.revalidate();
        body.repaint();
    }

    private void openAdd()
    {
        closeInline();
        activeInline = new AddLandmarkPanel(capture, overrides,
            () -> (LandmarkType) typePicker.getSelectedItem(),
            size -> {
                if (size <= 0)
                {
                    areaOverlay.setActive(false);
                }
                else
                {
                    areaOverlay.setSize(size);
                    areaOverlay.setActive(true);
                }
            },
            this::closeInline);
        rebuild();
    }

    private void openEdit(BboxIndex.Entry e)
    {
        closeInline();
        LandmarkType type = (LandmarkType) typePicker.getSelectedItem();
        activeInline = new InlineLandmarkEdit(type, e, overrides, capture, this::closeInline);
        rebuild();
    }

    private void closeInline()
    {
        activeInline = null;
        areaOverlay.setActive(false);
        rebuild();
    }

    private void onDelete(BboxIndex.Entry e)
    {
        overrides.deleteBundledEntry(
            ((LandmarkType) typePicker.getSelectedItem()).name(),
            e.name, e.x1, e.y1, e.x2, e.y2, e.plane);
    }
}
