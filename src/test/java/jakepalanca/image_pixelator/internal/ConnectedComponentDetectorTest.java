package jakepalanca.image_pixelator.internal;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the {@link ConnectedComponentDetector} class.
 * <p>
 * This test creates an image with a clear single connected component (a white square on a transparent background)
 * and verifies that only one component is detected.
 */
public class ConnectedComponentDetectorTest {

    /**
     * Default constructor for ConnectedComponentDetectorTest.
     */
    public ConnectedComponentDetectorTest() {
        // Default Constructor
    }

    /**
     * Tests that a single connected component is correctly detected.
     */
    @Test
    public void testDetectSingleComponent() {
        // Create a 5x5 image with a white block from (1,1) to (3,3).
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        // Set all pixels to transparent.
        for (int y = 0; y < 5; y++)
            for (int x = 0; x < 5; x++)
                image.setRGB(x, y, 0x00000000);
        // Set block pixels to opaque white.
        for (int y = 1; y <= 3; y++)
            for (int x = 1; x <= 3; x++)
                image.setRGB(x, y, 0xFFFFFFFF);

        int[][] labels = ConnectedComponentDetector.detect(image);
        assertNotNull(labels, "Labels array should not be null.");

        // Calculate maximum label in the array.
        int maxLabel = 0;
        for (int[] label : labels) {
            for (int x = 0; x < labels[0].length; x++) {
                if (label[x] > maxLabel) {
                    maxLabel = label[x];
                }
            }
        }
        // We expect exactly one component.
        assertEquals(1, maxLabel, "Should detect one connected component.");
    }
}
