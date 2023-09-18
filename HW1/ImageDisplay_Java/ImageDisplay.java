
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage trueImage;
	BufferedImage displayedImage;
	int width = 7680;
	int height = 4320;

	/**
	 * Adjust resolution based on scale S.
	 */
	private BufferedImage adjustResolution(BufferedImage originalImage, double scale) {
		int newWidth = (int) (originalImage.getWidth() * scale);
		int newHeight = (int) (originalImage.getHeight() * scale);
		width = newWidth;
		height = newHeight;

		BufferedImage downscaledImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
		Graphics2D gDown = downscaledImage.createGraphics();
		gDown.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
		gDown.dispose();

		return downscaledImage;
	}

	/**
	 * Applies anti-aliasing using a 5x5 kernel.
	 */
	private BufferedImage applyAntiAliasing(BufferedImage originalImage) {
		float[] meanFilter = {
				1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f,
				1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f,
				1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f,
				1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f,
				1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f, 1 / 25f,
		};

		BufferedImageOp op = new ConvolveOp(new Kernel(5, 5, meanFilter),
				ConvolveOp.EDGE_NO_OP, null);
		return op.filter(originalImage, null);
	}

	/**
	 * Helper function to get subImage from original image.
	 */
	private BufferedImage getSubImage(BufferedImage image, int x, int y, int w) {
		int startX = x - w / 2, endX = x + w / 2;
		int startY = y - w / 2, endY = y + w / 2;

		if (startX < 0)
			startX = 0;
		if (startY < 0)
			startY = 0;
		if (startX + w > image.getWidth())
			endX = image.getWidth();
		if (startY + w > image.getHeight())
			endY = image.getHeight();

		return image.getSubimage(startX, startY, endX - startX, endY - startY);
	}

	/**
	 * Adds a listener to mouse
	 * Allows mouse event to create subImage when `CTRL` button is clicked.
	 */
	private void setSubImageListener(JLabel lbIm1, int w) {
		lbIm1.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (e.isControlDown()) {
					BufferedImage subImage = getSubImage(trueImage, e.getX(), e.getY(), w);
					BufferedImage combined = new BufferedImage(width, height,
							BufferedImage.TYPE_INT_RGB);
					Graphics2D g = combined.createGraphics();
					g.drawImage(displayedImage, 0, 0, null);
					g.drawImage(subImage, e.getX() - w / 2, e.getY() - w / 2, null);
					g.dispose();
					lbIm1.setIcon(new ImageIcon(combined));
				} else {
					lbIm1.setIcon(new ImageIcon(displayedImage));
				}
			}
		});
	}

	/**
	 * Read Image RGB
	 * Reads the image of given width and height at the given imgPath into the
	 * provided BufferedImage.
	 */
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Render image based on parameters scale S, alias A, width w.
	 */
	public void showIms(String filePath, double S, boolean A, int w) {
		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		// Read in the specified image
		trueImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, filePath, trueImage);

		displayedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, filePath, displayedImage);

		// Apply anti-aliasing
		if (A) {
			displayedImage = applyAntiAliasing(displayedImage);
		}

		// Resize image to S
		if (S < 1.0) {
			displayedImage = adjustResolution(displayedImage, S);
		}

		// Square window
		lbIm1 = new JLabel(new ImageIcon(displayedImage));
		setSubImageListener(lbIm1, w);

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
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println("Incorrect number of arguments. Expected 4, got " + args.length);
			return;
		}

		try {
			String filePath = args[0];

			double S = Double.valueOf(args[1]);
			System.out.println("S: " + S);

			boolean A = args[2].equals("1") ? true : false;
			System.out.println("A: " + A);

			int w = Integer.valueOf(args[3]);
			System.out.println("w: " + w);

			ImageDisplay ren = new ImageDisplay();
			ren.showIms(filePath, S, A, w);

		} catch (Exception e) {
			System.out.println("Exception: " + e);
			return;
		}
	}

}
