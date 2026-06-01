package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.Library;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Static factories for the two transient body banners the panel can show above its category list:
 * the "Shortest Path missing" notice (with a dismiss-forever button) and the "library load failed"
 * reset banner. Both are height-capped via {@link Styles#cappedHeightPanel} so the body's
 * {@code BoxLayout(Y_AXIS)} does not stretch them.
 */
final class PanelBanners
{
    private PanelBanners() {}

    /**
     * Shown while the Shortest Path plugin is unavailable and the user has not dismissed the
     * notice. {@code onDismiss} runs after the dismiss flag is persisted so the panel can rebuild.
     */
    static JComponent shortestPathMissing(WaypointerConfig config, Runnable onDismiss)
    {
        JPanel p = Styles.cappedHeightPanel(new BorderLayout(4, 4));
        p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(140, 100, 0)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel msg = new JLabel("<html>Install the Shortest Path plugin to use Play.</html>");
        msg.setForeground(Color.WHITE);
        p.add(msg, BorderLayout.CENTER);
        JButton dismiss = new JButton("Don't show again");
        Styles.secondaryButton(dismiss);
        dismiss.addActionListener(e -> { config.setShortestPathBannerDismissed(true); onDismiss.run(); });
        p.add(dismiss, BorderLayout.EAST);
        return p;
    }

    /**
     * Shown when the library and its backup both failed to load and the store is refusing saves.
     * The Reset button confirms, deletes the unreadable files, and bootstraps a fresh library;
     * the store's own listeners drive the subsequent rebuild. {@code dialogParent} owns the
     * confirm dialog.
     */
    static JComponent loadFailedReset(WaypointStorePersistence persistence, WaypointStore store,
        Component dialogParent)
    {
        JPanel p = Styles.cappedHeightPanel(new BorderLayout(4, 4));
        p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 40, 40)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel resetMsg = new JLabel("<html>Library failed to load - backup also unreadable.<br>"
            + "Click Reset to start fresh, or fix the file at:<br><tt>"
            + persistence.libraryFile() + "</tt></html>");
        resetMsg.setForeground(Color.WHITE);
        p.add(resetMsg, BorderLayout.CENTER);
        JButton reset = new JButton("Reset library");
        Styles.secondaryButton(reset);
        reset.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(dialogParent,
                "This will discard the unreadable library files. Continue?",
                "Reset library", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            try { Files.deleteIfExists(persistence.libraryFile()); }
            catch (IOException ignored) {}
            try { Files.deleteIfExists(persistence.backupFile()); }
            catch (IOException ignored) {}
            persistence.allowSavesAfterReset();
            store.bootstrap(new Library());
        });
        p.add(reset, BorderLayout.EAST);
        return p;
    }
}
