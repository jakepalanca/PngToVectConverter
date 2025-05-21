package PngToVectConverter.internal;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Random;

import static org.bytedeco.opencv.global.opencv_core.*;

/**
 * Utility methods for image processing, color operations, visualization,
 * and conversions between BufferedImage and OpenCV Mat formats.
 */
class Utils {

    private static final Logger logger = LoggerFactory.getLogger(org.opencv.android.Utils.class);
    private static final Random random = new Random();

    /**
     * Default constructor.
     */
    Utils() {
        // Default Constructor
    }

    /**
     * Performs color binning (posterization) on a single ARGB color value.
     * Reduces color depth by mapping each channel to the start of its bin.
     * Alpha channel remains unchanged.
     *
     * @param argb    the input ARGB color integer
     * @param binSize the size of the color bin (e.g., 16, 32). Must be > 0.
     * @return the binned ARGB color integer, or original color if binSize <= 1
     */
    static int binColorARGB(int argb, int binSize) {
        if (binSize <= 1) {
            return argb;
        }
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        r = Math.min(255, (r / binSize) * binSize);
        g = Math.min(255, (g / binSize) * binSize);
        b = Math.min(255, (b / binSize) * binSize);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Calculates the shortest distance between two hues on the color wheel.
     *
     * @param hsb1 HSB values of the first color [hue, saturation, brightness]; hue in [0.0,1.0]
     * @param hsb2 HSB values of the second color [hue, saturation, brightness]; hue in [0.0,1.0]
     * @return the hue distance (0.0 to 0.5)
     * @throws InvalidPropertiesFormatException if input arrays are null or empty
     */
    static float hueDistance(float[] hsb1, float[] hsb2) throws InvalidPropertiesFormatException {
        if (hsb1 == null || hsb2 == null || hsb1.length < 1 || hsb2.length < 1) {
            logger.warn("Invalid HSB input for hueDistance calculation.");
            throw new InvalidPropertiesFormatException("Invalid HSB input for hueDistance calculation.");
        }
        float dh = Math.abs(hsb1[0] - hsb2[0]);
        return Math.min(dh, 1.0f - dh);
    }

    /**
     * Creates a visualization image where each detected connected component
     * is colored with a unique random color. Background pixels remain transparent.
     *
     * @param base   the original image, used for dimensions (can be null)
     * @param labels a 2D int array containing component labels (0=background, >0=component ID)
     * @return a BufferedImage visualizing components, or null if labels invalid
     */
    static BufferedImage visualizeComponents(BufferedImage base, int[][] labels) {
        if (labels == null || labels.length == 0 || labels[0].length == 0) {
            logger.error("Cannot visualize components: Invalid labels array provided.");
            return null;
        }
        int height = labels.length;
        int width = labels[0].length;
        if (base != null && (base.getWidth() != width || base.getHeight() != height)) {
            logger.warn("Base image dims ({}x{}) differ from labels dims ({}x{}). Using label dims.",
                    base.getWidth(), base.getHeight(), width, height);
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int maxLabel = 0;
        for (int[] row : labels) {
            for (int label : row) {
                if (label > maxLabel) maxLabel = label;
            }
        }
        logger.debug("Visualizing {} components (max label {}).", maxLabel, maxLabel);

        int[] labelColors = new int[maxLabel + 1];
        labelColors[0] = 0x00000000;
        for (int i = 1; i <= maxLabel; i++) {
            int r = random.nextInt(256);
            int g = random.nextInt(256);
            int b = random.nextInt(256);
            labelColors[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = labels[y][x];
                if (label >= 0 && label < labelColors.length) {
                    out.setRGB(x, y, labelColors[label]);
                } else {
                    logger.warn("Label {} at ({},{}) out of range [0..{}]. Setting transparent.", label, x, y, maxLabel);
                    out.setRGB(x, y, 0x00000000);
                }
            }
        }
        return out;
    }

    /**
     * Creates a visualization image highlighting the pixels belonging to provided sprites.
     * Each sprite is drawn with a random color on a transparent background.
     *
     * @param base    the original image for dimensions
     * @param sprites list of Sprite objects to visualize
     * @return a BufferedImage visualizing sprites, or null if inputs invalid
     */
    static BufferedImage visualizeSprites(BufferedImage base, List<Sprite> sprites) {
        if (base == null || sprites == null) {
            logger.error("Cannot visualize sprites: Base image or sprite list is null.");
            return null;
        }
        int width = base.getWidth();
        int height = base.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        logger.debug("Visualizing {} sprites on a {}x{} canvas.", sprites.size(), width, height);

        int[] spriteColors = new int[sprites.size()];
        for (int i = 0; i < sprites.size(); i++) {
            int r = random.nextInt(256);
            int g = random.nextInt(256);
            int b = random.nextInt(256);
            spriteColors[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }

        for (int i = 0; i < sprites.size(); i++) {
            Sprite sprite = sprites.get(i);
            if (sprite == null || sprite.getPixels() == null) {
                logger.warn("Skipping null sprite or sprite with null pixels at index {}.", i);
                continue;
            }
            int color = spriteColors[i];
            for (Sprite.PixelData pd : sprite.getPixels()) {
                if (pd.x() >= 0 && pd.x() < width && pd.y() >= 0 && pd.y() < height) {
                    out.setRGB(pd.x(), pd.y(), color);
                } else {
                    logger.warn("Pixel data ({},{}) for sprite {} out of bounds {}x{}.",
                            pd.x(), pd.y(), i, width, height);
                }
            }
        }
        return out;
    }

    /**
     * Replaces the pixel data within a Sprite with pixels from a BufferedImage.
     * Clears existing pixels and adds all pixels from the new image.
     * Does NOT recalculate center of mass.
     *
     * @param sprite   the Sprite to update
     * @param newImage the BufferedImage containing new pixels
     */
    static void replaceSpritePixels(Sprite sprite, BufferedImage newImage) {
        if (sprite == null || newImage == null) {
            logger.error("Cannot replace sprite pixels: Sprite or newImage is null.");
            return;
        }
        sprite.getPixels().clear();
        sprite.resetPixelCount();
        int width = newImage.getWidth();
        int height = newImage.getHeight();
        logger.debug("Replacing pixels for sprite {} with new image ({}x{}).", sprite.getLabelId(), width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = newImage.getRGB(x, y);
                sprite.addPixel(x, y, argb);
            }
        }
        logger.debug("Finished replacing pixels for sprite {}. New pixel count: {}.",
                sprite.getLabelId(), sprite.getPixelCount());
    }

    /**
     * Writes a String to a file using UTF-8 encoding. Creates parent directories if needed.
     *
     * @param content the String content to write
     * @param file    the destination File
     * @throws IOException if an I/O error occurs
     */
    static void writeStringToFile(String content, File file) throws IOException {
        if (content == null || file == null) {
            throw new IllegalArgumentException("Content and file cannot be null.");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            logger.debug("Creating directories for {}.", file.getAbsolutePath());
            if (!parent.mkdirs()) {
                logger.error("Failed to create directories for {}.", file.getAbsolutePath());
                throw new IOException("Could not create directories for " + file.getAbsolutePath());
            }
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
            logger.trace("Wrote content to {}.", file.getAbsolutePath());
        }
    }

    /**
     * Converts a BufferedImage to an OpenCV Mat in BGRA format (CV_8UC4).
     *
     * @param bi the input BufferedImage
     * @return the resulting Mat, or empty Mat if input invalid
     */
    static Mat bufferedImageToMat(BufferedImage bi) {
        if (bi == null) {
            logger.error("Input BufferedImage is null.");
            return new Mat();
        }
        int w = bi.getWidth(), h = bi.getHeight();
        if (w <= 0 || h <= 0) {
            logger.error("BufferedImage has zero dims {}x{}.", w, h);
            return new Mat();
        }
        BufferedImage img = bi;
        if (bi.getType() != BufferedImage.TYPE_INT_ARGB) {
            logger.warn("Converting image type {} to TYPE_INT_ARGB.", bi.getType());
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, w, h);
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(bi, 0, 0, null);
            g.dispose();
        }
        Mat mat = new Mat(h, w, CV_8UC4);
        int[] data = new int[w * h];
        img.getRGB(0, 0, w, h, data, 0, w);
        byte[] bytes = new byte[w * h * 4];
        for (int i = 0; i < data.length; i++) {
            int argb = data[i];
            bytes[i * 4] = (byte) (argb & 0xFF);
            bytes[i * 4 + 1] = (byte) ((argb >> 8) & 0xFF);
            bytes[i * 4 + 2] = (byte) ((argb >> 16) & 0xFF);
            bytes[i * 4 + 3] = (byte) ((argb >> 24) & 0xFF);
        }
        BytePointer bp = new BytePointer(bytes);
        mat.data(bp);
        mat.data().put(bytes);
        bp.close();
        return mat;
    }

    /**
     * Converts a BGRA OpenCV Mat into a TYPE_INT_ARGB BufferedImage.
     * Pixels with alpha==0 become fully transparent.
     *
     * @param mat the input Mat
     * @return the resulting BufferedImage, or null if input invalid
     */
    static BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) {
            logger.error("Input Mat is null or empty.");
            return null;
        }
        if (mat.channels() != 4 || mat.depth() != CV_8U) {
            logger.error("Mat must be 4-channel CV_8U. Found channels={}, depth={}.", mat.channels(), mat.depth());
            return null;
        }
        int width = mat.cols(), height = mat.rows();
        if (width <= 0 || height <= 0) {
            logger.error("Mat has zero dims {}x{}.", width, height);
            return null;
        }
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        byte[] matData = new byte[width * height * 4];
        mat.data().get(matData);
        int[] intData = new int[width * height];
        for (int i = 0; i < intData.length; i++) {
            int b = matData[i * 4] & 0xFF;
            int g = matData[i * 4 + 1] & 0xFF;
            int r = matData[i * 4 + 2] & 0xFF;
            int a = matData[i * 4 + 3] & 0xFF;
            intData[i] = (a == 0) ? 0x00000000 : (a << 24) | (r << 16) | (g << 8) | b;
        }
        bi.setRGB(0, 0, width, height, intData, 0, width);
        return bi;
    }

    /**
     * Checks if any pixel in the image is transparent.
     *
     * @param img the image to check
     * @return true if at least one pixel has alpha==0
     */
    static boolean hasTransparentPixel(BufferedImage img) {
        if (img == null) return false;
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
        for (int argb : pixels) {
            if ((argb >>> 24) == 0) return true;
        }
        return false;
    }

    /**
     * Applies a feathered alpha mask to a BGR image, returning a BufferedImage.
     *
     * @param bgr       the input BGR Mat
     * @param alphaMask single-channel alpha mask Mat (CV_8U)
     * @return the masked image, or null if inputs invalid
     */
    static BufferedImage applyFeatheredMask(Mat bgr, Mat alphaMask) {
        if (bgr == null || alphaMask == null || bgr.empty() || alphaMask.empty()) {
            logger.error("Cannot apply mask: BGR or alphaMask is null or empty.");
            return null;
        }
        if (bgr.rows() != alphaMask.rows() || bgr.cols() != alphaMask.cols()) {
            logger.error("Cannot apply mask: Dimension mismatch BGR({}x{}) vs alphaMask({}x{})."
                    , bgr.cols(), bgr.rows(), alphaMask.cols(), alphaMask.rows());
            return null;
        }
        if (alphaMask.channels() != 1 || alphaMask.depth() != CV_8U) {
            logger.error("Alpha mask must be single-channel CV_8U. Found channels={}, depth={}."
                    , alphaMask.channels(), alphaMask.depth());
            return null;
        }

        MatVector bgrChannels = new MatVector();
        split(bgr, bgrChannels);
        Mat alpha = alphaMask;

        MatVector bgraChannels = new MatVector(
                bgrChannels.get(0),
                bgrChannels.get(1),
                bgrChannels.get(2),
                alpha
        );
        Mat bgra = new Mat(bgr.rows(), bgr.cols(), CV_8UC4);
        merge(bgraChannels, bgra);

        BufferedImage out = matToBufferedImage(bgra);

        bgrChannels.deallocate();
        bgra.release();
        return out;
    }
}
