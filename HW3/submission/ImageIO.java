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

    public static byte[][][] initByteArray(String imgPath) {
        BufferedImage bufferedImage = getBufferedImage(imgPath);
        byte[][][] image = new byte[HEIGHT][WIDTH][3];

        try {
            for (int h = 0; h < HEIGHT; h++) {
                for (int w = 0; w < WIDTH; w++) {
                    Color color = new Color(bufferedImage.getRGB(w, h));
                    image[h][w][0] = (byte) color.getRed();
                    image[h][w][1] = (byte) color.getGreen();
                    image[h][w][2] = (byte) color.getBlue();
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading image: " + e.getMessage());
            return null;
        }

        return image;
    }

    private static BufferedImage getBufferedImage(String imgPath) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        try {
            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = WIDTH * HEIGHT * 3;
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
                    image.setRGB(x, y, pix);
                    ind++;
                }
            }

            raf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }

    public static BufferedImage byteToBufferedImage(byte[][][] byteImage) {
        BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int h = 0; h < HEIGHT; h++) {
            for (int w = 0; w < WIDTH; w++) {
                // Extract the RGB values.
                int red = byteImage[h][w][0] & 0xFF; // Convert byte to unsigned int
                int green = byteImage[h][w][1] & 0xFF;
                int blue = byteImage[h][w][2] & 0xFF;

                // Create an RGB integer with the format 0xRRGGBB
                int rgb = new Color(red, green, blue).getRGB();

                bufferedImage.setRGB(w, h, rgb);
            }
        }

        return bufferedImage;
    }

    public static void showImage(byte[][][] byteImage) {
        BufferedImage image = byteToBufferedImage(byteImage);
        showImage(image);
    }

    public static void showImage(BufferedImage image) {
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
}
