package maze.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import maze.model.MazeGrid;
import maze.model.RenderConfig;

/**
 * מצייר את המבוך ומריץ את אנימציית הפתרון.
 * <p>
 * המבוך מצויר כאן מחדש, משבצת אחר משבצת, לפי המבנה שפוענח מתמונת המקור ולפי
 * הגדרות הציור שהתקבלו מהשרת. תמונת המקור עצמה אינה מוצגת בשום שלב.
 * <p>
 * כל המחלקה פועלת ב-Event Dispatch Thread בלבד, כולל {@link Timer} של Swing
 * שמפעיל את האנימציה ומריץ את פעימותיו על אותו thread.
 */
public class MazePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** גודל משבצת בפיקסלים. התרגיל מתיר לקבוע אותו בקוד - הוא אינו מגיע מהשרת. */
    private static final int CELL_SIZE = 20;

    private transient MazeGrid grid;
    private transient RenderConfig config;

    /** המשבצות שכבר נצבעו באנימציה. ריקה כשלא מוצג פתרון. */
    private final transient List<Point> revealedPath = new ArrayList<>();

    /** ה-Timer הפעיל, או {@code null} כשאין אנימציה. מחזיק אותו כדי שניתן יהיה לעצור. */
    private transient Timer animation;

    /**
     * מציג מבוך חדש. אנימציה שרצה נעצרת והפתרון הקודם נמחק.
     *
     * @param grid   המבוך לציור
     * @param config הגדרות הציור מהשרת
     */
    public void setMaze(MazeGrid grid, RenderConfig config) {
        // בלי העצירה הזו ה-Timer של המבוך הקודם היה ממשיך לרוץ ולצבוע
        // משבצות לפי הנתיב הישן, על גבי המבוך החדש.
        stopAnimation();

        this.grid = grid;
        this.config = config;
        this.revealedPath.clear();

        setPreferredSize(new Dimension(grid.getWidth() * CELL_SIZE, grid.getHeight() * CELL_SIZE));
        revalidate();
        repaint();
    }

    /** המבוך המוצג, או {@code null} אם עדיין לא נטען מבוך. */
    public MazeGrid getGrid() {
        return grid;
    }

    /** האם אנימציה רצה כרגע. */
    public boolean isAnimating() {
        return animation != null;
    }

    /** עוצר אנימציה שרצה. פעולת no-op אם אין כזו. הקריאה החוזרת לא מופעלת. */
    public void stopAnimation() {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
    }

    /**
     * מציג את הפתרון באנימציה: משבצת אחת בכל פעם, בהמתנה של
     * {@code animationDelayMs} בין משבצת למשבצת.
     * <p>
     * ציור קודם של פתרון נמחק תחילה, כך שלחיצה חוזרת מציגה את האנימציה מהתחלה.
     *
     * @param path       הנתיב לפי הסדר
     * @param onFinished מופעל כשהאנימציה הסתיימה. אינו מופעל אם היא נעצרה באמצע
     */
    public void animateSolution(List<Point> path, Runnable onFinished) {
        stopAnimation();
        revealedPath.clear();
        repaint();

        int[] nextIndex = {0};
        animation = new Timer(config.getAnimationDelayMs(), event -> {
            if (nextIndex[0] < path.size()) {
                Point cell = path.get(nextIndex[0]++);
                revealedPath.add(cell);

                // רק המשבצת החדשה מצוירת מחדש. repaint() ללא ארגומנטים היה מצייר
                // מחדש את כל המבוך בכל פעימה - 10,000 משבצות במבוך 100x100, לאורך
                // כל האנימציה. paintComponent ממילא מצייר רק את מה שה-clip מכסה.
                repaint(cell.x * CELL_SIZE, cell.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            } else {
                stopAnimation();
                if (onFinished != null) {
                    onFinished.run();
                }
            }
        });
        animation.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (grid == null || config == null) {
            return;
        }

        // משבצות מעבר תמיד לבנות. צבע הקירות מגיע מהשרת ואינו קבוע בקוד.
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                g.setColor(grid.isOpen(col, row) ? Color.WHITE : config.getWallColor());
                g.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // החלק מהנתיב שנחשף עד כה
        g.setColor(config.getPathColor());
        for (Point cell : revealedPath) {
            g.fillRect(cell.x * CELL_SIZE, cell.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        if (config.isDrawGrid()) {
            // הקו הסוגר יושב על הפיקסל האחרון של הפאנל ולא אחריו. ציור ב-
            // width * CELL_SIZE נופל מחוץ לתחום הפיקסלים 0..width*CELL_SIZE-1,
            // ולכן קווי הגבול הימני והתחתון פשוט לא היו מצוירים.
            int lastX = grid.getWidth() * CELL_SIZE - 1;
            int lastY = grid.getHeight() * CELL_SIZE - 1;

            g.setColor(config.getGridColor());
            for (int row = 0; row <= grid.getHeight(); row++) {
                int y = Math.min(row * CELL_SIZE, lastY);
                g.drawLine(0, y, lastX, y);
            }
            for (int col = 0; col <= grid.getWidth(); col++) {
                int x = Math.min(col * CELL_SIZE, lastX);
                g.drawLine(x, 0, x, lastY);
            }
        }
    }
}
