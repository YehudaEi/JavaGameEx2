package maze.ui;

import maze.model.MazeGrid;
import maze.model.RenderConfig;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MazePanel extends JPanel {
    private final int CELL_SIZE = 20;
    private MazeGrid grid;
    private RenderConfig config;
    private List<Point> pathAnimation = new ArrayList<>();
    private boolean animating = false;

    public void setMaze(MazeGrid grid, RenderConfig config) {
        this.grid = grid;
        this.config = config;
        this.pathAnimation.clear();

        setPreferredSize(new Dimension(grid.getWidth() * CELL_SIZE, grid.getHeight() * CELL_SIZE));
        revalidate();
        repaint();
    }

    public boolean isAnimating() { return animating; }
    public MazeGrid getGrid() { return grid; }

    public void animatePath(List<Point> path, Runnable onDone) {
        animating = true;
        pathAnimation.clear();
        final int[] index = {0};

        Timer t = new Timer(config.getAnimationDelayMs(), e -> {
            if (index[0] < path.size()) {
                pathAnimation.add(path.get(index[0]++));
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
                animating = false;
                if (onDone != null) onDone.run();
            }
        });
        t.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (grid == null || config == null) return;

        // ציור קירות ומעברים לפי צבע ה-wallColor מהשרת
        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {
                g.setColor(grid.isOpen(c, r) ? Color.WHITE : config.getWallColor());
                g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // ציור נתיב פתרון באנימציה לפי pathColor מהשרת
        g.setColor(config.getPathColor());
        for (Point p : pathAnimation) {
            g.fillRect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        // ציור רשת במידה ו-drawGrid הוא true
        if (config.isDrawGrid()) {
            g.setColor(config.getGridColor());
            for (int r = 0; r <= grid.getHeight(); r++) {
                g.drawLine(0, r * CELL_SIZE, grid.getWidth() * CELL_SIZE, r * CELL_SIZE);
            }
            for (int c = 0; c <= grid.getWidth(); c++) {
                g.drawLine(c * CELL_SIZE, 0, c * CELL_SIZE, grid.getHeight() * CELL_SIZE);
            }
        }
    }
}