public class Main {
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private static final int MAX_LEVEL = 9;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java DWTCompression <path_to_image.rgb> <level>");
            return;
        }

        String imagePath = args[0];
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Level must be an integer");
            return;
        }

        if (level < 0 || level > 9) {
            System.err.println("Level must be between 0 and 9");
            return;
        }

        executeCompression(imagePath, level);
    }

    public static void executeCompression(String imagePath, int level) {
        System.out.println("Starting compression for " + imagePath + " at n = " + level);

        // DWT.progressiveDecoding(transformedImage, level);

        byte[][][] image = ImageIO.initByteArray(imagePath);

        float[][][] floatImage = byteToFloat(image);
        for (int c = 0; c < 3; c++) {
            float[][] blah = new float[HEIGHT][WIDTH];
            for (int h = 0; h < HEIGHT; h++) {
                for (int w = 0; w < WIDTH; w++) {
                    blah[h][w] = floatImage[h][w][c];
                }
            }
            blah = DWT.recursiveDWT(blah, c, level, MAX_LEVEL);
            // blah = DWT.zeroHighPassCoefficients(blah, c);
            for (int h = 0; h < HEIGHT; h++) {
                for (int w = 0; w < WIDTH; w++) {
                    floatImage[h][w][c] = blah[h][w];
                }
            }
        }

        float[][][] reconstructedImage = new float[HEIGHT][WIDTH][3];
        for (int c = 0; c < 3; c++) {
            float[][] blah = new float[HEIGHT][WIDTH];
            for (int h = 0; h < HEIGHT; h++) {
                for (int w = 0; w < WIDTH; w++) {
                    blah[h][w] = floatImage[h][w][c];
                }
            }
            blah = DWT.recursiveInverseDWT(blah, c, level, MAX_LEVEL);
            for (int h = 0; h < HEIGHT; h++) {
                for (int w = 0; w < WIDTH; w++) {
                    reconstructedImage[h][w][c] = blah[h][w];
                }
            }
        }

        byte[][][] finalImage = floatToByte(reconstructedImage);
        ImageIO.showImage(finalImage);
    }

    private static float[][][] byteToFloat(byte[][][] image) {
        float[][][] floatImage = new float[HEIGHT][WIDTH][3];

        for (int h = 0; h < HEIGHT; h++) {
            for (int w = 0; w < WIDTH; w++) {
                for (int c = 0; c < 3; c++) {
                    floatImage[h][w][c] = (image[h][w][c] & 0xFF) / 255.0f;
                }
            }
        }

        return floatImage;
    }

    private static byte[][][] floatToByte(float[][][] floatImage) {
        byte[][][] image = new byte[HEIGHT][WIDTH][3];

        for (int h = 0; h < HEIGHT; h++) {
            for (int w = 0; w < WIDTH; w++) {
                for (int c = 0; c < 3; c++) {
                    image[h][w][c] = (byte) Math.min(255, Math.max(0, Math.round(floatImage[h][w][c] * 255)));
                }
            }
        }

        return image;
    }
}
