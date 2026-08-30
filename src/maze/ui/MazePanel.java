package maze.ui;

import maze.model.RenderConfig;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MazePanel extends JPanel {
    private final int CELL_SIZE = 20;
    private boolean[][] isPassable; // [row][col]
    private int gridWidth, gridHeight;
    private RenderConfig config;
    private List<Point> pathAnimation = new ArrayList<>();
    private boolean animating = false;

    public void setMaze(BufferedImage img, RenderConfig config, int targetW, int targetH) {
        this.config = config;
        this.gridWidth = targetW;
        this.gridHeight = targetH;
        this.isPassable = new boolean[targetH][targetW];
        this.pathAnimation.clear();

        // גודל פיקסל בודד בתמונת המקור שהגיעה מהשרת
        double cellW = (double) img.getWidth() / targetW;
        double cellH = (double) img.getHeight() / targetH;

        for (int r = 0; r < targetH; r++) {
            for (int c = 0; c < targetW; c++) {
                // דגימת פיקסל מולקולרי בדיוק ממרכז המשבצת (מונע קריאת גבולות)
                int sampleX = (int) ((c + 0.5) * cellW);
                int sampleY = (int) ((r + 0.5) * cellH);

                sampleX = Math.max(0, Math.min(sampleX, img.getWidth() - 1));
                sampleY = Math.max(0, Math.min(sampleY, img.getHeight() - 1));

                int rgb = img.getRGB(sampleX, sampleY);
                int rVal = (rgb >> 16) & 0xFF;
                int gVal = (rgb >> 8) & 0xFF;
                int bVal = rgb & 0xFF;

                // פיקסל לבן = מעבר (ערכי RGB קרובים ל-255)
                boolean isWhite = (rVal > 200 && gVal > 200 && bVal > 200);
                isPassable[r][c] = isWhite;
            }
        }

        setPreferredSize(new Dimension(targetW * CELL_SIZE, targetH * CELL_SIZE));
        revalidate();
        repaint();
    }

    public boolean isAnimating() { return animating; }
    public boolean[][] getIsPassable() { return isPassable; }

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
        if (isPassable == null || config == null) return;

        // ציור קירות ומעברים לפי צבע ה-wallColor מהשרת
        for (int r = 0; r < gridHeight; r++) {
            for (int c = 0; c < gridWidth; c++) {
                g.setColor(isPassable[r][c] ? Color.WHITE : config.getWallColor());
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
            for (int r = 0; r <= gridHeight; r++) {
                g.drawLine(0, r * CELL_SIZE, gridWidth * CELL_SIZE, r * CELL_SIZE);
            }
            for (int c = 0; c <= gridWidth; c++) {
                g.drawLine(c * CELL_SIZE, 0, c * CELL_SIZE, gridHeight * CELL_SIZE);
            }
        }
    }
}