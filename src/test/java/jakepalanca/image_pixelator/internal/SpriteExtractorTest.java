package jakepalanca.image_pixelator.internal;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link SpriteExtractor} class.
 * <p>
 * These tests verify that the extraction process correctly groups pixels into sprites,
 * filters out small noise components (fewer than the minimum pixel count), and preserves proper label IDs.
 */
public class SpriteExtractorTest {

    /**
     * Default constructor for SpriteExtractorTest.
     */
    public SpriteExtractorTest() {
        // Default Constructor
    }

    /**
     * Tests that the extraction process removes components below the noise threshold.
     */
    @Test
    public void testExtractSpritesRemovesNoise() {
        // Create a 5x5 image where one small component exists.
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        int[][] labels = new int[5][5];

        // Simulate a small component with label 1 (size = 3 pixels)
        labels[1][1] = 1;
        labels[1][2] = 1;
        labels[2][1] = 1;
        image.setRGB(1, 1, 0xFFFF0000);
        image.setRGB(1, 2, 0xFFFF0000);
        image.setRGB(2, 1, 0xFFFF0000);

        // Also add a larger component with label 2 (simulate at least 10 pixels)
        // For simplicity, create a 3x3 block plus extra pixels
        for (int y = 3; y < 5; y++) {
            for (int x = 3; x < 5; x++) {
                labels[y][x] = 2;
                image.setRGB(x, y, 0xFF00FF00);
            }
        }
        labels[2][3] = 2;
        image.setRGB(3, 2, 0xFF00FF00);
        labels[2][4] = 2;
        image.setRGB(4, 2, 0xFF00FF00);
        labels[3][2] = 2;
        image.setRGB(2, 3, 0xFF00FF00);
        // Add a few more pixels to label 2 to reach a count >= 10
        labels[0][3] = 2;
        image.setRGB(3, 0, 0xFF00FF00);
        labels[0][4] = 2;
        image.setRGB(4, 0, 0xFF00FF00);
        labels[1][3] = 2;
        image.setRGB(3, 1, 0xFF00FF00);

        List<Sprite> sprites = SpriteExtractor.extractSprites(image, labels, 2, new PixelatorConfig());
        // Only label 2 should remain (noise < 10 removed)
        assertNotNull(sprites, "Extracted sprites list should not be null.");
        assertEquals(1, sprites.size(), "Expected only one sprite after noise removal.");
        Sprite sprite = sprites.get(0);
        assertTrue(sprite.getPixelCount() >= 10, "Remaining sprite pixel count should be at least 10.");
        assertEquals(2, sprite.getLabelId(), "The remaining sprite should have label ID 2.");
    }
}
