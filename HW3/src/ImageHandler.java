import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageHandler {
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;

    public static byte[][][] readImageRGB(String imagePath) {
        byte[][][] image = new byte[WIDTH][HEIGHT][3];

        try {
            byte[] data = Files.readAllBytes(Paths.get(imagePath));

            int index = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    image[x][y][0] = data[index++];
                    image[x][y][1] = data[index++];
                    image[x][y][2] = data[index++];
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading image: " + e.getMessage());
            return null;
        }

        return image;
    }
}
