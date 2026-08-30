package maze;

import maze.ui.MazeApp;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // הרצת ממשק המשתמש ב-Event Dispatch Thread של Swing
        SwingUtilities.invokeLater(() -> {
            MazeApp app = new MazeApp();
            app.setVisible(true);
        });
    }
}