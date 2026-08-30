package maze;

import javax.swing.SwingUtilities;

import maze.ui.MainFrame;

/**
 * נקודת הכניסה לתוכנית.
 * <p>
 * החלון נבנה בתוך {@code invokeLater} ולא ב-thread הראשי, משום שרכיבי Swing
 * מיועדים לשימוש מ-Event Dispatch Thread בלבד.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
