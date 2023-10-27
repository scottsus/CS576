import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;

public class ImageIO {
    private static JFrame frame;
    private static JLabel label;
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;

    public ImageIO() {
        // Empty constructor
    }

    public static BufferedImage floatToBufferedImage(float[][][] floatImage) {
        byte[][][] byteImage = floatToByte(floatImage);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);

        byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        int index = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int c = 0; c < 3; c++) {
                    targetPixels[index++] = byteImage[x][y][c];
                }
            }
        }

        return image;
    }

    private static byte[][][] floatToByte(float[][][] floatImage) {
        byte[][][] image = new byte[WIDTH][HEIGHT][3];

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int channel = 0; channel < 3; channel++) {
                    image[x][y][channel] = (byte) Math.min(255, Math.max(0, Math.round(floatImage[x][y][channel])));
                }
            }
        }
        return image;
    }

    public static void showImages(float[][][] floatImage) {
        BufferedImage image = floatToBufferedImage(floatImage);

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

    public void readImageRGB(int WIDTH, int HEIGHT, String imgPath, BufferedImage img) {
        try {
            int frameLength = WIDTH * HEIGHT * 3;

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    byte r = bytes[ind];
                    byte g = bytes[ind + HEIGHT * WIDTH];
                    byte b = bytes[ind + HEIGHT * WIDTH * 2];

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

}
