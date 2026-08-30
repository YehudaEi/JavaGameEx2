package maze.util;

import maze.model.RenderConfig;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class ApiService {
    private static final String BASE = "https://shaitest-production-3066.up.railway.app/fm1";

    public static RenderConfig getConfig() throws Exception {
        URL url = new URL(BASE + "/get-render-config");
        Scanner s = new Scanner(url.openStream());
        StringBuilder sb = new StringBuilder();
        while (s.hasNext()) sb.append(s.nextLine());
        s.close();
        return RenderConfig.parse(sb.toString());
    }

    public static BufferedImage getMazeImage(int w, int h) throws Exception {
        URL url = new URL(BASE + "/get-maze-image?width=" + w + "&height=" + h);
        return ImageIO.read(url);
    }
}