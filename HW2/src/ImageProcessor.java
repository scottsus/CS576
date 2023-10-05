import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.awt.*;
import java.awt.image.*;

public class ImageProcessor {
    private BufferedImage inputImage;
    List<BufferedImage> objects = new ArrayList<>();
    List<Rectangle> allDetectedObjects = new ArrayList<>();
    final int width = 640;
    final int height = 480;
    final double SIMILARITY_THRESHOLD = 0.1;
    final double INTENSITY_THRESHOLD = 1;

    private HistogramProcessor histogramProcessor = new HistogramProcessor(width, height);
    private ImageIO imageIO = new ImageIO();

    private BufferedImage processImage() {
        BufferedImage image = null;

        int[][] inputImageHistogram = histogramProcessor.createHistogram(inputImage, false);
        for (BufferedImage object : objects) {
            int[][] objectHistogram = histogramProcessor.createHistogram(object, true);
            double similarity = histogramProcessor.compareHistograms(inputImageHistogram, objectHistogram);
            if (similarity > SIMILARITY_THRESHOLD) {
                BufferedImage probabilityImage = histogramProcessor.backProjectHistogram(inputImage,
                        inputImageHistogram, objectHistogram);
                BufferedImage refinedImage = refineImage(probabilityImage);
                List<Rectangle> detectedObjects = findBoundingBoxes(refinedImage);
                allDetectedObjects.addAll(detectedObjects);
                image = probabilityImage;
            }
        }

        return image;
    }

    private BufferedImage refineImage(BufferedImage probabilityImage) {
        BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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

        int erosionKernelSize = 50;
        BufferedImage erodedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int w = erosionKernelSize; w < width - erosionKernelSize; w++) {
            for (int h = erosionKernelSize; h < height - erosionKernelSize; h++) {
                boolean isEroded = true;
                for (int i = -erosionKernelSize; i <= erosionKernelSize; i++) {
                    for (int j = -erosionKernelSize; j <= erosionKernelSize; j++) {
                        Color color = new Color(thresholdedImage.getRGB(w + i, h + j));
                        if (color.equals(Color.BLACK)) {
                            isEroded = false;
                            break;
                        }
                    }
                    if (!isEroded)
                        break;
                }
                erodedImage.setRGB(w, h, isEroded ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }

        int dilationKernelSize = 50;
        BufferedImage dilatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int w = dilationKernelSize; w < width - dilationKernelSize; w++) {
            for (int h = dilationKernelSize; h < height - dilationKernelSize; h++) {
                boolean isDilated = false;
                for (int i = -dilationKernelSize; i <= dilationKernelSize; i++) {
                    for (int j = -dilationKernelSize; j < dilationKernelSize; j++) {
                        Color color = new Color(erodedImage.getRGB(w + i, h + j));
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
    }

    private List<Rectangle> findBoundingBoxes(BufferedImage image) {
        boolean[][] visited = new boolean[width][height];
        List<Rectangle> boundingBoxes = new ArrayList<>();

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                if (!visited[w][h]) {
                    Rectangle boundingBox = BFS(w, h, image, visited);
                    if (boundingBox != null) {
                        boundingBoxes.add(boundingBox);
                    }
                }
            }
        }

        return boundingBoxes;
    }

    private Rectangle BFS(int startW, int startH, BufferedImage image, boolean[][] visited) {
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
            int intensity = new Color(image.getRGB(w, h)).getRed();
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

    private void initializeImages(String inputImageFilePath, List<String> objectFilePaths) {
        inputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        imageIO.readImageRGB(width, height, inputImageFilePath, inputImage);

        for (String objectFilePath : objectFilePaths) {
            BufferedImage object = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            imageIO.readImageRGB(width, height, objectFilePath, object);
            objects.add(object);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Incorrect number of arguments. Expected >2, got " +
                    args.length);
            return;

        }

        String inputImageFilePath = "";
        List<String> objectFilePaths = new ArrayList<>();

        String pathFlag = args[0];
        if (pathFlag.equals("--oneObj")) {
            String filePrefix = "../dataset/data_sample_rgb/";
            String imageSuffix = "_image.rgb";
            String objectSuffix = "_object.rgb";
            inputImageFilePath = filePrefix + args[1] + imageSuffix;
            objectFilePaths.add(filePrefix + args[1] + objectSuffix);
        } else {
            inputImageFilePath = args[0];
            for (int i = 1; i < args.length; i++)
                objectFilePaths.add(args[i]);
        }

        ImageProcessor imProc = new ImageProcessor();
        imProc.initializeImages(inputImageFilePath, objectFilePaths);
        BufferedImage image = imProc.processImage();

        if (image == null) {
            System.out.println("No image to display");
            return;
        }

        System.out.println("Displaying image...");
        imProc.imageIO.drawBoundingBoxes(imProc.inputImage, imProc.allDetectedObjects);
        imProc.imageIO.showImages(imProc.inputImage);
    }
}