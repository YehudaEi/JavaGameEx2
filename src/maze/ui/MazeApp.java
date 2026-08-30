package maze.ui;

import maze.model.MazeSolver;
import maze.model.RenderConfig;
import maze.util.ApiService;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class MazeApp extends JFrame {
    private RenderConfig config;
    private JTextField wField = new JTextField("30", 4), hField = new JTextField("30", 4);
    private JLabel infoLabel = new JLabel("הגדרות לא נטענו");
    private JButton refreshBtn = new JButton("Refresh Config");
    private JButton getMazeBtn = new JButton("GET MAZE");
    private JButton checkBtn = new JButton("Check Solution");
    private MazePanel mazePanel = new MazePanel();
    private int width = 30, height = 30;

    public MazeApp() {
        setTitle("Visual Maze Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.add(refreshBtn);
        top.add(infoLabel);
        top.add(new JLabel("Width:"));
        top.add(wField);
        top.add(new JLabel("Height:"));
        top.add(hField);
        top.add(getMazeBtn);
        top.add(checkBtn);
        checkBtn.setEnabled(false);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(mazePanel), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> fetchConfig());
        getMazeBtn.addActionListener(e -> fetchMaze());
        checkBtn.addActionListener(e -> solve());

        pack();
        setLocationRelativeTo(null);
        fetchConfig();
    }

    private void fetchConfig() {
        new Thread(() -> {
            try {
                config = ApiService.fetchRenderConfig();
                infoLabel.setText("Delay: " + config.getAnimationDelayMs() + "ms | Grid: " + config.isDrawGrid());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "שגיאה בטעינת הגדרות");
            }
        }).start();
    }

    private void fetchMaze() {
        width = validate(wField.getText());
        height = validate(hField.getText());

        new Thread(() -> {
            try {
                BufferedImage img = ApiService.fetchMazeImage(width, height);
                SwingUtilities.invokeLater(() -> {
                    mazePanel.setMaze(img, config, width, height);
                    checkBtn.setEnabled(true);
                    pack();
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "שגיאה בטעינת מבוך");
            }
        }).start();
    }

    private int validate(String text) {
        try {
            int v = Integer.parseInt(text);
            if (v >= 5 && v <= 100) return v;
        } catch (Exception ignored) {
        }
        return 30;
    }

    private void solve() {
        if (mazePanel.isAnimating()) return;
        checkBtn.setEnabled(false);

        List<Point> path = MazeSolver.solve(mazePanel.getIsPassable(), width, height);
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No solution found");
            checkBtn.setEnabled(true);
        } else {
            mazePanel.animatePath(path, () -> checkBtn.setEnabled(true));
        }
    }
}

