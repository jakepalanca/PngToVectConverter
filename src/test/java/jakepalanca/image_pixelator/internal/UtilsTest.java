package jakepalanca.image_pixelator.internal;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Utils} class.
 * <p>
 * These tests cover utility methods for color binning, hue distance calculation,
 * component and sprite visualization, sprite pixel replacement, and file writing.
 */
public class UtilsTest {

    /**
     * Default constructor for UtilsTest.
     */
    public UtilsTest() {
        // Default Constructor
    }

    /**
     * Tests that color binning reduces color detail correctly.
     */
    @Test
    public void testBinColorARGB() {
        // A color (fully opaque, R=123, G=234, B=56)
        int original = (0xFF << 24) | (123 << 16) | (234 << 8) | 56;
        int binned = Utils.binColorARGB(original, 24);
        // Since 123/24 = 5 (bin start 120), 234/24 = 9 (bin start 216), 56/24 = 2 (bin start 48)
        int expected = (0xFF << 24) | (120 << 16) | (216 << 8) | 48;
        assertEquals(expected, binned, "Binned color should equal the expected value.");
    }

    /**
     * Tests that hue distance is computed accurately.
     *
     * @throws InvalidPropertiesFormatException thrown when HSB or distance properties are invalid.
     */
    @Test
    public void testHueDistance() throws InvalidPropertiesFormatException {
        // Two HSB arrays that are close (e.g., 0.1 and 0.15)
        float[] hsb1 = {0.1f, 0.8f, 0.8f};
        float[] hsb2 = {0.15f, 0.8f, 0.8f};
        float distance = Utils.hueDistance(hsb1, hsb2);
        assertEquals(0.05f, distance, 0.001, "Hue distance should be approximately 0.05");
    }

    /**
     * Tests that component visualization produces an image matching the label dimensions.
     */
    @Test
    public void testVisualizeComponents() {
        BufferedImage img = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        int[][] labels = new int[3][3];
        // Set a simple pattern: label 1 at (1,1), rest 0.
        labels[1][1] = 1;
        BufferedImage out = Utils.visualizeComponents(img, labels);
        assertNotNull(out, "Output visualization image should not be null.");
        // Verify dimensions match the label array dimensions.
        assertEquals(3, out.getWidth(), "Output width should be 3.");
        assertEquals(3, out.getHeight(), "Output height should be 3.");
    }

    /**
     * Tests that sprite visualization highlights expected sprite pixels.
     */
    @Test
    public void testVisualizeSprites() {
        BufferedImage base = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        Sprite sprite1 = new Sprite(1);
        sprite1.addPixel(1, 1, 0xFFFF0000);
        Sprite sprite2 = new Sprite(2);
        sprite2.addPixel(3, 3, 0xFF00FF00);
        List<Sprite> sprites = List.of(sprite1, sprite2);

        BufferedImage out = Utils.visualizeSprites(base, sprites);
        assertNotNull(out, "Sprite visualization image should not be null.");
        // Verify that pixels where sprites are expected are non-transparent.
        int color1 = out.getRGB(1, 1);
        int color2 = out.getRGB(3, 3);
        assertNotEquals(0x00000000, color1, "Expected non-transparent pixel at (1,1).");
        assertNotEquals(0x00000000, color2, "Expected non-transparent pixel at (3,3).");
    }

    /**
     * Tests that replacing sprite pixel data works correctly.
     */
    @Test
    public void testReplaceSpritePixels() {
        // Create a sprite with one initial pixel.
        Sprite sprite = new Sprite(1);
        sprite.addPixel(2, 2, 0xFFFF0000);
        // Create a new 3x3 image with opaque green pixels.
        BufferedImage newImg = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                newImg.setRGB(x, y, 0xFF00FF00);
            }
        }
        // Replace the sprite pixels with the new image data.
        Utils.replaceSpritePixels(sprite, newImg);
        assertEquals(9, sprite.getPixelCount(), "After replacement, sprite should have 9 pixels.");
    }

    /**
     * Tests that a string can be written to and read from a file correctly.
     *
     * @throws Exception if any I/O operation fails.
     */
    @Test
    public void testWriteStringToFile() throws Exception {
        String content = "Hello, JUnit!";
        File tempFile = File.createTempFile("testWrite", ".txt");
        tempFile.deleteOnExit();
        Utils.writeStringToFile(content, tempFile);
        String readBack = Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(content, readBack, "Content written and read back should match.");
    }
}
