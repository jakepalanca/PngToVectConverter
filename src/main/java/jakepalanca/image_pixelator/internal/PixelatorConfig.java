package jakepalanca.image_pixelator.internal;

import java.util.Set;

/**
 * Configuration object for the Pixelator processing pipeline.
 * <p>
 * This class holds various parameters that control the image processing workflow,
 * such as preprocessing thresholds, sprite extraction settings, output formats,
 * and S3 storage details. It allows fine-tuning of the pixelation process.
 */
public class PixelatorConfig {

    // ----- General / Auth Info -----

    /**
     * Flag indicating whether leftover smaller image fragments should be merged into a single sprite. Defaults to false.
     */
    private final boolean enableLeftoverMerge = false;

    // ----- Preprocessing thresholds -----
    /**
     * Flag indicating whether transparent edges should be trimmed from extracted sprites. Defaults to false.
     */
    private final boolean enableEdgeTrim = false;
    /**
     * The alpha channel value (0-255) below which pixels are considered fully transparent during processing. Defaults to 120.
     */
    private final int alphaCutoff = 120;
    /**
     * The minimum number of non-transparent pixels required for an extracted sprite to be considered valid. Defaults to 10.
     */
    private final int minVisiblePixels = 10;
    /**
     * A uniform scaling factor applied to sprites after extraction and trimming. Defaults to 0.0 (no scaling unless set).
     */
    private final double uniformScale = 0.0;

    // ----- Sprite extraction & scaling -----
    /**
     * Unique identifier for the user, often used for organizing outputs (e.g., S3 folder prefix).
     */
    private String userId;
    /**
     * The threshold for neighboring pixels used during the erosion morphological operation.
     */
    private int erosionNeighborsThreshold;
    /**
     * The threshold for neighboring pixels used during the dilation morphological operation.
     */
    private int dilationNeighborsThreshold;
    /**
     * The minimum size (in pixels) for a connected component to be considered valid; smaller components are removed as noise.
     */
    private int minNoiseSize;

    // ----- Output formats -----
    /**
     * The size of bins used for color quantization, reducing the number of distinct colors.
     */
    private int colorBinningSize;

    // ----- S3-related Info -----
    /**
     * The maximum number of largest sprites to extract and retain from the image.
     */
    private int topNSprites;

    // ----- Logging level -----
    /**
     * The target width (in pixels) to resize extracted sprites to.
     */
    private int targetWidth;
    /**
     * The target height (in pixels) to resize extracted sprites to.
     */
    private int targetHeight;
    /**
     * Flag indicating whether to disable the secondary glow removal step during sprite trimming. Defaults to true.
     */
    private boolean disableGlowRemovalInTrimmer = true;
    /**
     * A set specifying the desired output image formats (e.g., "png", "jpeg", "svg").
     */
    private Set<String> outputFormats;
    /**
     * The name of the AWS S3 bucket where processed output files will be stored.
     */
    private String bucketName;
    /**
     * The desired logging level for the processing pipeline (e.g., "DEBUG", "INFO", "WARN").
     */
    private String logLevel;

    /**
     * Constructs a new {@code PixelatorConfig} with default values.
     */
    public PixelatorConfig() {
        // Default Constructor
    }

    /**
     * Constructs a new {@code PixelatorConfig} with specified initial values.
     *
     * @param userId                      The unique identifier for the user.
     * @param erosionNeighborsThreshold   The threshold for erosion neighbors.
     * @param dilationNeighborsThreshold  The threshold for dilation neighbors.
     * @param minNoiseSize                The minimum size for noise removal.
     * @param colorBinningSize            The size for color binning.
     * @param topNSprites                 The number of top sprites to keep.
     * @param targetWidth                 The target width for resizing sprites.
     * @param targetHeight                The target height for resizing sprites.
     * @param disableGlowRemovalInTrimmer True to disable glow removal, false otherwise.
     * @param outputFormats               The set of desired output formats.
     * @param bucketName                  The S3 bucket name for output storage.
     * @param logLevel                    The logging level for the process.
     */
    public PixelatorConfig(
            String userId,
            int erosionNeighborsThreshold,
            int dilationNeighborsThreshold,
            int minNoiseSize,
            int colorBinningSize,
            int topNSprites,
            int targetWidth,
            int targetHeight,
            boolean disableGlowRemovalInTrimmer,
            Set<String> outputFormats,
            String bucketName,
            String logLevel) {
        this.userId = userId;
        this.erosionNeighborsThreshold = erosionNeighborsThreshold;
        this.dilationNeighborsThreshold = dilationNeighborsThreshold;
        this.minNoiseSize = minNoiseSize;
        this.colorBinningSize = colorBinningSize;
        this.topNSprites = topNSprites;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.disableGlowRemovalInTrimmer = disableGlowRemovalInTrimmer;
        this.outputFormats = outputFormats;
        this.bucketName = bucketName;
        this.logLevel = logLevel;
    }

    /**
     * Gets the user identifier.
     *
     * @return The user ID string.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier.
     *
     * @param userId The user ID string.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the erosion neighbors threshold.
     *
     * @return The erosion threshold value.
     */
    public int getErosionNeighborsThreshold() {
        return erosionNeighborsThreshold;
    }

    /**
     * Sets the erosion neighbors threshold.
     *
     * @param threshold The erosion threshold value.
     */
    public void setErosionNeighborsThreshold(int threshold) {
        this.erosionNeighborsThreshold = threshold;
    }

    /**
     * Gets the dilation neighbors threshold.
     *
     * @return The dilation threshold value.
     */
    public int getDilationNeighborsThreshold() {
        return dilationNeighborsThreshold;
    }

    /**
     * Sets the dilation neighbors threshold.
     *
     * @param threshold The dilation threshold value.
     */
    public void setDilationNeighborsThreshold(int threshold) {
        this.dilationNeighborsThreshold = threshold;
    }

    /**
     * Gets the minimum noise size for component removal.
     *
     * @return The minimum noise size in pixels.
     */
    public int getMinNoiseSize() {
        return minNoiseSize;
    }

    /**
     * Sets the minimum noise size for component removal.
     *
     * @param minNoiseSize The minimum noise size in pixels.
     */
    public void setMinNoiseSize(int minNoiseSize) {
        this.minNoiseSize = minNoiseSize;
    }

    /**
     * Gets the color binning size for quantization.
     *
     * @return The color binning size.
     */
    public int getColorBinningSize() {
        return colorBinningSize;
    }

    /**
     * Sets the color binning size for quantization.
     *
     * @param colorBinningSize The color binning size.
     */
    public void setColorBinningSize(int colorBinningSize) {
        this.colorBinningSize = colorBinningSize;
    }

    /**
     * Gets the maximum number of top sprites to retain.
     *
     * @return The number of top sprites.
     */
    public int getTopNSprites() {
        return topNSprites;
    }

    /**
     * Sets the maximum number of top sprites to retain.
     *
     * @param topNSprites The number of top sprites.
     */
    public void setTopNSprites(int topNSprites) {
        this.topNSprites = topNSprites;
    }

    /**
     * Gets the target width for resized sprites.
     *
     * @return The target width in pixels.
     */
    public int getTargetWidth() {
        return targetWidth;
    }

    /**
     * Sets the target width for resized sprites.
     *
     * @param targetWidth The target width in pixels.
     */
    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    /**
     * Gets the target height for resized sprites.
     *
     * @return The target height in pixels.
     */
    public int getTargetHeight() {
        return targetHeight;
    }

    /**
     * Sets the target height for resized sprites.
     *
     * @param targetHeight The target height in pixels.
     */
    public void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    /**
     * Checks if glow removal during trimming is disabled.
     *
     * @return true if glow removal is disabled, false otherwise.
     */
    public boolean isDisableGlowRemovalInTrimmer() {
        return disableGlowRemovalInTrimmer;
    }

    /**
     * Gets the set of desired output formats.
     *
     * @return A set of output format strings (e.g., "png", "jpeg").
     */
    public Set<String> getOutputFormats() {
        return outputFormats;
    }

    /**
     * Sets the desired output formats.
     *
     * @param outputFormats A set of output format strings.
     */
    public void setOutputFormats(Set<String> outputFormats) {
        this.outputFormats = outputFormats;
    }

    /**
     * Gets the S3 bucket name for output storage.
     *
     * @return The S3 bucket name string.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the S3 bucket name for output storage.
     *
     * @param bucketName The S3 bucket name string.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the configured logging level.
     *
     * @return The logging level string (e.g., "INFO").
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the logging level.
     *
     * @param logLevel The logging level string (e.g., "DEBUG").
     */
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Checks whether merging of leftover smaller components is enabled.
     *
     * @return true if leftover merging is enabled, false otherwise.
     */
    public boolean isEnableLeftoverMerge() {
        return enableLeftoverMerge;
    }

    /**
     * Checks whether trimming of transparent edges from sprites is enabled.
     *
     * @return true if edge trimming is enabled, false otherwise.
     */
    public boolean isEnableEdgeTrim() {
        return enableEdgeTrim;
    }

    /**
     * Gets the alpha cutoff threshold used to determine pixel transparency.
     *
     * @return The alpha cutoff value (0-255).
     */
    public int getAlphaCutoff() {
        return alphaCutoff;
    }

    /**
     * Gets the uniform scaling factor applied to sprites.
     *
     * @return The uniform scale factor (e.g., 1.0 means no scaling).
     */
    public double getUniformScale() {
        return uniformScale;
    }
}