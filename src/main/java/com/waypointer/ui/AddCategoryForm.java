package com.waypointer.ui;

import com.waypointer.service.PresetOverrides;
import com.waypointer.service.PresetOverridesSnapshot.CategoryOverride;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;

final class AddCategoryForm extends JPanel
{
    AddCategoryForm(PresetOverrides overrides, Runnable onClose, Consumer<String> toast)
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JTextField name = new JTextField();
        Styles.textField(name);
        JTextArea desc = new JTextArea("", 2, 12);
        desc.setLineWrap(true);
        Styles.textArea(desc);

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setOpaque(false);
        center.add(name, BorderLayout.NORTH);
        center.add(new JScrollPane(desc), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        south.setOpaque(false);
        JButton ok = new JButton("Add");
        Styles.secondaryButton(ok);
        JButton cancel = new JButton("Cancel");
        Styles.secondaryButton(cancel);
        south.add(cancel);
        south.add(ok);
        add(south, BorderLayout.SOUTH);

        ok.addActionListener(e -> {
            String n = name.getText().trim();
            if (n.isEmpty()) { toast.accept("Category name is required"); return; }
            boolean added = overrides.addCategory(
                new CategoryOverride(n, desc.getText(), null, new ArrayList<>()));
            if (!added) { toast.accept("That category already exists. Switch to it instead."); return; }
            onClose.run();
        });
        cancel.addActionListener(e -> onClose.run());
    }
}
