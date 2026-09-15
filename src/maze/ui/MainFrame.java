package maze.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import maze.model.MazeGrid;
import maze.model.MazeSolver;
import maze.model.RenderConfig;
import maze.util.ApiService;

/**
 * חלון התוכנית, והמעבר בין שני המסכים.
 *
 * <h2>שני מסכים</h2>
 * המסך הראשון הוא {@link ConfigPanel} - הגדרות הציור וגודל המבוך. מסך המבוך
 * נפתח רק לאחר ש-GET MAZE החזיר תמונה, ולכן לפני כן אין מבוך על המסך ואין
 * מה לבדוק. המעבר נעשה ב-{@link CardLayout}.
 *
 * <h2>Threading</h2>
 * קריאות הרשת חייבות לרוץ מחוץ ל-Event Dispatch Thread, אחרת החלון נתקע עד
 * שהשרת עונה. מנגד, אסור לגעת ברכיבי Swing מחוץ ל-EDT. שתי הדרישות מתקיימות
 * באמצעות {@link SwingWorker}: {@code doInBackground} פונה לשרת ומפענח, ורק
 * {@code done} נוגע בממשק. לכן גם {@link #config} נכתב ונקרא ב-EDT בלבד, ואין
 * צורך בסנכרון.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String CARD_CONFIG = "Config";
    private static final String CARD_MAZE = "Maze";

    /**
     * גבול לאזור הגלילה שבו מוצג המבוך. בלעדיו מבוך 100x100 היה נותן חלון
     * ברוחב 2000 פיקסלים - גדול מכל מסך - והכפתורים היו יוצאים מהתמונה.
     */
    private static final int MAX_VIEW_WIDTH = 900;
    private static final int MAX_VIEW_HEIGHT = 640;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final ConfigPanel configPanel;
    private final MazePanel mazePanel = new MazePanel();
    private final JScrollPane mazeScrollPane = new JScrollPane(mazePanel);
    private final JButton checkSolutionButton = new JButton("Check Solution");
    private final JButton backButton = new JButton("Back to settings");

    /** ההגדרות האחרונות שהתקבלו מהשרת. נגישות מה-EDT בלבד. */
    private transient RenderConfig config;

    public MainFrame() {
        setTitle("Visual Maze");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        configPanel = new ConfigPanel(event -> loadConfig(), event -> loadMaze());
        cards.add(configPanel, CARD_CONFIG);
        cards.add(buildMazeCard(), CARD_MAZE);
        add(cards);

        checkSolutionButton.addActionListener(event -> checkSolution());
        backButton.addActionListener(event -> showConfigScreen());

        cardLayout.show(cards, CARD_CONFIG);
        resizeToFit();
        setLocationRelativeTo(null);

        loadConfig();
    }

    /** מסך המבוך: אזור הציור, וכפתורי הבדיקה והחזרה. */
    private JPanel buildMazeCard() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttons.add(checkSolutionButton);
        buttons.add(backButton);

        JPanel card = new JPanel(new BorderLayout());
        card.add(mazeScrollPane, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    /**
     * שולף את הגדרות הציור מהשרת ומציג אותן.
     * <p>
     * מופעל פעם אחת עם פתיחת התוכנית, ושוב בכל לחיצה על Refresh Config.
     * שולף רק את ההגדרות - לא את המבוך.
     */
    private void loadConfig() {
        configPanel.setBusy(true, "Loading render settings...");

        new SwingWorker<RenderConfig, Void>() {
            @Override
            protected RenderConfig doInBackground() throws Exception {
                return ApiService.fetchRenderConfig();
            }

            @Override
            protected void done() {
                try {
                    config = get();
                    configPanel.showConfig(config);
                    configPanel.setBusy(false, "Render settings updated.");

                    // ריבועי הצבע וערכי ההקסה רחבים מהמצייני המקום שהוחלפו,
                    // ובלי התאמה מחדש שורת הרוחב והגובה הייתה נחתכת.
                    resizeToFit();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    configPanel.setBusy(false, "");
                } catch (ExecutionException e) {
                    configPanel.setBusy(false, "");
                    showError("Failed to load render settings", e);
                }
            }
        }.execute();
    }

    /**
     * שולף תמונת מבוך בגודל שנבחר, מפענח אותה ומציג את המבוך.
     * <p>
     * גם השליפה וגם הפענוח מתבצעים ב-{@code doInBackground}: פענוח מבוך
     * 100x100 סורק 10,000 משבצות, ואין סיבה להעמיס זאת על ה-EDT.
     */
    private void loadMaze() {
        Dimension requested = configPanel.readAndNormalizeSize();
        int width = requested.width;
        int height = requested.height;
        RenderConfig activeConfig = config;

        configPanel.setBusy(true, "Loading maze " + width + "x" + height + "...");

        new SwingWorker<MazeGrid, Void>() {
            @Override
            protected MazeGrid doInBackground() throws Exception {
                BufferedImage image = ApiService.fetchMazeImage(width, height);
                return MazeGrid.fromImage(image, width, height);
            }

            @Override
            protected void done() {
                try {
                    showMazeScreen(get(), activeConfig);
                    configPanel.setBusy(false, "");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    configPanel.setBusy(false, "");
                } catch (ExecutionException e) {
                    configPanel.setBusy(false, "");
                    showError("Failed to load the maze", e);
                }
            }
        }.execute();
    }

    /** מציג את המבוך שהתקבל ועובר אליו. */
    private void showMazeScreen(MazeGrid grid, RenderConfig renderConfig) {
        mazePanel.setMaze(grid, renderConfig);
        checkSolutionButton.setEnabled(true);

        // הגודל נקבע על ה-viewport ולא על ה-JScrollPane עצמו, כדי שרוחב המסגרת
        // ייחשב מעליו. אחרת מבוך שנכנס בדיוק היה מקבל פסי גלילה מיותרים.
        Dimension maze = mazePanel.getPreferredSize();
        mazeScrollPane.getViewport().setPreferredSize(new Dimension(
                Math.min(maze.width, MAX_VIEW_WIDTH),
                Math.min(maze.height, MAX_VIEW_HEIGHT)));

        cardLayout.show(cards, CARD_MAZE);
        resizeToFit();
        setLocationRelativeTo(null);
    }

    /**
     * חוזר למסך ההגדרות, ועוצר אנימציה שרצה כדי שלא תמשיך ברקע.
     * <p>
     * גודל החלון נשמר בכוונה: כיווץ החלון בכל מעבר בין המסכים היה קופצני.
     */
    private void showConfigScreen() {
        mazePanel.stopAnimation();
        cardLayout.show(cards, CARD_CONFIG);
    }

    /**
     * בודק אם קיים פתרון, ומציג אותו באנימציה.
     * <p>
     * הכפתור מנוטרל למשך האנימציה כדי שלא יופעלו כמה אנימציות במקביל. לחיצה
     * לאחר שהאנימציה הסתיימה מתחילה אותה מחדש, על מבוך נקי.
     */
    private void checkSolution() {
        if (mazePanel.getGrid() == null || mazePanel.isAnimating()) {
            return;
        }

        List<Point> path = MazeSolver.solve(mazePanel.getGrid());
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No solution found",
                    "Check Solution", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        checkSolutionButton.setEnabled(false);
        mazePanel.animateSolution(path, () -> checkSolutionButton.setEnabled(true));
    }

    /**
     * {@code pack()} שאינו חורג מגבולות המסך.
     * <p>
     * מבוך 100x100 הוא 2000 פיקסלים ברוחב. בלי החיתוך הזה החלון היה גדול מכל
     * מסך והכפתורים היו יוצאים מהתמונה. הגלילה מטפלת בשארית.
     */
    private void resizeToFit() {
        pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(getWidth(), screen.width), Math.min(getHeight(), screen.height));
    }

    /**
     * מציג את סיבת הכישלון האמיתית ולא את העטיפה של {@link ExecutionException}.
     * <p>
     * לחריגה שאין לה הודעה - למשל {@code NullPointerException} - מוצג שם המחלקה,
     * אחרת הדיאלוג היה מציג למשתמש את המילה "null" בלבד.
     */
    private void showError(String title, ExecutionException failure) {
        Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
        String detail = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        JOptionPane.showMessageDialog(this, title + ":\n" + detail,
                title, JOptionPane.ERROR_MESSAGE);
    }
}
