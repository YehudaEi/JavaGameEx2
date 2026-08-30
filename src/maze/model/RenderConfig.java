package maze.model;

import java.awt.Color;

public class RenderConfig {
    public Color wallColor, pathColor, gridColor;
    public boolean drawGrid;
    public int animationDelay;

    public RenderConfig(Color wallColor, Color pathColor, boolean drawGrid, Color gridColor, int animationDelay) {
        this.wallColor = wallColor;
        this.pathColor = pathColor;
        this.drawGrid = drawGrid;
        this.gridColor = gridColor;
        this.animationDelay = animationDelay;
    }

    // ניתוח פשוט של הטקסט שחוזר מהשרת
    public static RenderConfig parse(String json) {
        Color wall = parseColor(getValue(json, "wallCellColor"), Color.BLACK);
        Color path = parseColor(getValue(json, "pathColor"), Color.GREEN);
        boolean grid = Boolean.parseBoolean(getValue(json, "drawGrid"));
        Color gridCol = parseColor(getValue(json, "gridColor"), Color.GRAY);
        int delay = 80;
        try { delay = Integer.parseInt(getValue(json, "animationDelayMs")); } catch (Exception ignored) {}

        return new RenderConfig(wall, path, grid, gridCol, delay);
    }

    private static String getValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":");
        if (start == -1) return "";
        start = json.indexOf(":", start) + 1;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return json.substring(start, end).replace("\"", "").trim();
    }

    private static Color parseColor(String hex, Color def) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return new Color(Integer.parseInt(hex, 16));
        } catch (Exception e) { return def; }
    }
}