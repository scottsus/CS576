import java.awt.*;
import java.awt.image.*;

public class HistogramProcessor {
    private int width;
    private int height;

    public HistogramProcessor(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int[][] createHistogram(BufferedImage image, boolean isObject) {
        int[] uHistogram = new int[256];
        int[] vHistogram = new int[256];

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Color color = new Color(image.getRGB(w, h));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();

                if (isObject && isGreenChroma(r, g, b))
                    continue;

                int[] yuv = RGBtoYUV(r, g, b);
                int u = yuv[1];
                int v = yuv[2];

                uHistogram[u]++;
                vHistogram[v]++;
            }
        }

        return new int[][] { uHistogram, vHistogram };
    }

    public BufferedImage backProjectHistogram(BufferedImage image, int[][] inputImageHistogram,
            int[][] objectHistogram) {
        BufferedImage backProjectedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Color color = new Color(image.getRGB(w, h));
                int r = color.getRed();
                int b = color.getBlue();
                int g = color.getGreen();

                int[] yuv = RGBtoYUV(r, g, b);
                int u = yuv[1], v = yuv[2];
                double uRatio = (objectHistogram[0][u] + 1.0) / (inputImageHistogram[0][u] + 1.0);
                double vRatio = (objectHistogram[1][v] + 1.0) / (inputImageHistogram[1][v] + 1.0);

                double avgRatio = (uRatio + vRatio) / 2.0;

                // Normalize to the range 0-255
                int intensity = (int) (255 * avgRatio);
                intensity = Math.min(255, Math.max(0, intensity));

                Color newColor = new Color(intensity, intensity, intensity);
                backProjectedImage.setRGB(w, h, newColor.getRGB());
            }
        }

        return backProjectedImage;
    }

    public double compareHistograms(int[][] h1, int[][] h2) {
        double uComparison = compareHistograms(h1[0], h2[0]);
        double vComparison = compareHistograms(h1[1], h2[1]);
        return (uComparison + vComparison) / 2.0; // Average comparison across both channels
    }

    private double compareHistograms(int[] h1, int[] h2) {
        double chiSquareValue = 0.0;

        for (int i = 0; i < h1.length; i++) {
            if (h1[i] + h2[i] == 0) // Avoid division by zero
                continue;
            chiSquareValue += (Math.pow(h1[i] - h2[i], 2)) / (double) (h1[i] + h2[i]);
        }

        return chiSquareValue;
    }

    private boolean isGreenChroma(int r, int g, int b) {
        return g > 200 && r < 100 && b < 100;
    }

    private int[] RGBtoYUV(int r, int g, int b) {
        int y = (int) (0.257 * r + 0.504 * g + 0.098 * b + 16);
        int u = (int) (-0.148 * r - 0.291 * g + 0.439 * b + 128);
        int v = (int) (0.439 * r - 0.368 * g - 0.071 * b + 128);

        return new int[] { y, u, v };
    }

}
