package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import net.runelite.client.ui.ColorScheme;

/**
 * Reusable inline form: a caption, one text field, a hidden error label, and Submit / Cancel
 * buttons. Parked inline rather than shown as a dialog, so opening it never steals focus the way
 * a {@code JOptionPane} does. Configured per use through {@link #openFor}. Submit stays disabled
 * until the field holds non-blank text; the submit handler reports a failure by returning an
 * error message, which is shown in red while the form stays open.
 */
class InlineInputForm extends JPanel
{
    /** Submit handler: return {@code null} on success (form hides), or a message to show inline. */
    interface Submit
    {
        String onSubmit(String text);
    }

    private final JLabel caption = new JLabel(" ");
    private final JTextField field = new JTextField();
    private final JLabel errorLabel = Styles.errorLabel();
    private final JButton submitBtn = new JButton("Submit");
    private final JButton cancelBtn = new JButton("Cancel");

    private Submit submit = text -> null;

    InlineInputForm()
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(ColorScheme.DARK_GRAY_COLOR);

        caption.setForeground(Color.WHITE);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(caption);

        // Field sits in a one-cell GridBag so it fills the width but keeps its preferred height,
        // instead of stretching vertically the way a bare component does inside BoxLayout(Y_AXIS).
        JPanel fieldRow = new JPanel(new GridBagLayout());
        fieldRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        fieldRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 0, 2, 0);
        g.gridx = 0; g.gridy = 0; g.weightx = 1;
        Styles.textField(field);
        fieldRow.add(field, g);
        stack.add(fieldRow);

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(errorLabel);

        add(stack, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        Styles.secondaryButton(cancelBtn);
        Styles.primaryButton(submitBtn);
        cancelBtn.addActionListener(e -> dismiss());
        submitBtn.addActionListener(e -> doSubmit());
        buttons.add(cancelBtn);
        buttons.add(submitBtn);
        add(buttons, BorderLayout.SOUTH);

        // Keep Submit disabled until the field holds non-blank text - this is what stops an empty
        // value from being submitted and silently dropped.
        field.getDocument().addDocumentListener(Styles.documentListener(this::updateSubmitEnabled));

        // ENTER in the field submits; ESC anywhere in the form cancels. Bound on the form's own
        // maps so they don't fire when focus is elsewhere in the panel.
        field.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "inlineSubmit");
        field.getActionMap().put("inlineSubmit", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e) { doSubmit(); }
        });
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "inlineCancel");
        getActionMap().put("inlineCancel", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e) { dismiss(); }
        });

        updateSubmitEnabled();
        setVisible(false);
    }

    /** Configure the form for one use and show it: caption, submit-button text, submit handler. */
    void openFor(String captionText, String submitText, Submit submit)
    {
        this.submit = submit;
        caption.setText(captionText);
        submitBtn.setText(submitText);
        field.setText("");
        errorLabel.setText(" ");
        errorLabel.setVisible(false);
        updateSubmitEnabled();
        setVisible(true);
        revalidate();
        repaint();
        field.requestFocusInWindow();
    }

    void dismiss()
    {
        field.setText("");
        errorLabel.setVisible(false);
        setVisible(false);
        revalidate();
        repaint();
    }

    private void doSubmit()
    {
        String typed = field.getText();
        String trimmed = typed == null ? "" : typed.trim();
        if (trimmed.isEmpty()) return;   // Submit is gated; guard the ENTER path too.
        String error = submit.onSubmit(trimmed);
        if (error == null)
        {
            dismiss();
            return;
        }
        errorLabel.setText(error);
        errorLabel.setVisible(true);
        revalidate();
        repaint();
        field.requestFocusInWindow();
    }

    private void updateSubmitEnabled()
    {
        String text = field.getText();
        submitBtn.setEnabled(text != null && !text.trim().isEmpty());
    }

    // ---- test seams ----
    void setText(String text) { field.setText(text); }
    void clickSubmit() { submitBtn.doClick(); }
    void clickCancel() { cancelBtn.doClick(); }
    String getErrorText() { return errorLabel.getText(); }
    boolean isSubmitEnabled() { return submitBtn.isEnabled(); }
}
