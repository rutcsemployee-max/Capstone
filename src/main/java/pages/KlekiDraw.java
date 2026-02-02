package pages;

import browser.Browser;
import locators.KlekiPage;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;


/**
 * KlekiFullDrawFixed
 * - Loads stroke-separated lineart.dat (from the Python pre-bake)
 * - Computes correct aspect-preserving scale and centers the drawing
 * - Inverts Y so the artwork is upright
 * - Calls KL.draw(stroke) once per stroke (no teleport lines)
 * - Saves canvas screenshot to output/result.png
 * <p>
 * Note: no change required to the Python baking step.
 */
public class KlekiDraw extends Browser {

    static class Point {
        double x, y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Stroke {
        List<Point> pts = new ArrayList<>();
    }

    // CONFIG
    private static final String DATA_FILE = "/home/labuser/IdeaProjects/Capstone/src/main/resources/lineart2.dat";
    private static final String OUTPUT_IMAGE = "output/result"+Instant.now()+".png";

    // PERFORMANCE TUNING
    // 1 = every point, 2 = every 2nd point, etc. Increase to speed up.
    private static final int POINT_STEP = 1;

    // Padding around drawing inside the canvas (pixels)
    private static final double PADDING = 60.0;

    public static void draw() throws Exception {
        //WebDriver driver = new FirefoxDriver();
        //driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        Browser.launch();
        Browser.navigate("https://kleki.com/");
        try {
            // wait for Kleki to initialize; increase if your connection is slow

            new WebDriverWait(driver, Duration.ofSeconds(8)).until(
                    webDriver -> (((JavascriptExecutor) webDriver).executeScript("return !!(window.KL && typeof KL.draw === 'function');"))==null?false:(((JavascriptExecutor) webDriver).executeScript("return !!(window.KL && typeof KL.draw === 'function');"))
            );

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // ensure KL.draw exists (boolean)
//            Boolean ready = (Boolean) js.executeScript(
//                    "return !!(window.KL && typeof KL.draw === 'function');");
//            if (Boolean.FALSE.equals(ready)) {
//                throw new RuntimeException("KL.draw API not available on the page");
//            }

            // load stroke-separated file produced by the Python pre-bake
            List<Stroke> strokes = loadStrokes(DATA_FILE);
            if (strokes.isEmpty()) {
                throw new RuntimeException("No strokes loaded from " + DATA_FILE);
            }

            // compute global bounds across all strokes
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (Stroke s : strokes) {
                for (Point p : s.pts) {
                    if (p.x < minX) minX = p.x;
                    if (p.y < minY) minY = p.y;
                    if (p.x > maxX) maxX = p.x;
                    if (p.y > maxY) maxY = p.y;
                }
            }

            double svgW = Math.max(1e-6, maxX - minX);
            double svgH = Math.max(1e-6, maxY - minY);

            // get canvas pixel size (Number -> double)
            Object cwObj = js.executeScript("return document.querySelector('canvas').width;");
            Object chObj = js.executeScript("return document.querySelector('canvas').height;");
            double cw = ((Number) cwObj).doubleValue();
            double ch = ((Number) chObj).doubleValue();

            // compute usable area (respecting padding) and scale to preserve aspect ratio
            double usableW = Math.max(1e-6, cw - 2.0 * PADDING);
            double usableH = Math.max(1e-6, ch - 2.0 * PADDING);
            double scale = Math.min(usableW / svgW, usableH / svgH);

            double drawW = svgW * scale;
            double drawH = svgH * scale;

            // center the drawing in the canvas (one centering operation)
            double offsetX = (cw - drawW) / 2.0;
            double offsetY = (ch - drawH) / 2.0;

            System.out.println(String.format("Canvas: %.0fx%.0f | svg: %.2fx%.2f | scale: %.4f | strokes: %d",
                    cw, ch, svgW, svgH, scale, strokes.size()));

            reduceBrushSize(driver);

            // draw each stroke separately (one KL.draw per stroke)
            for (int si = 0; si < strokes.size(); si++) {
                Stroke stroke = strokes.get(si);
                if (stroke.pts.size() < 2) continue;

                StringBuilder jsArray = new StringBuilder("[");
                for (int i = 0; i < stroke.pts.size(); i += POINT_STEP) {
                    Point p = stroke.pts.get(i);

                    // normalize (0..1)
                    double nx = (p.x - minX) / svgW;
                    // invert Y so SVG (which may be y-up) maps to canvas y-down
                    double ny = 1.0 - ((p.y - minY) / svgH);

                    // scale into pixel coords within drawW/drawH and center via offsets
                    double x = nx * drawW + offsetX;
                    double y = ny * drawH + offsetY;

                    jsArray.append("{x:").append(String.format("%.2f", x))
                            .append(",y:").append(String.format("%.2f", y)).append("},");
                }
                jsArray.append("]");

                // call Kleki draw API for this single stroke
                js.executeScript("KL.draw(" + jsArray.toString() + ");");

                // brief pause to let Kleki process the stroke chain (still very fast)
                Thread.sleep(25);
            }

            // allow final rendering to settle
            Thread.sleep(300);

            // screenshot the canvas only
            //WebElement canvas = driver.findElement(By.tagName("canvas"));
            captureScreenshot();

        } finally {
            // driver.quit(); // uncomment to close browser automatically
            Browser.close();
        }
    }

    private static void captureScreenshot() {
        File src = KlekiPage.canvas.getScreenshotAs(OutputType.FILE);
        File dest = new File(OUTPUT_IMAGE);
        dest.getParentFile().mkdirs();
        try {
            Files.copy(src.toPath(), dest.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Done. Saved: " + dest.getAbsolutePath());
    }

    //reduce brush thickness
    public static void reduceBrushSize(WebDriver driver) {

        Actions actions = new Actions(driver);
// Locate left toolbar
        KlekiPage.locate();
        actions.moveToElement(KlekiPage.toolbar, -111, 0).click().perform();

    }

    /**
     * Load stroke-separated file. Lines starting with '#' start a new stroke.
     * Each non-comment line should be: x,y  (floats or ints)
     */
    private static List<Stroke> loadStrokes(String file) throws IOException {
        List<Stroke> strokes = new ArrayList<>();
        Stroke current = new Stroke();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("#")) {
                    if (!current.pts.isEmpty()) {
                        strokes.add(current);
                        current = new Stroke();
                    } else {
                        current = new Stroke();
                    }
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                current.pts.add(new Point(x, y));
            }
        }

        if (!current.pts.isEmpty()) strokes.add(current);
        return strokes;
    }
}
