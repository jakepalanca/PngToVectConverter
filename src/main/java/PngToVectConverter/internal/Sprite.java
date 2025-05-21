package PngToVectConverter.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code Sprite} class represents a collection of pixels that together define a sprite.
 * <p>
 * A sprite is identified by a unique label ID and is composed of a list of {@link PixelData} elements.
 * Each pixel stores its original coordinates (x, y) and its ARGB (Alpha, Red, Green, Blue) color value.
 * </p>
 * <p>
 * This class provides methods to:
 * <ul>
 *   <li>Add individual pixels to the sprite</li>
 *   <li>Calculate the center of mass (average coordinate) of all pixels</li>
 *   <li>Generate a {@link BufferedImage} cropped to the minimal bounding box containing all pixels</li>
 *   <li>Retrieve various attributes such as label ID, pixel count, and center coordinates</li>
 *   <li>Reset the internal pixel count</li>
 * </ul>
 * </p>
 */
class Sprite {

    private static final Logger logger = LoggerFactory.getLogger(Sprite.class);

    private final int labelId;
    private final List<PixelData> pixels;

    private double centerX;
    private double centerY;
    private int pixelCount;

    /**
     * Constructs a new {@code Sprite} with the specified label ID.
     *
     * @param labelId the unique identifier for the sprite
     */
    Sprite(int labelId) {
        this.labelId = labelId;
        this.pixels = new ArrayList<>();
        this.pixelCount = 0;
        this.centerX = 0;
        this.centerY = 0;
        logger.trace("Sprite object created with label ID: {}", labelId);
    }

    /**
     * Adds a pixel to this sprite.
     *
     * @param x    the x-coordinate of the pixel
     * @param y    the y-coordinate of the pixel
     * @param argb the ARGB color value of the pixel
     */
    void addPixel(int x, int y, int argb) {
        pixels.add(new PixelData(x, y, argb));
        pixelCount++;
        logger.trace("Added pixel ({}, {}) ARGB: {} to sprite {}", x, y, Integer.toHexString(argb), labelId);
    }

    /**
     * Calculates the center of mass (average x and y position) of the sprite's pixels.
     */
    void calculateCenterOfMass() {
        if (pixelCount == 0) {
            this.centerX = 0;
            this.centerY = 0;
            logger.trace("Calculated COM for sprite {}: (0, 0) - No pixels", labelId);
            return;
        }

        double sumX = 0;
        double sumY = 0;
        for (PixelData pd : pixels) {
            sumX += pd.x();
            sumY += pd.y();
        }

        this.centerX = sumX / pixelCount;
        this.centerY = sumY / pixelCount;
        logger.trace("Calculated COM for sprite {}: ({}, {}) based on {} pixels", labelId, this.centerX, this.centerY, pixelCount);
    }

    /**
     * Generates a {@link BufferedImage} representing the sprite,
     * cropped to its minimal bounding box.
     *
     * @return the cropped image, or {@code null} if the sprite has no pixels
     */
    BufferedImage getBufferedImage() {
        if (pixelCount == 0) {
            logger.warn("getBufferedImage called on sprite {} with no pixels. Returning null.", labelId);
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (PixelData p : pixels) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }

        if (minX > maxX) {
            logger.error("Sprite {} has {} pixels but invalid bounds. Returning 1x1 transparent image.", labelId, pixelCount);
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (PixelData p : pixels) {
            int relativeX = p.x() - minX;
            int relativeY = p.y() - minY;

            if (relativeX >= 0 && relativeX < width && relativeY >= 0 && relativeY < height) {
                img.setRGB(relativeX, relativeY, p.argb());
            } else {
                logger.warn("Pixel ({}, {}) translated to ({}, {}) is outside bounds. Skipping.",
                        p.x(), p.y(), relativeX, relativeY);
            }
        }

        logger.trace("Generated BufferedImage for sprite {} with dimensions {}x{}", labelId, width, height);
        return img;
    }

    /**
     * Gets the label ID of this sprite.
     *
     * @return the label ID
     */
    int getLabelId() {
        return labelId;
    }

    /**
     * Gets the list of pixels in this sprite.
     *
     * @return the list of {@link PixelData}
     */
    List<PixelData> getPixels() {
        return pixels;
    }

    /**
     * Gets the total number of pixels in this sprite.
     *
     * @return the pixel count
     */
    int getPixelCount() {
        return pixelCount;
    }

    /**
     * Gets the calculated center X-coordinate of this sprite.
     *
     * @return center of mass X-coordinate
     */
    double getCenterX() {
        return centerX;
    }

    /**
     * Gets the calculated center Y-coordinate of this sprite.
     *
     * @return center of mass Y-coordinate
     */
    double getCenterY() {
        return centerY;
    }

    /**
     * Resets the pixel count to zero.
     */
    void resetPixelCount() {
        this.pixelCount = 0;
        logger.trace("Reset pixel count for sprite {}", labelId);
    }

    /**
     * Returns a string representation of the sprite.
     *
     * @return a debug-friendly string describing the sprite
     */
    @Override
    public String toString() {
        return "Sprite{" +
                "labelId=" + labelId +
                ", pixelCount=" + pixelCount +
                ", centerX=" + String.format("%.2f", centerX) +
                ", centerY=" + String.format("%.2f", centerY) +
                '}';
    }

    /**
     * Represents the data for a single pixel within the sprite.
     *
     * @param x    the x-coordinate of the pixel
     * @param y    the y-coordinate of the pixel
     * @param argb the ARGB color value of the pixel
     */
    public record PixelData(int x, int y, int argb) {
    }
}
