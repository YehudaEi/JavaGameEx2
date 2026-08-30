package maze.model;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * מציאת מסלול מהמשבצת השמאלית העליונה אל הימנית התחתונה.
 * <p>
 * המימוש הוא BFS, ולכן המסלול שמוחזר הוא גם הקצר ביותר. תנועה מותרת בארבעה
 * כיוונים בלבד - למעלה, למטה, שמאלה וימינה - ורק דרך משבצות מעבר.
 */
public final class MazeSolver {

    /** ההיסטים לארבעת הכיוונים: למעלה, למטה, שמאלה וימינה. אין אלכסונים. */
    private static final int[] COLUMN_STEPS = {0, 0, -1, 1};
    private static final int[] ROW_STEPS = {-1, 1, 0, 0};

    private MazeSolver() {
    }

    /**
     * מחפש מסלול מ-{@code (0, 0)} אל {@code (width - 1, height - 1)}.
     *
     * @param grid המבוך
     * @return המסלול לפי הסדר, מההתחלה עד הסוף, או רשימה ריקה אם אין פתרון.
     *         בכל {@link Point} מתקיים {@code x = col} ו-{@code y = row}.
     */
    public static List<Point> solve(MazeGrid grid) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        int goalCol = width - 1;
        int goalRow = height - 1;

        // מבוך שההתחלה או הסיום שלו הם קיר אינו פתיר, ואין טעם לחפש בו.
        if (!grid.isOpen(0, 0) || !grid.isOpen(goalCol, goalRow)) {
            return List.of();
        }

        Point[][] cameFrom = new Point[height][width];
        boolean[][] visited = new boolean[height][width];
        Deque<Point> queue = new ArrayDeque<>();

        queue.add(new Point(0, 0));
        visited[0][0] = true;

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            if (current.x == goalCol && current.y == goalRow) {
                return buildPath(cameFrom, current);
            }

            for (int direction = 0; direction < COLUMN_STEPS.length; direction++) {
                int col = current.x + COLUMN_STEPS[direction];
                int row = current.y + ROW_STEPS[direction];

                if (grid.isOpen(col, row) && !visited[row][col]) {
                    visited[row][col] = true;
                    cameFrom[row][col] = current;
                    queue.add(new Point(col, row));
                }
            }
        }
        return List.of();
    }

    /** משחזר את המסלול לאחור מהיעד עד ההתחלה, ומחזיר אותו בסדר הנכון. */
    private static List<Point> buildPath(Point[][] cameFrom, Point goal) {
        List<Point> path = new ArrayList<>();
        for (Point step = goal; step != null; step = cameFrom[step.y][step.x]) {
            path.add(step);
        }
        Collections.reverse(path);
        return path;
    }
}
