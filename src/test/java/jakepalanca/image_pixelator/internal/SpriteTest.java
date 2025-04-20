package jakepalanca.image_pixelator.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the {@link Sprite} class.
 * <p>
 * These tests verify that pixels are added correctly, the center of mass is calculated as expected,
 * and the generated {@link BufferedImage} is cropped to the correct bounding box.
 */
public class SpriteTest {

    private Sprite sprite;

    /**
     * Default constructor for SpriteTest.
     */
    public SpriteTest() {
        // Default Constructor
    }

    /**
     * Sets up a new Sprite instance with a sample label before each test.
     */
    @BeforeEach
    public void setup() {
        sprite = new Sprite(1);
    }

    /**
     * Tests that adding pixels increases the pixel count and list size.
     */
    @Test
    public void testAddPixelAndCount() {
        sprite.addPixel(10, 10, 0xFFFF0000); // red pixel
        sprite.addPixel(20, 20, 0xFF00FF00); // green pixel
        assertEquals(2, sprite.getPixelCount(), "Pixel count should be 2.");
        assertEquals(2, sprite.getPixels().size(), "Pixels list size should be 2.");
    }

    /**
     * Tests that the center of mass is correctly computed.
     */
    @Test
    public void testCalculateCenterOfMass() {
        sprite.addPixel(0, 0, 0xFFFFFFFF);
        sprite.addPixel(2, 2, 0xFFFFFFFF);
        sprite.calculateCenterOfMass();
        // Expected center: (1,1)
        assertEquals(1.0, sprite.getCenterX(), 0.01, "Center X should be 1.0");
        assertEquals(1.0, sprite.getCenterY(), 0.01, "Center Y should be 1.0");
    }

    /**
     * Tests that the generated BufferedImage is cropped to a minimal bounding box.
     */
    @Test
    public void testGetBufferedImage() {
        // Add two pixels at (5,5) and (7,8)
        sprite.addPixel(5, 5, 0xFFFF0000);
        sprite.addPixel(7, 8, 0xFFFF0000);
        BufferedImage img = sprite.getBufferedImage();
        // Expected bounding box: minX=5, maxX=7 (width = 3) and minY=5, maxY=8 (height = 4)
        assertNotNull(img, "BufferedImage should not be null.");
        assertEquals(3, img.getWidth(), "BufferedImage width should be 3.");
        assertEquals(4, img.getHeight(), "BufferedImage height should be 4.");

        // Check that the top-left pixel corresponds to the added pixel color
        int color = img.getRGB(0, 0);
        assertEquals(0xFFFF0000, color, "Expected red color in the cropped image.");
    }
}
