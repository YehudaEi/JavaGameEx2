package maze.model;

import java.awt.image.BufferedImage;

/**
 * מבנה המבוך: לכל משבצת נשמר האם היא מעבר או קיר.
 *
 * <h2>מוסכמת הקואורדינטות</h2>
 * לאורך כל הפרויקט <b>{@code col} היא עמודה ו-{@code row} היא שורה</b>, ובאובייקטי
 * {@link java.awt.Point} מתקיים {@code x = col} ו-{@code y = row}. המערך הפנימי
 * מאונדקס {@code [row][col]}, כמקובל במערכים דו-ממדיים, ולכן החליפו סדר והכל נשבר
 * בשקט. זו הסיבה שכל הגישות אליו עוברות דרך {@link #isOpen(int, int)}.
 *
 * <h2>המרה מתמונה</h2>
 * המחלקה אינה תלויה ב-Swing ואינה מציגה דבר. תמונת המקור משמשת כקלט בלבד.
 */
public final class MazeGrid {

    /** מעל הסף הזה בכל שלושת הערוצים נחשב הפיקסל לבן, כלומר מעבר. */
    private static final int WHITE_THRESHOLD = 200;

    /** {@code open[row][col]} - האם ניתן לעבור דרך המשבצת. */
    private final boolean[][] open;
    private final int width;
    private final int height;

    private MazeGrid(boolean[][] open, int width, int height) {
        this.open = open;
        this.width = width;
        this.height = height;
    }

    /**
     * מפענח מבוך מתוך תמונת המקור שהתקבלה מהשרת.
     * <p>
     * <b>שימו לב:</b> בניגוד למה שכתוב בהנחיות, התמונה אינה פיקסל אחד למשבצת.
     * השרת מחזיר בפועל בלוק של 16x16 פיקסלים לכל משבצת (נבדק בגדלים 5x5 עד
     * 100x100, תמיד ביחס שלם וללא שארית). לכן נדגם פיקסל אחד ממרכז כל משבצת,
     * ולא {@code image.getRGB(col, row)} - קריאה כזו הייתה מחזירה את 16 השורות
     * הראשונות של המבוך בלבד.
     * <p>
     * הדגימה ממרכז המשבצת ולא מפינתה גם עמידה יותר: היא נכונה גם אילו יחס
     * ההגדלה היה משתנה או מפסיק להיות שלם.
     *
     * @param image  תמונת המקור
     * @param width  רוחב המבוך במשבצות, כפי שנבחר על ידי המשתמש
     * @param height גובה המבוך במשבצות
     * @return המבוך המפוענח
     * @throws IllegalArgumentException אם הגודל אינו חיובי או שהתמונה קטנה ממנו
     */
    public static MazeGrid fromImage(BufferedImage image, int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("גודל מבוך לא תקין: " + width + "x" + height);
        }
        if (image.getWidth() < width || image.getHeight() < height) {
            throw new IllegalArgumentException("התמונה שהתקבלה (" + image.getWidth() + "x" + image.getHeight()
                    + ") קטנה מהמבוך המבוקש (" + width + "x" + height + ")");
        }

        double cellPixelWidth = (double) image.getWidth() / width;
        double cellPixelHeight = (double) image.getHeight() / height;

        boolean[][] open = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            int sampleY = clamp((int) ((row + 0.5) * cellPixelHeight), image.getHeight() - 1);
            for (int col = 0; col < width; col++) {
                int sampleX = clamp((int) ((col + 0.5) * cellPixelWidth), image.getWidth() - 1);
                open[row][col] = isWhite(image.getRGB(sampleX, sampleY));
            }
        }
        return new MazeGrid(open, width, height);
    }

    /** רוחב המבוך במשבצות. */
    public int getWidth() {
        return width;
    }

    /** גובה המבוך במשבצות. */
    public int getHeight() {
        return height;
    }

    /**
     * האם המשבצת היא מעבר.
     *
     * @param col עמודה, בטווח {@code [0, width)}
     * @param row שורה, בטווח {@code [0, height)}
     * @return {@code true} אם ניתן לעבור דרכה, {@code false} אם היא קיר או מחוץ למבוך
     */
    public boolean isOpen(int col, int row) {
        return col >= 0 && col < width && row >= 0 && row < height && open[row][col];
    }

    /**
     * פיקסל לבן מייצג מעבר וכל צבע אחר מייצג קיר.
     * <p>
     * הבדיקה היא סף ולא השוואה מדויקת ל-{@code #FFFFFF}. בתמונות שהשרת מחזיר היום
     * יש בדיוק שני צבעים שטוחים ולכן שתי הגישות שקולות, אבל סף עמיד גם בפורמט עם
     * אובדן מידע כמו JPEG, שבו הלבן אינו נשמר במדויק.
     */
    private static boolean isWhite(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red > WHITE_THRESHOLD && green > WHITE_THRESHOLD && blue > WHITE_THRESHOLD;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }
}
