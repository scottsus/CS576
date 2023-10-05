import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.*;

public class ImageProcessor {
    JFrame frame;
    JLabel label;
    BufferedImage inputImage;
    List<BufferedImage> objects = new ArrayList<>();
    List<Rectangle> allDetectedObjects = new ArrayList<>();
    int width = 640;
    int height = 480;
    double THRESHOLD = 0.1;
    final int INTENSITY_THRESHOLD = 32;

    private BufferedImage processImage() {
        BufferedImage image = null;

        int[][] inputImageHistogram = createHistogram(inputImage, false);
        for (BufferedImage object : objects) {
            int[][] objectHistogram = createHistogram(object, true);
            double similarity = compareHistograms(inputImageHistogram, objectHistogram);
            if (similarity > THRESHOLD) {
                BufferedImage probabilityImage = backProjectHistogram(inputImageHistogram, objectHistogram);
                BufferedImage refinedImage = refineImage(probabilityImage);
                List<Rectangle> detectedObjects = findBoundingBoxes(refinedImage);
                allDetectedObjects.addAll(detectedObjects);
                image = probabilityImage;
            }
        }

        return image;
    }

    private BufferedImage refineImage(BufferedImage probabilityImage) {
        BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                int intensity = new Color(probabilityImage.getRGB(w, h)).getRed();
                if (intensity >= INTENSITY_THRESHOLD) {
                    thresholdedImage.setRGB(w, h, Color.WHITE.getRGB());
                } else {
                    thresholdedImage.setRGB(w, h, Color.BLACK.getRGB());
                }
            }
        }

        int kernelSize = 10;
        BufferedImage refinedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_BYTE_BINARY);
        for (int w = kernelSize; w < width - kernelSize; w++) {
            for (int h = kernelSize; h < height - kernelSize; h++) {
                boolean isEroded = true;
                for (int i = -kernelSize; i <= kernelSize; i++) {
                    for (int j = -kernelSize; j <= kernelSize; j++) {
                        Color color = new Color(thresholdedImage.getRGB(w + i, h + j));
                        if (color.equals(Color.BLACK)) {
                            isEroded = false;
                            break;
                        }
                    }
                    if (!isEroded)
                        break;
                }
                refinedImage.setRGB(w, h, isEroded ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }

        BufferedImage dilatedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_BYTE_BINARY);
        for (int w = kernelSize; w < width - kernelSize; w++) {
            for (int h = kernelSize; h < height - kernelSize; h++) {
                boolean isDilated = false;
                for (int i = -kernelSize; i <= kernelSize; i++) {
                    for (int j = -kernelSize; j < kernelSize; j++) {
                        Color color = new Color(refinedImage.getRGB(w + i, h + j));
                        if (color.equals(Color.WHITE)) {
                            isDilated = true;
                            break;
                        }
                    }
                    if (isDilated)
                        break;
                }
                dilatedImage.setRGB(w, h, isDilated ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }

        return dilatedImage;
        // return refinedImage;
    }

    private List<Rectangle> findBoundingBoxes(BufferedImage probabilityImage) {
        boolean[][] visited = new boolean[width][height];
        List<Rectangle> boundingBoxes = new ArrayList<>();

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                if (!visited[w][h]) {
                    Rectangle boundingBox = BFS(w, h, probabilityImage, visited);
                    if (boundingBox != null) {
                        boundingBoxes.add(boundingBox);
                    }
                }
            }
        }

        return boundingBoxes;
    }

    private Rectangle BFS(int startW, int startH, BufferedImage probabilityImage, boolean[][] visited) {
        int minW = startW, maxW = startW;
        int minH = startH, maxH = startH;
        Queue<int[]> q = new LinkedList<>();
        q.offer(new int[] { startW, startH });

        while (!q.isEmpty()) {
            int[] coord = q.poll();
            int w = coord[0], h = coord[1];

            if (w < 0 || w >= width || h < 0 || h >= height)
                continue;
            if (visited[w][h])
                continue;
            int intensity = new Color(probabilityImage.getRGB(w, h)).getRed();
            if (intensity <= INTENSITY_THRESHOLD)
                continue;

            visited[w][h] = true;
            minW = Math.min(w, minW);
            maxW = Math.max(w, maxW);
            minH = Math.min(h, minH);
            maxH = Math.max(h, maxH);

            q.offer(new int[] { w - 1, h });
            q.offer(new int[] { w + 1, h });
            q.offer(new int[] { w, h - 1 });
            q.offer(new int[] { w, h + 1 });
        }

        if (minW == maxW || minH == maxH) {
            return null;
        }
        return new Rectangle(minW, minH, maxW - minW, maxH - minH);
    }

    private BufferedImage backProjectHistogram(int[][] inputImageHistogram, int[][] objectHistogram) {
        BufferedImage backProjectedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Color color = new Color(inputImage.getRGB(w, h));
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

    private double compareHistograms(int[][] h1, int[][] h2) {
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

    private int[][] createHistogram(BufferedImage image, boolean isObject) {
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

    private boolean isGreenChroma(int r, int g, int b) {
        return g > 150 && r < 100 && b < 100;
    }

    private int[] RGBtoYUV(int r, int g, int b) {
        int y = (int) (0.257 * r + 0.504 * g + 0.098 * b + 16);
        int u = (int) (-0.148 * r - 0.291 * g + 0.439 * b + 128);
        int v = (int) (0.439 * r - 0.368 * g - 0.071 * b + 128);

        return new int[] { y, u, v };
    }

    private void initializeImages(String inputImageFilePath, List<String> objectFilePaths) {
        inputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, inputImageFilePath, inputImage);

        for (String objectFilePath : objectFilePaths) {
            BufferedImage object = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            readImageRGB(width, height, objectFilePath, object);
            objects.add(object);
        }
    }

    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            int frameLength = width * height * 3;

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    // int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }

            raf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawBoundingBoxes(BufferedImage image, List<Rectangle> boundingBoxes) {
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        for (Rectangle box : boundingBoxes) {
            g.drawRect(box.x, box.y, box.width, box.height);
        }
        g.dispose();
    }

    private void showImages(BufferedImage image) {
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        label = new JLabel(new ImageIcon(image));

        // Draw image
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(label, c);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Incorrect number of arguments. Expected >2, got " +
                    args.length);
            return;
        }

        String inputImageFilePath = args[0];
        List<String> objectFilePaths = new ArrayList<>();
        for (int i = 1; i < args.length; i++)
            objectFilePaths.add(args[i]);

        ImageProcessor imProc = new ImageProcessor();
        imProc.initializeImages(inputImageFilePath, objectFilePaths);
        BufferedImage image = imProc.processImage();

        if (image == null) {
            System.out.println("No image to display");
            return;
        }

        System.out.println("Displaying image...");
        imProc.drawBoundingBoxes(imProc.inputImage, imProc.allDetectedObjects);
        imProc.showImages(imProc.inputImage);
    }
}