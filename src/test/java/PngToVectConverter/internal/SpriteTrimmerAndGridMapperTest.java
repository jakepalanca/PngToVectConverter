package PngToVectConverter.internal;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link SpriteTrimmerAndGridMapper} class.
 * <p>
 * Instead of merely checking that the opaque pixel count is above a threshold,
 * this test simulates a realistic scenario: a sprite is constructed as a filled block
 * extracted from a 1024×1024 input image. Then, after processing (trimming, scaling, centering),
 * the output image is compared pixel‐by‐pixel with a fully red reference image.
 */
public class SpriteTrimmerAndGridMapperTest {

    /**
     * Default constructor for SpriteTrimmerAndGridMapperTest.
     */
    public SpriteTrimmerAndGridMapperTest() {
        // Default Constructor
    }

    /**
     * Tests that a sprite representing a filled red block extracted from a 1024×1024 image,
     * when trimmed and scaled to 64×64, produces the expected fully red output image.
     * <p>
     * In this test:
     * <ul>
     *   <li>A sprite is created by adding red pixels in a 100×100 block within a 1024×1024 coordinate system (from (400,400) to (499,499)).</li>
     *   <li>The sprite's bounding box is expected to be 100×100.</li>
     *   <li>After processing via {@code trimAndScaleSprite} to a target of 64×64, the output should be a 64×64 red image.</li>
     *   <li>The test then generates an expected 64×64 red image and compares the two images pixel by pixel.</li>
     *   <li>Additional assertions verify that the computed center-of-mass is near the geometric center.</li>
     * </ul>
     */
    @Test
    public void testTrimAndScaleSprite_FilledRedBlockMatchesExpected() {
        // Create a sprite mimicking a filled red block from a 1024×1024 image.
        // For example, populate a 100×100 block in the region (400,400) to (499,499).
        Sprite sprite = new Sprite(1);
        for (int x = 400; x < 500; x++) {
            for (int y = 400; y < 500; y++) {
                sprite.addPixel(x, y, 0xFFFF0000); // bright red (opaque)
            }
        }

        // Ensure the computed bounding box is as expected (i.e. 100x100)
        BufferedImage cropped = sprite.getBufferedImage();
        assertNotNull(cropped, "Cropped image should not be null.");
        assertEquals(100, cropped.getWidth(), "Cropped image width should be 100.");
        assertEquals(100, cropped.getHeight(), "Cropped image height should be 100.");

        // Define target dimensions for the final sprite.
        int targetWidth = 64;
        int targetHeight = 64;

        PixelatorConfig pixelatorConfig = new PixelatorConfig();
        pixelatorConfig.setTargetWidth(targetWidth);
        pixelatorConfig.setTargetHeight(targetHeight);

        // Process the sprite: trim, scale, center, remove edge fuzz, and apply alpha threshold.
        // Disable glow removal for this test.
        SpriteTrimmerAndGridMapper.trimAndScaleSprite(sprite, pixelatorConfig);

        // Retrieve the output image from the processed sprite.
        BufferedImage result = sprite.getBufferedImage();
        assertNotNull(result, "Result image should not be null.");
        assertEquals(targetWidth, result.getWidth(), "Result image width should be 64.");
        assertEquals(targetHeight, result.getHeight(), "Result image height should be 64.");

        // Generate the expected image: a 64×64 image where every pixel is fully opaque red (0xFFFF0000).
        BufferedImage expected = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                expected.setRGB(x, y, 0xFFFF0000);
            }
        }

        // Compare the expected and result images pixel by pixel.
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int expectedRGB = expected.getRGB(x, y);
                int resultRGB = result.getRGB(x, y);
                assertEquals(expectedRGB, resultRGB,
                        String.format("At pixel (%d,%d): expected %08X but found %08X.", x, y, expectedRGB, resultRGB));
            }
        }

        // As an additional check, verify that the sprite's computed center-of-mass lies near the geometric center.
        double centerX = sprite.getCenterX();
        double centerY = sprite.getCenterY();
        double expectedCenter = targetWidth / 2.0;
        double tolerance = targetWidth * 0.15; // allow 15% tolerance
        assertTrue(Math.abs(centerX - expectedCenter) < tolerance,
                "Center of mass X (" + centerX + ") is not near the expected center (" + expectedCenter + ").");
        assertTrue(Math.abs(centerY - expectedCenter) < tolerance,
                "Center of mass Y (" + centerY + ") is not near the expected center (" + expectedCenter + ").");
    }
}
