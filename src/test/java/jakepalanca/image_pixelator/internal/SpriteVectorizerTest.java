package jakepalanca.image_pixelator.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link SpriteVectorizer} class.
 * <p>
 * These tests verify that a sprite with defined pixels is correctly converted into an SVG string
 * containing the expected SVG elements and color formats.
 */
public class SpriteVectorizerTest {

    /**
     * Default constructor for SpriteVectorizerTest.
     */
    public SpriteVectorizerTest() {
        // Default Constructor
    }

    /**
     * Tests that vectorizing a sprite produces a valid SVG string.
     */
    @Test
    public void testVectorizeSpriteGeneratesSVG() {
        // Create a simple sprite with two pixels.
        Sprite sprite = new Sprite(1);
        sprite.addPixel(0, 0, 0xFFFF0000);  // red pixel
        sprite.addPixel(1, 0, 0xFF00FF00);  // green pixel
        // Compute center-of-mass (optional for SVG generation).
        sprite.calculateCenterOfMass();

        // Generate SVG string.
        String svg = SpriteVectorizer.vectorizeSprite(sprite);
        assertNotNull(svg, "SVG string should not be null.");
        // Check that the SVG contains the <svg> tag.
        assertTrue(svg.contains("<svg"), "SVG string should contain the <svg> tag.");
        // Check that rect elements exist.
        assertTrue(svg.contains("<rect"), "SVG string should contain <rect> tags.");
        // Verify that the fill attribute uses a proper hexadecimal color.
        assertTrue(svg.matches("(?s).*#([0-9A-F]{6}).*"), "SVG fill colors should be in hex format.");
    }
}
