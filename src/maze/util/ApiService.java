package maze.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import maze.model.RenderConfig;

/**
 * קריאות ה-HTTP אל שרת המבוך.
 * <p>
 * שתי המתודות הציבוריות חוסמות עד לקבלת התשובה, ולכן אין לקרוא להן מתוך
 * Event Dispatch Thread של Swing. שכבת התצוגה מפעילה אותן דרך {@code SwingWorker}.
 */
public final class ApiService {

    private static final String BASE_URL = "https://shaitest-production-3066.up.railway.app/fm1";

    /** אין להמתין לחיבור יותר מכך. שרת שאינו זמין ייכשל במקום להקפיא את הבקשה. */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /** יצירת מבוך 100x100 בצד השרת אורכת זמן, ולכן זמן הקריאה נדיב יותר. */
    private static final int READ_TIMEOUT_MS = 30_000;

    private ApiService() {
    }

    /**
     * שולף את הגדרות הציור מהשרת.
     *
     * @return ההגדרות שהתקבלו
     * @throws IOException אם הבקשה נכשלה או שהתשובה אינה תקינה
     */
    public static RenderConfig fetchRenderConfig() throws IOException {
        HttpURLConnection connection = openConnection("/get-render-config");
        try (InputStream input = connection.getInputStream()) {
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return RenderConfig.parse(json);
        } catch (IllegalArgumentException e) {
            // תשובה שהתקבלה במלואה אך אינה תקינה. נעטפת כדי שלקוחות המחלקה
            // יצטרכו לטפל בסוג חריגה אחד בלבד.
            throw new IOException("תשובת ההגדרות מהשרת אינה תקינה: " + e.getMessage(), e);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * שולף מהשרת תמונת מבוך בגודל המבוקש.
     * <p>
     * התמונה משמשת כקלט בלבד - היא אינה מוצגת למשתמש, אלא רק מפוענחת
     * על ידי הקוד שמסיק ממנה איפה קיר ואיפה מעבר.
     *
     * @param width  רוחב המבוך במשבצות
     * @param height גובה המבוך במשבצות
     * @return התמונה שהתקבלה
     * @throws IOException אם הבקשה נכשלה או שהתשובה אינה תמונה שניתן לפענח
     */
    public static BufferedImage fetchMazeImage(int width, int height) throws IOException {
        HttpURLConnection connection = openConnection("/get-maze-image?width=" + width + "&height=" + height);
        try (InputStream input = connection.getInputStream()) {
            BufferedImage image = ImageIO.read(input);

            // ImageIO.read מחזיר null - ולא זורק - כשאין קורא מתאים לתוכן שהתקבל.
            // בלי הבדיקה הזו ה-null היה מתפוצץ הרבה יותר מאוחר, בתוך הפענוח.
            if (image == null) {
                throw new IOException("השרת החזיר תוכן שאינו תמונה שניתן לקרוא");
            }
            return image;
        } finally {
            connection.disconnect();
        }
    }

    /** פותח חיבור עם timeout ומוודא שהשרת החזיר 200 לפני שקוראים את גוף התשובה. */
    private static HttpURLConnection openConnection(String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(BASE_URL + path).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("השרת החזיר סטטוס " + status + " עבור " + path);
        }
        return connection;
    }
}
