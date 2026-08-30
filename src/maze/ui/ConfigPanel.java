package maze.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import maze.model.RenderConfig;

/**
 * המסך הראשון: הצגת הגדרות הציור שהתקבלו מהשרת, ובחירת גודל המבוך.
 * <p>
 * הפאנל אינו יודע דבר על רשת או על ניווט בין מסכים. הוא מקבל שני
 * {@link ActionListener} מבחוץ, ו-{@link MainFrame} הוא שמחליט מה קורה בלחיצה.
 */
public class ConfigPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** טווח הגדלים המותר, וגודל ברירת המחדל שמחליף כל ערך לא חוקי. */
    private static final int MIN_SIZE = 5;
    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 30;

    private final ColorField wallColorField = new ColorField();
    private final ColorField pathColorField = new ColorField();
    private final ColorField gridColorField = new ColorField();
    private final JLabel drawGridValue = new JLabel("-");
    private final JLabel animationDelayValue = new JLabel("-");

    private final JTextField widthField = new JTextField(String.valueOf(DEFAULT_SIZE), 5);
    private final JTextField heightField = new JTextField(String.valueOf(DEFAULT_SIZE), 5);

    private final JButton refreshButton = new JButton("Refresh Config");
    private final JButton getMazeButton = new JButton("GET MAZE");
    private final JLabel statusLabel = new JLabel(" ");

    /**
     * @param onRefresh מופעל בלחיצה על Refresh Config
     * @param onGetMaze מופעל בלחיצה על GET MAZE
     */
    public ConfigPanel(ActionListener onRefresh, ActionListener onGetMaze) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        add(buildSettingsSection(), BorderLayout.NORTH);
        add(buildSizeSection(), BorderLayout.CENTER);
        add(buildActionSection(), BorderLayout.SOUTH);

        refreshButton.addActionListener(onRefresh);
        getMazeButton.addActionListener(onGetMaze);

        // אין מה לשלוף עד שההגדרות הגיעו: בלי הגדרות אי אפשר לצייר את המבוך.
        getMazeButton.setEnabled(false);
    }

    /** חמש הגדרות הציור שהתקבלו מהשרת. */
    private JPanel buildSettingsSection() {
        JPanel settings = new JPanel(new GridLayout(5, 2, 12, 8));
        settings.add(fieldLabel("Wall color"));
        settings.add(wallColorField);
        settings.add(fieldLabel("Path color"));
        settings.add(pathColorField);
        settings.add(fieldLabel("Draw grid"));
        settings.add(drawGridValue);
        settings.add(fieldLabel("Grid color"));
        settings.add(gridColorField);
        settings.add(fieldLabel("Animation delay"));
        settings.add(animationDelayValue);

        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.add(sectionTitle("Render settings received from the server"), BorderLayout.NORTH);
        section.add(settings, BorderLayout.CENTER);
        return section;
    }

    /** שדות הרוחב והגובה. */
    private JPanel buildSizeSection() {
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fields.add(new JLabel("Width:"));
        fields.add(widthField);
        fields.add(new JLabel("Height:"));
        fields.add(heightField);
        fields.add(new JLabel("(" + MIN_SIZE + "-" + MAX_SIZE + ", default " + DEFAULT_SIZE + ")"));

        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBorder(BorderFactory.createEmptyBorder(20, 0, 12, 0));
        section.add(sectionTitle("Maze size"), BorderLayout.NORTH);
        section.add(fields, BorderLayout.CENTER);
        return section;
    }

    /** הכפתורים ושורת המצב. */
    private JPanel buildActionSection() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(refreshButton);
        buttons.add(getMazeButton);

        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        section.add(buttons);
        section.add(statusLabel);
        return section;
    }

    /**
     * מציג הגדרות שהתקבלו מהשרת ומאפשר לשלוף מבוך.
     *
     * @param config ההגדרות להצגה
     */
    public void showConfig(RenderConfig config) {
        wallColorField.setColor(config.getWallColor());
        pathColorField.setColor(config.getPathColor());
        gridColorField.setColor(config.getGridColor());
        drawGridValue.setText(String.valueOf(config.isDrawGrid()));
        animationDelayValue.setText(config.getAnimationDelayMs() + " ms");
        getMazeButton.setEnabled(true);
    }

    /**
     * הרוחב שנבחר, לאחר בדיקה ותיקון.
     * <p>
     * הערך המתוקן נכתב חזרה לשדה, כדי שהמשתמש יראה באיזה גודל התוכנית באמת
     * משתמשת ולא יישאר מול הערך הפסול שהקליד.
     *
     * @return ערך בטווח המותר
     */
    public int getRequestedWidth() {
        return readSize(widthField);
    }

    /** הגובה שנבחר, לאחר אותה בדיקה ותיקון כמו ב-{@link #getRequestedWidth()}. */
    public int getRequestedHeight() {
        return readSize(heightField);
    }

    /**
     * נועל את הפאנל בזמן בקשה לשרת, כדי שלחיצות חוזרות לא ייצרו בקשות מקבילות.
     *
     * @param busy    האם בקשה מתבצעת כרגע
     * @param message הודעה לשורת המצב
     */
    public void setBusy(boolean busy, String message) {
        refreshButton.setEnabled(!busy);
        // GET MAZE נשאר מנוטרל גם אחרי הבקשה כל עוד אין הגדרות להציג.
        getMazeButton.setEnabled(!busy && wallColorField.hasColor());
        widthField.setEnabled(!busy);
        heightField.setEnabled(!busy);
        statusLabel.setText(message == null || message.isEmpty() ? " " : message);
    }

    /** קורא מספר מהשדה, מתקן ערך לא חוקי, וכותב את התוצאה חזרה לשדה. */
    private static int readSize(JTextField field) {
        int size = DEFAULT_SIZE;
        try {
            int typed = Integer.parseInt(field.getText().trim());
            if (typed >= MIN_SIZE && typed <= MAX_SIZE) {
                size = typed;
            }
        } catch (NumberFormatException ignored) {
            // טקסט שאינו מספר מטופל בדיוק כמו מספר מחוץ לטווח
        }
        field.setText(String.valueOf(size));
        return size;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        return label;
    }

    private static JLabel sectionTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        return title;
    }

    /**
     * מציג צבע כריבוע צבוע לצד ערכו ההקסדצימלי, כדי שהמשתמש יראה את הצבע עצמו
     * ולא רק את המחרוזת שהגיעה מהשרת.
     */
    private static final class ColorField extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final Dimension SWATCH_SIZE = new Dimension(18, 18);

        private final JPanel swatch = new JPanel();
        private final JLabel hexLabel = new JLabel("-");
        private boolean hasColor;

        ColorField() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
            setOpaque(false);
            swatch.setPreferredSize(SWATCH_SIZE);
            swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            swatch.setVisible(false);
            add(swatch);
            add(hexLabel);
        }

        void setColor(Color color) {
            swatch.setBackground(color);
            swatch.setVisible(true);
            hexLabel.setText(String.format("#%06X", color.getRGB() & 0xFFFFFF));
            hasColor = true;
        }

        boolean hasColor() {
            return hasColor;
        }
    }
}
