package maze.model;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * הגדרות הציור של המבוך, כפי שהתקבלו מהשרת.
 * <p>
 * כל הערכים כאן מגיעים מה-API ואף אחד מהם אינו קבוע בקוד. זו דרישה מפורשת של
 * התרגיל, ולכן {@link #parse(String)} זורקת חריגה כששדה חסר במקום להשלים אותו
 * בערך ברירת מחדל: תצוגה של צבע שהומצא בקוד גרועה יותר מהודעת שגיאה.
 * <p>
 * המחלקה חסינה לשינוי (immutable) ואינה תלויה ב-Swing.
 */
public final class RenderConfig {

    /**
     * מאתר {@code "key": value} ומאפשר רווחים סביב הנקודתיים.
     * הקבוצה הראשונה תופסת ערך במרכאות, השנייה ערך חשוף כמו מספר או בוליאני.
     */
    private static final String FIELD_PATTERN = "\"%s\"\s*:\s*(?:\"([^\"]*)\"|([^,}\s]+))";

    private final Color wallColor;
    private final Color pathColor;
    private final boolean drawGrid;
    private final Color gridColor;
    private final int animationDelayMs;

    public RenderConfig(Color wallColor, Color pathColor, boolean drawGrid, Color gridColor, int animationDelayMs) {
        this.wallColor = wallColor;
        this.pathColor = pathColor;
        this.drawGrid = drawGrid;
        this.gridColor = gridColor;
        this.animationDelayMs = animationDelayMs;
    }

    /** הצבע שבו נצבעות משבצות הקיר. אינו בהכרח צבע הקירות שבתמונת המקור. */
    public Color getWallColor() {
        return wallColor;
    }

    /** הצבע שבו נצבע נתיב הפתרון באנימציה. */
    public Color getPathColor() {
        return pathColor;
    }

    /** האם לצייר קווי רשת בין המשבצות. */
    public boolean isDrawGrid() {
        return drawGrid;
    }

    /** צבע קווי הרשת. רלוונטי רק כאשר {@link #isDrawGrid()} מחזירה {@code true}. */
    public Color getGridColor() {
        return gridColor;
    }

    /** זמן ההמתנה במילישניות בין צביעת משבצת אחת בנתיב לבין הבאה. */
    public int getAnimationDelayMs() {
        return animationDelayMs;
    }

    /**
     * מפענח את ה-JSON שמחזיר {@code /fm1/get-render-config}.
     * <p>
     * מימוש ידני וקצר, כדי שלא תידרש ספריית JSON חיצונית. הוא מסתפק במבנה השטוח
     * שהשרת מחזיר בפועל, ואינו מנסה להיות מפענח JSON כללי.
     *
     * @param json גוף התשובה
     * @return ההגדרות שנקראו
     * @throws IllegalArgumentException אם שדה חסר או שערכו אינו תקין
     */
    public static RenderConfig parse(String json) {
        Color wall = parseColor(requireField(json, "wallCellColor"), "wallCellColor");
        Color path = parseColor(requireField(json, "pathColor"), "pathColor");
        boolean grid = parseBoolean(requireField(json, "drawGrid"));
        Color gridColor = parseColor(requireField(json, "gridColor"), "gridColor");
        int delay = parseDelay(requireField(json, "animationDelayMs"));

        return new RenderConfig(wall, path, grid, gridColor, delay);
    }

    /** מחזיר את ערכו של שדה, או זורק חריגה אם אינו קיים. */
    private static String requireField(String json, String key) {
        Matcher matcher = Pattern.compile(String.format(FIELD_PATTERN, key)).matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("חסר השדה " + key + " בתשובת השרת");
        }
        String quoted = matcher.group(1);
        return (quoted != null ? quoted : matcher.group(2)).trim();
    }

    /** ממיר {@code "#RRGGBB"} או {@code "RRGGBB"} ל-{@link Color}, בכל רישיות. */
    private static Color parseColor(String value, String key) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6) {
            throw new IllegalArgumentException("ערך צבע לא תקין עבור " + key + ": " + value);
        }
        try {
            return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ערך צבע לא תקין עבור " + key + ": " + value, e);
        }
    }

    /**
     * ממיר בוליאני. לא נעשה שימוש ב-{@code Boolean.parseBoolean} משום שהיא מחזירה
     * {@code false} עבור כל מחרוזת שאינה "true", ובכך מסתירה ערך פגום.
     */
    private static boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("ערך לא תקין עבור drawGrid: " + value);
    }

    /**
     * ממיר את זמן האנימציה. ערך שלילי אינו חוקי עבור {@code javax.swing.Timer},
     * ולכן הוא נחתך לאפס - זו הגנה על חוזה ה-API של Swing, לא ערך ברירת מחדל.
     */
    private static int parseDelay(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ערך לא תקין עבור animationDelayMs: " + value, e);
        }
    }
}
