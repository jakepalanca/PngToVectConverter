package jakepalanca.image_pixelator.internal;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the {@link ImagePreprocessor} class.
 * <p>
 * These tests verify that background removal and noise pruning are applied correctly.
 * For example, the test checks that the background is removed (transparent pixels in the borders)
 * while the target object (a black square) remains intact.
 */
public class ImagePreprocessorTest {

    /**
     * Default constructor for ImagePreprocessorTest.
     */
    public ImagePreprocessorTest() {
        // Default Constructor
    }

    /**
     * Tests that the preprocess method removes background pixels and preserves a centered object.
     */
    @Test
    public void testPreprocessRemovesBackgroundAndBinning() {
        // Create an 11x11 image with a border color simulating the background.
        BufferedImage original = new BufferedImage(11, 11, BufferedImage.TYPE_INT_ARGB);
        // Set all pixels to light gray (simulate background).
        for (int y = 0; y < 11; y++) {
            for (int x = 0; x < 11; x++) {
                original.setRGB(x, y, 0xFFCCCCCC);
            }
        }
        // Draw a black square from (3,3) to (7,7) for the object.
        for (int y = 3; y <= 7; y++) {
            for (int x = 3; x <= 7; x++) {
                original.setRGB(x, y, 0xFF000000);
            }
        }

        PixelatorConfig config = new PixelatorConfig();
        config.setErosionNeighborsThreshold(2);
        config.setDilationNeighborsThreshold(2);
        config.setMinNoiseSize(3);
        config.setColorBinningSize(24);

        BufferedImage preprocessed = ImagePreprocessor.preprocess(original, config);
        assertNotNull(preprocessed, "Preprocessed image should not be null.");

        // Check that a corner pixel becomes transparent (background removed).
        int cornerARGB = preprocessed.getRGB(0, 0);
        assertEquals(0, cornerARGB, "Corner pixel should be transparent after preprocessing.");

        // Check that the center pixel (e.g. at (5,5)) remains black.
        int centerARGB = preprocessed.getRGB(5, 5);
        assertEquals(0xFF000000, centerARGB, "Center pixel should remain opaque black after preprocessing.");
    }
}
