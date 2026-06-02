package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.Library;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.service.RouteStore;
import com.waypointer.service.RouteStorePersistence;
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
 * Static factories for the transient body banners panels can show above their content lists:
 * the "Shortest Path missing" notice (with a dismiss-forever button), the waypoint "library load
 * failed" reset banner, and its routes-tab equivalent. All are height-capped via
 * {@link Styles#cappedHeightPanel} so the containing {@code BoxLayout(Y_AXIS)} does not stretch them.
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
            // Cancel is the default-focused button so a stray Enter can't discard the files.
            String[] options = {"Cancel", "Reset"};
            int choice = JOptionPane.showOptionDialog(dialogParent,
                "This will discard the unreadable library files. Continue?",
                "Reset library", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            if (choice != 1) return;
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

    /**
     * Routes-tab equivalent of {@link #loadFailedReset}: shown when routes.json and its backup
     * both failed to load and the route store is refusing saves. Reset confirms, deletes the
     * unreadable files, clears the freeze, and bootstraps an empty route library; the store's
     * own listeners drive the subsequent rebuild. {@code dialogParent} owns the confirm dialog.
     */
    static JComponent routeLoadFailedReset(RouteStorePersistence persistence, RouteStore store,
        Component dialogParent)
    {
        JPanel p = Styles.cappedHeightPanel(new BorderLayout(4, 4));
        p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 40, 40)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel resetMsg = new JLabel("<html>Routes failed to load - backup also unreadable.<br>"
            + "Click Reset to start fresh, or fix the file at:<br><tt>"
            + persistence.routesFile() + "</tt></html>");
        resetMsg.setForeground(Color.WHITE);
        p.add(resetMsg, BorderLayout.CENTER);
        JButton reset = new JButton("Reset routes");
        Styles.secondaryButton(reset);
        reset.addActionListener(e -> {
            // Cancel is the default-focused button so a stray Enter can't discard the files.
            String[] options = {"Cancel", "Reset"};
            int choice = JOptionPane.showOptionDialog(dialogParent,
                "This will discard the unreadable route files. Continue?",
                "Reset routes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            if (choice != 1) return;
            try { Files.deleteIfExists(persistence.routesFile()); }
            catch (IOException ignored) {}
            try { Files.deleteIfExists(persistence.backupFile()); }
            catch (IOException ignored) {}
            persistence.allowSavesAfterReset();
            store.bootstrap(new RouteLibrary());
        });
        p.add(reset, BorderLayout.EAST);
        return p;
    }
}
