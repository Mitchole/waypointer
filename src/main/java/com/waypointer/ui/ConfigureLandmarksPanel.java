package com.waypointer.ui;

import com.waypointer.service.LandmarkType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Inline picker shown below the {@link NearestLandmarkBar} when the customize button is
 * clicked. One row per landmark type. Each row: drag-handle, checkbox, icon, name.
 * Rows support drag-to-reorder via a Swing {@link TransferHandler} with string-flavor payload.
 */
final class ConfigureLandmarksPanel extends JPanel
{
    private static final int ROW_HEIGHT = 24;
    private static final int DRAG_THRESHOLD_SQ = 25;
    private static final String PAYLOAD_PREFIX = "landmark:";
    private static final DataFlavor STR = DataFlavor.stringFlavor;

    private final SpriteManager spriteManager;
    private final Map<LandmarkType, Integer> spriteIds;
    private final BiConsumer<LandmarkType, Boolean> onToggle;
    private final BiConsumer<Integer, Integer> onReorder;
    private final Runnable onClose;

    private LandmarkSelection selection;

    ConfigureLandmarksPanel(SpriteManager spriteManager,
        Map<LandmarkType, Integer> spriteIds,
        LandmarkSelection initial,
        BiConsumer<LandmarkType, Boolean> onToggle,
        BiConsumer<Integer, Integer> onReorder,
        Runnable onClose)
    {
        this.spriteManager = spriteManager;
        this.spriteIds = spriteIds;
        this.selection = initial;
        this.onToggle = onToggle;
        this.onReorder = onReorder;
        this.onClose = onClose;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setVisible(false);

        rebuild();
    }

    void setSelection(LandmarkSelection s)
    {
        this.selection = s;
        rebuild();
    }

    private void rebuild()
    {
        removeAll();
        add(buildHeader());
        int index = 0;
        for (LandmarkType type : selection.order())
        {
            add(buildRow(type, index));
            index++;
        }
        revalidate();
        repaint();
    }

    // Title row with a close (X) affordance. The picker is otherwise only dismissable by
    // re-clicking the overflow toggle, which isn't obvious -- the X makes it explicit.
    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new java.awt.BorderLayout());
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        header.setPreferredSize(new Dimension(0, ROW_HEIGHT));

        JLabel title = new JLabel("Landmarks");
        title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        title.setFont(FontManager.getRunescapeSmallFont());
        header.add(title, java.awt.BorderLayout.WEST);

        JLabel close = new JLabel("✕"); // U+2715 multiplication X -- close glyph
        close.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        close.setFont(FontManager.getRunescapeFont());
        close.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        close.setToolTipText("Close");
        close.getAccessibleContext().setAccessibleName("Close landmark picker");
        close.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e) { if (onClose != null) onClose.run(); }
        });
        header.add(close, java.awt.BorderLayout.EAST);
        return header;
    }

    private JPanel buildRow(LandmarkType type, int index)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        row.setPreferredSize(new Dimension(0, ROW_HEIGHT));

        JLabel handle = new JLabel("⋮"); // U+22EE VERTICAL ELLIPSIS -- drag-handle glyph
        handle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        handle.setFont(FontManager.getRunescapeFont());
        handle.setPreferredSize(new Dimension(10, ROW_HEIGHT));
        handle.getAccessibleContext().setAccessibleName("Reorder " + type.displayName());
        row.add(handle);

        JCheckBox cb = new JCheckBox();
        cb.setSelected(selection.isSelected(type));
        cb.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cb.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        cb.setFocusPainted(false);
        cb.addActionListener(e -> onToggle.accept(type, cb.isSelected()));
        row.add(cb);

        JLabel iconLbl = new JLabel();
        iconLbl.setPreferredSize(new Dimension(20, ROW_HEIGHT));
        applySprite(iconLbl, type);
        row.add(iconLbl);

        JLabel nameLbl = new JLabel(type.displayName());
        nameLbl.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        nameLbl.setFont(FontManager.getRunescapeSmallFont());
        row.add(nameLbl);

        // Drop target: row accepts another row's payload and forwards to onReorder.
        final int targetIndex = index;
        row.setTransferHandler(new TransferHandler()
        {
            @Override public boolean canImport(TransferSupport s)
            {
                return s.isDataFlavorSupported(STR);
            }

            @Override public boolean importData(TransferSupport s)
            {
                String payload = readPayload(s);
                if (payload == null || !payload.startsWith(PAYLOAD_PREFIX)) return false;
                LandmarkType dragged = parseTypeOrNull(payload.substring(PAYLOAD_PREFIX.length()));
                if (dragged == null) return false;
                int from = selection.order().indexOf(dragged);
                if (from < 0 || from == targetIndex) return false;
                onReorder.accept(from, targetIndex);
                return true;
            }
        });

        // Drag source: handle starts a MOVE drag after a 5 px-squared threshold.
        // Installs a SourceCapable TransferHandler on the row on first press so exportAsDrag has a payload.
        DragGesture gesture = new DragGesture(row, type);
        handle.addMouseListener(gesture);
        handle.addMouseMotionListener(gesture);

        return row;
    }

    private static String readPayload(TransferHandler.TransferSupport s)
    {
        try { return (String) s.getTransferable().getTransferData(STR); }
        catch (Exception e) { return null; }
    }

    private static LandmarkType parseTypeOrNull(String name)
    {
        try { return LandmarkType.valueOf(name); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** Tracks press + drag-distance on a row's drag-handle and triggers exportAsDrag. */
    private static final class DragGesture extends MouseAdapter implements MouseMotionListener
    {
        private final JComponent dragSource;
        private final LandmarkType type;
        private Point pressPoint;
        private boolean started;

        DragGesture(JComponent dragSource, LandmarkType type)
        {
            this.dragSource = dragSource;
            this.type = type;
        }

        @Override public void mousePressed(MouseEvent e)
        {
            pressPoint = e.getPoint();
            started = false;
            // Lazily install a source-capable wrapper on the row TransferHandler so exportAsDrag has a payload.
            TransferHandler current = dragSource.getTransferHandler();
            if (!(current instanceof SourceCapable))
            {
                dragSource.setTransferHandler(new SourceCapable(current, type));
            }
        }

        @Override public void mouseReleased(MouseEvent e)
        {
            pressPoint = null;
            started = false;
        }

        @Override public void mouseDragged(MouseEvent e)
        {
            if (pressPoint == null || started) return;
            int dx = e.getX() - pressPoint.x;
            int dy = e.getY() - pressPoint.y;
            if (dx * dx + dy * dy >= DRAG_THRESHOLD_SQ)
            {
                started = true;
                TransferHandler th = dragSource.getTransferHandler();
                if (th != null) th.exportAsDrag(dragSource, e, TransferHandler.MOVE);
            }
        }

        @Override public void mouseMoved(MouseEvent e) { }
    }

    /**
     * Adapter that augments the row's drop-target TransferHandler with source semantics
     * (createTransferable + getSourceActions) so exportAsDrag has a payload to ship.
     */
    private static final class SourceCapable extends TransferHandler
    {
        private final TransferHandler delegate;
        private final LandmarkType type;

        SourceCapable(TransferHandler delegate, LandmarkType type)
        {
            this.delegate = delegate;
            this.type = type;
        }

        @Override public int getSourceActions(JComponent c) { return MOVE; }

        @Override protected Transferable createTransferable(JComponent c)
        {
            return new StringSelection(PAYLOAD_PREFIX + type.name());
        }

        @Override public boolean canImport(TransferSupport s)
        {
            return delegate != null && delegate.canImport(s);
        }

        @Override public boolean importData(TransferSupport s)
        {
            return delegate != null && delegate.importData(s);
        }
    }

    private void applySprite(JLabel target, LandmarkType type)
    {
        Integer id = spriteIds.get(type);
        if (id == null || spriteManager == null) return;
        spriteManager.getSpriteAsync(id, 0, img -> {
            if (img == null) return;
            SwingUtilities.invokeLater(() -> {
                target.setIcon(new ImageIcon(scaleDownIfNeeded(img)));
            });
        });
    }

    private static Image scaleDownIfNeeded(BufferedImage src)
    {
        int longest = Math.max(src.getWidth(), src.getHeight());
        if (longest <= 16) return src;
        double scale = 16.0 / longest;
        return src.getScaledInstance((int) Math.round(src.getWidth() * scale),
            (int) Math.round(src.getHeight() * scale), Image.SCALE_SMOOTH);
    }
}
