package maze.model;

import java.awt.Point;
import java.util.*;

public class MazeSolver {
    public static List<Point> solve(boolean[][] isPassable, int width, int height) {
        List<Point> path = new ArrayList<>();

        // בדיקת נקודת התחלה (0,0) ונקודת סיום (width-1, height-1)
        if (!isPassable[0][0] || !isPassable[height - 1][width - 1]) {
            return path;
        }

        boolean[][] visited = new boolean[height][width];
        Point[][] parent = new Point[height][width];
        Queue<Point> q = new LinkedList<>();

        q.add(new Point(0, 0));
        visited[0][0] = true;

        // תנועה: למעלה, למטה, שמאלה, ימינה
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        boolean reached = false;

        while (!q.isEmpty()) {
            Point curr = q.poll();

            if (curr.x == width - 1 && curr.y == height - 1) {
                reached = true;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nr = curr.y + dr[i];
                int nc = curr.x + dc[i];

                if (nr >= 0 && nr < height && nc >= 0 && nc < width) {
                    if (isPassable[nr][nc] && !visited[nr][nc]) {
                        visited[nr][nc] = true;
                        parent[nr][nc] = curr;
                        q.add(new Point(nc, nr));
                    }
                }
            }
        }

        if (reached) {
            Point p = new Point(width - 1, height - 1);
            while (p != null) {
                path.add(0, p);
                p = parent[p.y][p.x];
            }
        }

        return path;
    }
}