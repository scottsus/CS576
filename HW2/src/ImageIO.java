import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import java.util.List;

public class ImageIO {
    private JFrame frame;
    private JLabel label;

    public ImageIO() {
        // Empty constructor
    }

    public void showImages(BufferedImage image) {
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

    public void drawBoundingBoxes(BufferedImage image, List<Rectangle> boundingBoxes) {
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        for (Rectangle box : boundingBoxes) {
            g.drawRect(box.x, box.y, box.width, box.height);
        }
        g.dispose();
    }

    public void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
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

}
