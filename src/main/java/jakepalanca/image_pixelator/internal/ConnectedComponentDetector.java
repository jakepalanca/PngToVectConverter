package jakepalanca.image_pixelator.internal;

import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Detects connected components of non-transparent pixels in a BufferedImage
 * using a 4-way flood fill approach. Pixels with alpha below a threshold
 * are treated as background.
 */
public class ConnectedComponentDetector {
    private static final Logger logger = LoggerFactory.getLogger(ConnectedComponentDetector.class);

    /**
     * Directions used for 4-way connectivity: North, South, East, West.
     * Each pair represents {dx, dy}.
     */
    private static final int[][] DIRECTIONS_4 = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    /**
     * Default constructor for ConnectedComponentDetector.
     */
    ConnectedComponentDetector() {
        // Default Constructor
    }

    /**
     * Detects connected components of non-transparent pixels in the input image.
     * Pixels with alpha below 10 are treated as background.
     *
     * @param image the input BufferedImage (ARGB format preferred)
     * @return a 2D array representing the component label for each pixel:
     * - 0 for background
     * - positive integers for unique connected components
     * Returns null if the image is null.
     */
    static int[][] detect(BufferedImage image) {
        if (image == null) {
            logger.error("Input image is null. Cannot detect components.");
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        logger.info("Starting connected component detection on {}x{} image...", width, height);

        int[][] labels = new int[height][width];
        int currentLabel = 0;

        long startTime = System.nanoTime();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha >= 10 && labels[y][x] == 0) {
                    currentLabel++;
                    floodFill(image, x, y, currentLabel, labels);
                }
            }
            if (y > 0 && y % 100 == 0) {
                logger.debug("Component detection progress: Row {}/{}", y, height);
            }
        }

        long endTime = System.nanoTime();
        long durationMillis = (endTime - startTime) / 1_000_000;

        logger.info("Connected component detection complete. Found {} components in {} ms.", currentLabel, durationMillis);
        return labels;
    }

    /**
     * Labels all connected non-transparent pixels using a flood fill algorithm.
     *
     * @param image  the source image
     * @param startX starting X position for flood fill
     * @param startY starting Y position for flood fill
     * @param label  the label to apply to the connected component
     * @param labels the label matrix to be updated
     */
    private static void floodFill(BufferedImage image, int startX, int startY, int label, int[][] labels) {
        int width = image.getWidth();
        int height = image.getHeight();

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        labels[startY][startX] = label;
        int pixelsLabeled = 0;

        while (!queue.isEmpty()) {
            Point currentPoint = queue.poll();
            pixelsLabeled++;
            int x = (int) currentPoint.x;
            int y = (int) currentPoint.y;

            for (int[] direction : DIRECTIONS_4) {
                int nextX = x + direction[0];
                int nextY = y + direction[1];

                if (nextX >= 0 && nextY >= 0 && nextX < width && nextY < height) {
                    int neighborAlpha = (image.getRGB(nextX, nextY) >>> 24) & 0xFF;
                    if (neighborAlpha != 0 && labels[nextY][nextX] == 0) {
                        labels[nextY][nextX] = label;
                        queue.add(new Point(nextX, nextY));
                    }
                }
            }
        }
        logger.trace("Flood fill for label {} finished. Labeled {} pixels.", label, pixelsLabeled);
    }
}
