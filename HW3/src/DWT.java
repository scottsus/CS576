public class DWT {
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private static final float[] LOW_PASS = { 0.5f, 0.5f };
    private static final float[] HIGH_PASS = { 0.5f, -0.5f };

    public static float[][] recursiveDWT(float[][] image, int channel, int level, int maxLevel) {
        if (level == maxLevel) {
            return image;
        }

        float[][] transformedImage = new float[WIDTH][HEIGHT];

        for (int y = 0; y < HEIGHT; y++) {
            transformedImage[y] = transformRow(image[y], channel);
        }

        for (int x = 0; x < WIDTH; x++) {
            transformedImage[x] = transformCol(getColumn(transformedImage, x), channel);
        }

        float[][] quarter = new float[WIDTH / 2][HEIGHT / 2];
        for (int y = 0; y < HEIGHT / 2; y++) {
            for (int x = 0; x < WIDTH / 2; x++) {
                quarter[x][y] = transformedImage[x][y];
            }
        }
        quarter = recursiveDWT(transformedImage, channel, level + 1, maxLevel);

        for (int y = 0; y < HEIGHT / 2; y++) {
            for (int x = 0; x < WIDTH / 2; x++) {
                transformedImage[x][y] = quarter[x][y];
            }
        }

        return transformedImage;
    }

    private static float[][] recursiveInverseDWT(float[][] transformedImage, int channel, int level, int maxLevel) {
        if (level == maxLevel) {
            return transformedImage;
        }

        float[][] quarter = new float[WIDTH / 2][HEIGHT / 2];
        for (int y = 0; y < HEIGHT / 2; y++) {
            for (int x = 0; x < WIDTH / 2; x++) {
                quarter[x][y] = transformedImage[x][y];
            }
        }
        quarter = recursiveInverseDWT(transformedImage, channel, level + 1, maxLevel);

        for (int y = 0; y < HEIGHT / 2; y++) {
            for (int x = 0; x < WIDTH / 2; x++) {
                transformedImage[x][y] = quarter[x][y];
            }
        }

        for (int x = 0; x < WIDTH; x++) {
            float[] col = new float[HEIGHT];
            for (int y = 0; y < HEIGHT; y++) {
                col[y] = transformedImage[x][y];
            }

            col = inverseTransformCol(col, channel);
            for (int y = 0; y < HEIGHT; y++) {
                transformedImage[x][y] = col[y];
            }
        }

        for (int y = 0; y < HEIGHT; y++) {
            float[] row = new float[WIDTH];
            for (int x = 0; x < WIDTH; x++) {
                row[x] = transformedImage[x][y];
            }

            row = inverseTransformRow(row, channel);
            for (int x = 0; x < WIDTH; x++) {
                transformedImage[x][y] = row[x];
            }
        }

        return transformedImage;
    }

    public static void progressiveDecoding(float[][][] transformedImage, int maxLevel) {
        for (int level = maxLevel; level >= 0; level--) {
            float[][][] reconstructedImage = new float[WIDTH][HEIGHT][3];
            for (int c = 0; c < 3; c++) {
                float[][] blah = new float[WIDTH][HEIGHT];
                for (int y = 0; y < HEIGHT; y++) {
                    for (int x = 0; x < WIDTH; x++) {
                        blah[x][y] = transformedImage[x][y][c];
                    }
                }
                float[][] zeroed = zeroHighPassCoefficients(blah, level);
                float[][] blaah = recursiveInverseDWT(zeroed, c, level, maxLevel);
                for (int y = 0; y < HEIGHT; y++) {
                    for (int x = 0; x < WIDTH; x++) {
                        reconstructedImage[x][y][c] = blaah[x][y];
                    }
                }
            }

            System.out.println("Level: " + level);
            ImageIO.showImages(reconstructedImage);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static float[][] zeroHighPassCoefficients(float[][] image, int level) {
        int startIndex = (int) Math.pow(2, level);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = startIndex; x < WIDTH; x++) {
                image[x][y] = 0;
            }
        }

        for (int y = startIndex; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                image[x][y] = 0;
            }
        }

        return image;
    }

    public static float[][][] forwardDWT(byte[][][] image, int level) {
        float[][][] transformedImage = new float[image.length][image[0].length][3];

        for (int y = 0; y < image[0].length; y++) {
            for (int x = 0; x < image.length; x++) {
                for (int c = 0; c < 3; c++) {
                    transformedImage[x][y][c] = image[x][y][c];
                }
            }
        }

        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < transformedImage[0].length; y++) {
                float[] row = new float[transformedImage.length];
                for (int x = 0; x < transformedImage.length; x++) {
                    row[x] = transformedImage[x][y][c];
                }

                row = transformRow(row, c);
                for (int x = 0; x < transformedImage.length; x++) {
                    transformedImage[x][y][c] = row[x];
                }
            }
        }

        for (int c = 0; c < 3; c++) {
            for (int x = 0; x < transformedImage.length; x++) {
                float[] col = new float[transformedImage[0].length];
                for (int y = 0; y < transformedImage[0].length; y++) {
                    col[y] = transformedImage[x][y][c];
                }

                col = transformCol(col, c);
                for (int y = 0; y < transformedImage[0].length; y++) {
                    transformedImage[x][y][c] = col[y];
                }
            }
        }

        return transformedImage;
    }

    public static float[][][] inverseDWT(float[][][] transformedImage, int level) {
        float[][][] reconstructedImage = new float[transformedImage.length][transformedImage[0].length][3];

        for (int y = 0; y < transformedImage[0].length; y++) {
            for (int x = 0; x < transformedImage.length; x++) {
                for (int c = 0; c < 3; c++) {
                    reconstructedImage[x][y][c] = transformedImage[x][y][c];
                }
            }
        }

        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < reconstructedImage[0].length; y++) {
                float[] row = new float[reconstructedImage.length];
                for (int x = 0; x < reconstructedImage.length; x++) {
                    row[x] = reconstructedImage[x][y][c];
                }

                row = inverseTransformRow(row, c);
                for (int x = 0; x < reconstructedImage.length; x++) {
                    reconstructedImage[x][y][c] = row[x];
                }
            }
        }

        for (int c = 0; c < 3; c++) {
            for (int x = 0; x < reconstructedImage.length; x++) {
                float[] col = new float[reconstructedImage[0].length];
                for (int y = 0; y < reconstructedImage[0].length; y++) {
                    col[y] = reconstructedImage[x][y][c];
                }

                col = inverseTransformCol(col, c);
                for (int y = 0; y < reconstructedImage[0].length; y++) {
                    reconstructedImage[x][y][c] = col[y];
                }
            }
        }

        return reconstructedImage;
    }

    private static float[] transformRow(float[] row, int channel) {
        if (row.length == 3) {
            System.out.println("3!");
        }
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
            // System.out.println("i: " + i);
            // System.out.println("col: " + column.length);
            // System.out.println("mat: " + matrix.length);
            // System.out.println("row: " + matrix[i].length);
            column[i] = matrix[i][columnIndex];
        }

        return column;
    }

}
