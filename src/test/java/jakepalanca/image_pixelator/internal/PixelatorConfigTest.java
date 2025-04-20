package jakepalanca.image_pixelator.internal;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link PixelatorConfig} class.
 * <p>
 * These tests verify that the configuration parameters can be set and retrieved correctly.
 * Since the full constructor is private, only the default constructor with setters is tested.
 */
public class PixelatorConfigTest {

    /**
     * Default constructor for PixelatorConfigTest.
     */
    public PixelatorConfigTest() {
        // Default Constructor
    }

    /**
     * Tests setting and getting configuration values.
     */
    @Test
    public void testDefaultAndSetters() {
        PixelatorConfig config = new PixelatorConfig();
        config.setUserId("testUser");
        config.setErosionNeighborsThreshold(3);
        config.setDilationNeighborsThreshold(3);
        config.setMinNoiseSize(15);
        config.setColorBinningSize(20);
        config.setTopNSprites(5);
        config.setTargetWidth(64);
        config.setTargetHeight(64);
        config.setOutputFormats(Set.of("png", "svg"));
        config.setBucketName("dummyBucket");
        config.setLogLevel("DEBUG");

        assertEquals("testUser", config.getUserId());
        assertEquals(3, config.getErosionNeighborsThreshold());
        assertEquals(3, config.getDilationNeighborsThreshold());
        assertEquals(15, config.getMinNoiseSize());
        assertEquals(20, config.getColorBinningSize());
        assertEquals(5, config.getTopNSprites());
        assertEquals(64, config.getTargetWidth());
        assertEquals(64, config.getTargetHeight());
        assertTrue(config.getOutputFormats().contains("png"));
        assertTrue(config.getOutputFormats().contains("svg"));
        assertEquals("dummyBucket", config.getBucketName());
        assertEquals("DEBUG", config.getLogLevel());
    }
}
