public class DWT {
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;

    public static float[][] recursiveDWT(float[][] image, int channel, int level, int maxLevel) {
        if (level == maxLevel) {
            return image;
        }

        int height = image.length;
        int width = image[0].length;
        float[][] transformedImage = new float[height][width];

        for (int h = 0; h < height; h++) {
            transformedImage[h] = transformRow(image[h], channel);
        }

        for (int w = 0; w < width; w++) {
            float[] col = transformCol(getColumn(transformedImage, w), channel);
            for (int h = 0; h < height; h++) {
                transformedImage[h][w] = col[h];
            }
        }

        float[][] quarter = new float[width / 2][height / 2];
        for (int h = 0; h < height / 2; h++) {
            for (int w = 0; w < width / 2; w++) {
                quarter[h][w] = transformedImage[h][w];
            }
        }
        quarter = recursiveDWT(quarter, channel, level + 1, maxLevel);

        for (int h = 0; h < height / 2; h++) {
            for (int w = 0; w < width / 2; w++) {
                transformedImage[h][w] = quarter[h][w];
            }
        }

        return transformedImage;
    }

    public static float[][] recursiveInverseDWT(float[][] transformedImage, int channel, int level, int maxLevel) {
        if (level == maxLevel) {
            return transformedImage;
        }

        int height = transformedImage.length;
        int width = transformedImage[0].length;

        float[][] quarter = new float[height / 2][width / 2];
        for (int h = 0; h < height / 2; h++) {
            for (int w = 0; w < width / 2; w++) {
                quarter[h][w] = transformedImage[h][w];
            }
        }
        quarter = recursiveInverseDWT(quarter, channel, level + 1, maxLevel);

        float[][] image = new float[height][width];
        for (int h = 0; h < height / 2; h++) {
            for (int w = 0; w < width / 2; w++) {
                image[h][w] = quarter[h][w];
            }
        }

        for (int w = 0; w < width; w++) {
            float[] col = inverseTransformCol(getColumn(image, w), channel);
            for (int h = 0; h < height; h++) {
                image[h][w] = col[h];
            }
        }

        for (int h = 0; h < height; h++) {
            image[h] = inverseTransformRow(image[h], channel);
        }

        return image;
    }

    public static void progressiveDecoding(float[][][] transformedImage, int maxLevel) {
        for (int level = maxLevel; level >= 0; level--) {
            float[][][] reconstructedImage = new float[HEIGHT][WIDTH][3];
            for (int c = 0; c < 3; c++) {
                float[][] blah = new float[HEIGHT][WIDTH];
                for (int h = 0; h < HEIGHT; h++) {
                    for (int w = 0; w < WIDTH; w++) {
                        blah[h][w] = transformedImage[h][w][c];
                    }
                }
                float[][] zeroed = zeroHighPassCoefficients(blah, level);
                float[][] blaah = recursiveInverseDWT(zeroed, c, level, maxLevel);
                for (int h = 0; h < HEIGHT; h++) {
                    for (int w = 0; w < WIDTH; w++) {
                        reconstructedImage[h][w][c] = blaah[h][w];
                    }
                }
            }

            System.out.println("Level: " + level);
            // ImageIO.showImage(reconstructedImage);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static float[][] zeroHighPassCoefficients(float[][] image, int level) {
        int startIndex = (int) Math.pow(2, level);

        for (int h = 0; h < HEIGHT; h++) {
            for (int w = startIndex; w < WIDTH; w++) {
                image[h][w] = 0;
            }
        }

        for (int h = startIndex; h < HEIGHT; h++) {
            for (int w = 0; w < WIDTH; w++) {
                image[h][w] = 0;
            }
        }

        return image;
    }

    private static float[] transformRow(float[] row, int channel) {
        float[] transformedRow = new float[row.length];
        int len = row.length / 2;

        for (int i = 0; i < len; i++) {
            float a = (row[2 * i] + row[2 * i + 1]) * 0.5f;
            float b = (row[2 * i] - row[2 * i + 1]) * 0.5f;

            transformedRow[i] = a;
            transformedRow[i + len] = b;
        }

        return transformedRow;
    }

    private static float[] transformCol(float[] col, int channel) {
        float[] transformedCol = new float[col.length];
        int len = col.length / 2;

        for (int i = 0; i < len; i++) {
            float a = (col[2 * i] + col[2 * i + 1]) * 0.5f;
            float b = (col[2 * i] - col[2 * i + 1]) * 0.5f;

            transformedCol[i] = a;
            transformedCol[i + len] = b;
        }

        return transformedCol;
    }

    public static float[] inverseTransformRow(float[] row, int channel) {
        float[] originalRow = new float[row.length];
        int len = row.length / 2;

        for (int i = 0; i < len; i++) {
            float a = row[i];
            float b = row[i + len];

            originalRow[2 * i] = a + b;
            originalRow[2 * i + 1] = a - b;
        }

        return originalRow;
    }

    public static float[] inverseTransformCol(float[] col, int channel) {
        float[] originalCol = new float[col.length];
        int len = col.length / 2;

        for (int i = 0; i < len; i++) {
            float a = col[i];
            float b = col[i + len];

            originalCol[2 * i] = a + b;
            originalCol[2 * i + 1] = a - b;
        }

        return originalCol;
    }

    private static float[] getColumn(float[][] matrix, int columnIndex) {
        int numRows = matrix.length;
        float[] column = new float[numRows];

        for (int i = 0; i < numRows; i++) {
            column[i] = matrix[i][columnIndex];
        }

        return column;
    }

}
