public class Main {
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;

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

        // byte[][][] image = ImageHandler.readImageRGB(imagePath);
        // System.out.println("R: " + image[0][0][0] + ", G: " + image[0][0][1] + ", B:
        // " + image[0][0][2]);

        // float[][][] transformedImage = DWT.forwardDWT(image, level);
        // System.out.println("R: " + transformedImage[0][0][0] + ", G: " +
        // transformedImage[0][0][1] + ", B: "
        // + transformedImage[0][0][2]);

        // float[][][] reconstructedImage = DWT.inverseDWT(transformedImage, level);
        // System.out.println("R: " + reconstructedImage[0][0][0] + ", G: " +
        // reconstructedImage[0][0][1] + ", B: "
        // + reconstructedImage[0][0][2]);

        byte[][][] image = ImageHandler.readImageRGB(imagePath);
        float[][][] floatImage = byteToFloat(image);

        float[][][] transformedImage = new float[WIDTH][HEIGHT][3];
        for (int c = 0; c < 3; c++) {
            float[][] blah = new float[WIDTH][HEIGHT];
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    blah[x][y] = floatImage[x][y][c];
                }
            }
            float[][] blaah = DWT.recursiveDWT(blah, c, 0, level);
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    transformedImage[x][y][c] = blaah[x][y];
                }
            }
        }

        DWT.progressiveDecoding(transformedImage, level);
    }

    private static float[][][] byteToFloat(byte[][][] image) {
        float[][][] floatImage = new float[WIDTH][HEIGHT][3];

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int channel = 0; channel < 3; channel++) {
                    floatImage[x][y][channel] = (float) (image[x][y][channel] & 0xFF); // Convert byte to unsigned value
                }
            }
        }
        return floatImage;
    }

}
