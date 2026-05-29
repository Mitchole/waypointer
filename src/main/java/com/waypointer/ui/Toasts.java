package com.waypointer.ui;

/**
 * Surface used by callers that want to display a transient confirmation message.
 * Decouples those callers from the JLayeredPane-based ToastOverlay implementation
 * and lets tests substitute a capturing fake.
 */
public interface Toasts
{
    /**
     * Visual treatment for a toast: drives the message colour and the leading glyph
     * the {@link ToastOverlay} prepends. Callers that don't care about styling can
     * use the non-severity overloads, which default to {@link #SUCCESS}.
     */
    enum Severity { SUCCESS, WARN, ERROR }

    /** Show a self-dismissing message with the default short duration. */
    void show(String text);

    /**
     * Show a message with a clickable action label (e.g. "Undo"). The message
     * stays on screen longer so the user has time to click. Clicking the label
     * runs onClick and hides the toast immediately.
     */
    void show(String text, String actionLabel, Runnable onClick);

    /** Severity-flavoured variant. Default routes to {@link #show(String)} so legacy implementations still compile. */
    default void show(String text, Severity severity) { show(text); }

    /** Severity-flavoured variant with an action label. */
    default void show(String text, String actionLabel, Runnable onClick, Severity severity)
    {
        show(text, actionLabel, onClick);
    }

    /** Drop-in target for components that don't have a real overlay wired yet. */
    Toasts NO_OP = new Toasts()
    {
        @Override public void show(String text) {}
        @Override public void show(String text, String actionLabel, Runnable onClick) {}
    };
}
