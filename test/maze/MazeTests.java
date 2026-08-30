package maze;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import maze.model.MazeGrid;
import maze.model.MazeSolver;
import maze.model.RenderConfig;
import maze.util.ApiService;

/**
 * בדיקות לליבת התוכנית: פענוח ההגדרות, פענוח התמונה ומציאת הפתרון.
 * <p>
 * אין כאן תלות ב-JUnit ולא בשום ספרייה חיצונית, בדיוק כמו בשאר הפרויקט. המחלקה
 * מדפיסה שורה לכל בדיקה ומסיימת בקוד יציאה 1 אם משהו נכשל, כך שאפשר להריץ
 * אותה גם ידנית וגם כחלק מבדיקה אוטומטית.
 * <p>
 * בדיקות הליבה אינן נוגעות ברשת ולכן עוברות גם בלי חיבור לאינטרנט. בדיקות מול
 * השרת רצות רק כשמעבירים את הארגומנט {@code --live}.
 *
 * <pre>
 * java -cp out maze.MazeTests
 * java -cp out maze.MazeTests --live
 * </pre>
 */
public final class MazeTests {

    private static int passed;
    private static int failed;

    private MazeTests() {
    }

    public static void main(String[] args) {
        section("RenderConfig.parse");
        testParsesTheFormatTheServerReturns();
        testToleratesWhitespaceAroundValues();
        testAcceptsColoursInAnyForm();
        testRejectsMissingFields();
        testRejectsMalformedValues();
        testClampsNegativeDelay();

        section("MazeGrid.fromImage");
        testScaledAndUnscaledImagesAgree();
        testOnlyWhiteIsOpen();
        testKeepsRowsAndColumnsApart();
        testRejectsImagesSmallerThanTheMaze();

        section("MazeSolver.solve");
        testStraightCorridor();
        testSingleCell();
        testBlockedStart();
        testBlockedGoal();
        testDisconnectedComponents();
        testReturnsTheShortestPath();
        testMovesInFourDirectionsOnly();

        if (isLive(args)) {
            section("Live server");
            testLiveConfig();
            testLiveMazes();
        } else {
            section("Live server");
            System.out.println("  skipped, pass --live to run");
        }

        System.out.println();
        System.out.println(failed == 0
                ? "All " + passed + " checks passed."
                : failed + " of " + (passed + failed) + " checks FAILED.");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // RenderConfig
    // ------------------------------------------------------------------

    /** הפורמט המדויק שהשרת מחזיר היום. */
    private static void testParsesTheFormatTheServerReturns() {
        RenderConfig config = RenderConfig.parse("{\"wallCellColor\":\"#222222\",\"pathColor\":\"#00AA00\","
                + "\"drawGrid\":true,\"gridColor\":\"#CCCCCC\",\"animationDelayMs\":80}");

        check("wall colour", new Color(0x222222).equals(config.getWallColor()));
        check("path colour", new Color(0x00AA00).equals(config.getPathColor()));
        check("draw grid", config.isDrawGrid());
        check("grid colour", new Color(0xCCCCCC).equals(config.getGridColor()));
        check("animation delay", config.getAnimationDelayMs() == 80);
    }

    /**
     * רגרסיה: המפענח הקודם חיפש בדיוק {@code "key":} ולכן רווח אחד היה מפיל את
     * כל השדות אל ערכי ברירת מחדל שהיו כתובים בקוד, בלי שום סימן למשתמש.
     */
    private static void testToleratesWhitespaceAroundValues() {
        RenderConfig config = RenderConfig.parse("{\n  \"wallCellColor\" : \"#101010\" ,\n"
                + "  \"pathColor\" : \"#202020\" ,\n  \"drawGrid\" : false ,\n"
                + "  \"gridColor\" : \"#303030\" ,\n  \"animationDelayMs\" : 15\n}");

        check("pretty printed wall colour", new Color(0x101010).equals(config.getWallColor()));
        check("pretty printed draw grid", !config.isDrawGrid());
        check("pretty printed delay", config.getAnimationDelayMs() == 15);
    }

    private static void testAcceptsColoursInAnyForm() {
        RenderConfig config = RenderConfig.parse(json("aB12Cd", "#FF0000", "TRUE", "#0000ff", "5"));

        check("colour without a leading hash", new Color(0xAB12CD).equals(config.getWallColor()));
        check("lower case colour", new Color(0x0000FF).equals(config.getGridColor()));
        check("upper case boolean", config.isDrawGrid());
    }

    /** הבדיקה החשובה: שדה חסר חייב להיכשל ולא להיות מוחלף בערך קבוע. */
    private static void testRejectsMissingFields() {
        String[] keys = {"wallCellColor", "pathColor", "drawGrid", "gridColor", "animationDelayMs"};
        String complete = json("#222222", "#00AA00", "true", "#CCCCCC", "80");

        for (String key : keys) {
            String withoutKey = complete.replace("\"" + key + "\"", "\"removed_" + key + "\"");
            check("missing " + key + " is rejected", throwsIllegalArgument(() -> RenderConfig.parse(withoutKey)));
        }
    }

    private static void testRejectsMalformedValues() {
        check("short colour is rejected",
                throwsIllegalArgument(() -> RenderConfig.parse(json("#FFF", "#00AA00", "true", "#CCCCCC", "80"))));
        check("non hex colour is rejected",
                throwsIllegalArgument(() -> RenderConfig.parse(json("#GGGGGG", "#00AA00", "true", "#CCCCCC", "80"))));
        check("non boolean drawGrid is rejected",
                throwsIllegalArgument(() -> RenderConfig.parse(json("#222222", "#00AA00", "yes", "#CCCCCC", "80"))));
        check("non numeric delay is rejected",
                throwsIllegalArgument(() -> RenderConfig.parse(json("#222222", "#00AA00", "true", "#CCCCCC", "fast"))));
    }

    /** זמן שלילי אינו חוקי עבור Timer של Swing, ולכן נחתך לאפס. */
    private static void testClampsNegativeDelay() {
        RenderConfig config = RenderConfig.parse(json("#222222", "#00AA00", "true", "#CCCCCC", "-40"));
        check("negative delay is clamped to zero", config.getAnimationDelayMs() == 0);
    }

    // ------------------------------------------------------------------
    // MazeGrid
    // ------------------------------------------------------------------

    /**
     * השרת מחזיר בפועל 16 פיקסלים למשבצת ולא אחד. שתי התמונות כאן מתארות את
     * אותו מבוך בשני קני מידה, והפענוח חייב להיות זהה.
     */
    private static void testScaledAndUnscaledImagesAgree() {
        String[] rows = {"..#..", "#.#.#", "....#"};
        MazeGrid plain = MazeGrid.fromImage(image(rows, 1), 5, 3);
        MazeGrid scaled = MazeGrid.fromImage(image(rows, 16), 5, 3);

        boolean identical = true;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                identical &= plain.isOpen(col, row) == scaled.isOpen(col, row);
                identical &= plain.isOpen(col, row) == (rows[row].charAt(col) == '.');
            }
        }
        check("1x and 16x images decode identically", identical);
        check("grid size is the requested size", scaled.getWidth() == 5 && scaled.getHeight() == 3);
    }

    private static void testOnlyWhiteIsOpen() {
        int[] wallColours = {0x000000, 0xFF0000, 0x3893D3, 0xCD2149, 0xFEFEFE - 0x404040};
        boolean allWalls = true;
        for (int colour : wallColours) {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, colour);
            allWalls &= !MazeGrid.fromImage(image, 1, 1).isOpen(0, 0);
        }
        check("every non white pixel is a wall", allWalls);

        BufferedImage white = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        white.setRGB(0, 0, 0xFFFFFF);
        check("a white pixel is a passage", MazeGrid.fromImage(white, 1, 1).isOpen(0, 0));
    }

    /**
     * מבוך לא ריבועי עם דפוס א-סימטרי. זו הבדיקה שתופסת החלפה בין שורה לעמודה,
     * שגיאה שמבוך ריבועי מסתיר לחלוטין.
     */
    private static void testKeepsRowsAndColumnsApart() {
        MazeGrid grid = MazeGrid.fromImage(image(new String[]{".....", "....#", "....."}, 16), 5, 3);

        check("wall is at column 4 row 1", !grid.isOpen(4, 1));
        check("column 1 row 4 is outside the maze", !grid.isOpen(1, 4));
        check("its neighbours are open", grid.isOpen(3, 1) && grid.isOpen(4, 0) && grid.isOpen(4, 2));
    }

    private static void testRejectsImagesSmallerThanTheMaze() {
        BufferedImage tiny = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        check("an undersized image is rejected", throwsIllegalArgument(() -> MazeGrid.fromImage(tiny, 10, 10)));
        check("a non positive size is rejected", throwsIllegalArgument(() -> MazeGrid.fromImage(tiny, 0, 4)));
    }

    // ------------------------------------------------------------------
    // MazeSolver
    // ------------------------------------------------------------------

    private static void testStraightCorridor() {
        List<Point> path = solve(".....", "#####", "#####");
        check("no path when the goal row is walled off", path.isEmpty());

        path = solve("....#", "####.", "#####");
        check("no path when the goal is a wall", path.isEmpty());

        // מסלול יחיד: לאורך השורה העליונה ואז למטה בעמודה האחרונה
        path = solve(".....", "####.", ".....");
        check("corridor is solved", !path.isEmpty());
        check("corridor length is 7", path.size() == 7);
        checkPath("corridor", path, ".....", "####.", ".....");
    }

    private static void testSingleCell() {
        List<Point> path = solve(".");
        check("a single open cell is a path of one", path.size() == 1);
        check("that cell is the origin", path.get(0).equals(new Point(0, 0)));
    }

    private static void testBlockedStart() {
        check("a walled start has no solution", solve("#..", "...", "...").isEmpty());
    }

    private static void testBlockedGoal() {
        check("a walled goal has no solution", solve("...", "...", "..#").isEmpty());
    }

    /**
     * שני הקצוות פתוחים אך אינם מחוברים. זהו התרחיש שהשרת מייצר בפועל בכמחצית
     * מהמבוכים, ולכן הוא חייב בדיקה משלו ולא רק "אין פתרון" בגלל קיר בקצה.
     */
    private static void testDisconnectedComponents() {
        List<Point> path = solve("..#..", "..#..", "..#..");
        check("both endpoints are open but separated", path.isEmpty());
    }

    /**
     * BFS מבטיח את המסלול הקצר. במבוך הזה יש מסלול באורך 5 סביב קיר יחיד, וגם
     * עקיפה באורך 7 באותו כיוון הפוך. חיפוש לעומק היה עלול להחזיר את הארוך.
     */
    private static void testReturnsTheShortestPath() {
        List<Point> path = solve("...", ".#.", "...");
        check("shortest of two routes is chosen", path.size() == 5);
        checkPath("two routes", path, "...", ".#.", "...");
    }

    private static void testMovesInFourDirectionsOnly() {
        // מעבר אלכסוני בלבד בין שני החצאים - ולכן אין מסלול חוקי
        check("diagonal moves are not allowed", solve(".#", "#.").isEmpty());
    }

    // ------------------------------------------------------------------
    // Live server
    // ------------------------------------------------------------------

    private static void testLiveConfig() {
        try {
            RenderConfig config = ApiService.fetchRenderConfig();
            check("live config has colours", config.getWallColor() != null
                    && config.getPathColor() != null && config.getGridColor() != null);
            check("live config has a non negative delay", config.getAnimationDelayMs() >= 0);
        } catch (Exception e) {
            check("live config request: " + e.getMessage(), false);
        }
    }

    /**
     * מוודא גם שקנה המידה של התמונה נשאר שלם. אם השרת ישנה אותו אי פעם, עדיף
     * שבדיקה תתריע מאשר שהמבוך ייראה אקראי.
     */
    private static void testLiveMazes() {
        int[][] sizes = {{5, 5}, {30, 30}, {100, 100}, {7, 53}};
        for (int[] size : sizes) {
            int width = size[0];
            int height = size[1];
            try {
                BufferedImage image = ApiService.fetchMazeImage(width, height);
                check(width + "x" + height + " image is at least the maze size",
                        image.getWidth() >= width && image.getHeight() >= height);
                check(width + "x" + height + " image scale is whole",
                        image.getWidth() % width == 0 && image.getHeight() % height == 0);

                MazeGrid grid = MazeGrid.fromImage(image, width, height);
                List<Point> path = MazeSolver.solve(grid);
                check(width + "x" + height + " path is valid", isValidPath(path, grid));
            } catch (Exception e) {
                check(width + "x" + height + " request: " + e.getMessage(), false);
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** בונה מבוך משורות טקסט ומחזיר את הפתרון. {@code '.'} מעבר, כל השאר קיר. */
    private static List<Point> solve(String... rows) {
        return MazeSolver.solve(MazeGrid.fromImage(image(rows, 1), rows[0].length(), rows.length));
    }

    /** בונה תמונה משורות טקסט, כאשר כל משבצת היא בלוק בגודל {@code scale}. */
    private static BufferedImage image(String[] rows, int scale) {
        int width = rows[0].length() * scale;
        int height = rows.length * scale;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean open = rows[y / scale].charAt(x / scale) == '.';
                image.setRGB(x, y, open ? 0xFFFFFF : 0x2B2B2B);
            }
        }
        return image;
    }

    /** מוודא שהמסלול חוקי מול המבוך שממנו הוא נוצר. */
    private static void checkPath(String name, List<Point> path, String... rows) {
        MazeGrid grid = MazeGrid.fromImage(image(rows, 1), rows[0].length(), rows.length);
        check(name + " path is valid", isValidPath(path, grid));
    }

    /**
     * מסלול חוקי מתחיל בפינה השמאלית העליונה, מסתיים בימנית התחתונה, עובר רק
     * במשבצות פתוחות, מתקדם צעד אחד בכל פעם ואינו חוזר על משבצת.
     * מסלול ריק נחשב חוקי - הוא המשמעות של "אין פתרון".
     */
    private static boolean isValidPath(List<Point> path, MazeGrid grid) {
        if (path.isEmpty()) {
            return true;
        }
        if (!path.get(0).equals(new Point(0, 0))) {
            return false;
        }
        if (!path.get(path.size() - 1).equals(new Point(grid.getWidth() - 1, grid.getHeight() - 1))) {
            return false;
        }

        Set<Point> seen = new HashSet<>();
        for (int i = 0; i < path.size(); i++) {
            Point cell = path.get(i);
            if (!grid.isOpen(cell.x, cell.y) || !seen.add(cell)) {
                return false;
            }
            if (i > 0) {
                Point previous = path.get(i - 1);
                if (Math.abs(cell.x - previous.x) + Math.abs(cell.y - previous.y) != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String json(String wall, String path, String drawGrid, String grid, String delay) {
        return "{\"wallCellColor\":\"" + wall + "\",\"pathColor\":\"" + path + "\",\"drawGrid\":" + drawGrid
                + ",\"gridColor\":\"" + grid + "\",\"animationDelayMs\":" + delay + "}";
    }

    private static boolean throwsIllegalArgument(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static boolean isLive(String[] args) {
        for (String arg : args) {
            if ("--live".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void section(String name) {
        System.out.println();
        System.out.println(name);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ok    " + description);
        } else {
            failed++;
            System.out.println("  FAIL  " + description);
        }
    }
}
