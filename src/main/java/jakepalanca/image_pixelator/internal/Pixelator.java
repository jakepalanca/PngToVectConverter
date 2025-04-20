package jakepalanca.image_pixelator.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Facade for the image pixelation pipeline.
 * <p>
 * Provides a builder-style API for configuring and executing the pixelation process.
 */
public final class Pixelator {
    private final PixelatorConfig config;

    /**
     * Private constructor for internal use with a given configuration.
     *
     * @param cfg The configuration for pixelation.
     */
    private Pixelator(PixelatorConfig cfg) {
        this.config = cfg;
    }

    /**
     * Creates a new {@code Pixelator} instance with default configuration.
     *
     * @return A new {@code Pixelator} instance.
     */
    public static Pixelator builder() {
        return new Pixelator(new PixelatorConfig());
    }

    /**
     * One-liner convenience method to process an image using default configuration.
     *
     * @param in  Input image path.
     * @param out Output directory path.
     * @throws IOException If an error occurs during processing.
     */
    public static void process(Path in, Path out) throws IOException {
        builder().processImage(in, out);
    }

    /**
     * Sets the user ID for logging, tracking, or storage purposes.
     *
     * @param id User identifier.
     * @return This {@code Pixelator} instance for chaining.
     */
    public Pixelator userId(String id) {
        config.setUserId(id);
        return this;
    }

    /**
     * Sets the minimum noise size (in pixels) to be filtered out.
     *
     * @param pixels Minimum number of pixels to be considered as noise.
     * @return This {@code Pixelator} instance for chaining.
     */
    public Pixelator minNoise(int pixels) {
        config.setMinNoiseSize(pixels);
        return this;
    }

    /**
     * Sets the target dimensions for the output image.
     *
     * @param width  Target width in pixels.
     * @param height Target height in pixels.
     * @return This {@code Pixelator} instance for chaining.
     */
    public Pixelator size(int width, int height) {
        config.setTargetWidth(width);
        config.setTargetHeight(height);
        return this;
    }

    /**
     * Sets how many top sprites (distinct patterns) to keep during pixelation.
     *
     * @param n Number of top sprites.
     * @return This {@code Pixelator} instance for chaining.
     */
    public Pixelator topSprites(int n) {
        config.setTopNSprites(n);
        return this;
    }

    /**
     * Sets the output formats for the pixelated image (e.g., "png", "svg").
     *
     * @param fmts Output format strings.
     * @return This {@code Pixelator} instance for chaining.
     */
    public Pixelator formats(String... fmts) {
        config.setOutputFormats(Set.of(fmts));
        return this;
    }

    /**
     * Executes the image pixelation process with the current configuration.
     *
     * @param in  Input image path.
     * @param out Output directory path.
     * @throws IOException If an error occurs during processing.
     */
    public void processImage(Path in, Path out) throws IOException {
        new PixelatorService(config).processImage(in, out);
    }
}
