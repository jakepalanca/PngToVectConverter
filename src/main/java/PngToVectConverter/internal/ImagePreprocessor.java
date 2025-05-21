package PngToVectConverter.internal;

import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles preprocessing of images to isolate foreground from background,
 * preserving pixel detail for pixel-art effects.
 */
class ImagePreprocessor {
    private static final Logger logger = LoggerFactory.getLogger(ImagePreprocessor.class);

    // Morphological kernel size for cleanup (3x3)
    private static final Size MORPH_KERNEL_SIZE = new Size(3, 3);

    /**
     * Preprocesses a BufferedImage by removing the background color and making
     * those pixels fully transparent. Pixel edges are preserved.
     *
     * @param original the original ARGB image to preprocess
     * @param config
     * @return a new BufferedImage with background removed
     */
    static BufferedImage preprocess(BufferedImage original, PixelatorConfig config) {
        if (original == null) {
            logger.error("Input image is null.");
            return null;
        }
        logger.info("Starting preprocessing...");

        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Use the top-left corner pixel as the "background" color
        int bgARGB = original.getRGB(0, 0);
        int bgRGB = bgARGB & 0x00FFFFFF;  // ignore alpha channel

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = original.getRGB(x, y);
                int rgb = argb & 0x00FFFFFF;
                if (rgb == bgRGB) {
                    // make border/background transparent
                    result.setRGB(x, y, 0x00000000);
                } else {
                    // keep the original pixel intact (including its alpha)
                    result.setRGB(x, y, argb);
                }
            }
        }

        logger.info("Preprocessing complete.");
        return result;
    }

    /**
     * Computes the average HSV color along the image border to estimate the background.
     *
     * @param hsv the HSV image
     * @return a Scalar representing the average border color in HSV
     */
    private static Scalar computeDominantBackgroundColor(Mat hsv) {
        int rows = hsv.rows(), cols = hsv.cols();
        List<double[]> samples = new ArrayList<>();
        if (rows < 2 || cols < 2) return new Scalar(0, 0, 0, 0);

        UByteIndexer idx = hsv.createIndexer();
        for (int x = 0; x < cols; x++) {
            samples.add(new double[]{idx.get(0, x, 0) & 0xFF, idx.get(0, x, 1) & 0xFF, idx.get(0, x, 2) & 0xFF});
            samples.add(new double[]{idx.get(rows - 1, x, 0) & 0xFF, idx.get(rows - 1, x, 1) & 0xFF, idx.get(rows - 1, x, 2) & 0xFF});
        }
        for (int y = 1; y < rows - 1; y++) {
            samples.add(new double[]{idx.get(y, 0, 0) & 0xFF, idx.get(y, 0, 1) & 0xFF, idx.get(y, 0, 2) & 0xFF});
            samples.add(new double[]{idx.get(y, cols - 1, 0) & 0xFF, idx.get(y, cols - 1, 1) & 0xFF, idx.get(y, cols - 1, 2) & 0xFF});
        }
        idx.release();
        double sumH = 0, sumS = 0, sumV = 0;
        for (double[] v : samples) {
            sumH += v[0];
            sumS += v[1];
            sumV += v[2];
        }
        int n = samples.size();
        return new Scalar(sumH / n, sumS / n, sumV / n, 0);
    }

    /**
     * Creates a binary mask identifying the background using HSV flood fill.
     *
     * @param hsv   the HSV image
     * @param bgHSV the estimated background HSV color
     * @param hTol  hue tolerance
     * @param sTol  saturation tolerance
     * @param vTol  value tolerance
     * @return binary Mat mask where background pixels are 255
     */
    private static Mat floodFillBackground(Mat hsv, Scalar bgHSV, int hTol, int sTol, int vTol) {
        int rows = hsv.rows(), cols = hsv.cols();
        Mat floodMask = Mat.zeros(rows + 2, cols + 2, opencv_core.CV_8U).asMat();
        Scalar lo = new Scalar(hTol, sTol, vTol, 0), hi = new Scalar(hTol, sTol, vTol, 0);
        int flags = 4 | opencv_imgproc.FLOODFILL_FIXED_RANGE | opencv_imgproc.FLOODFILL_MASK_ONLY | (1 << 8);
        for (int x = 0; x < cols; x++) {
            if (floodMask.ptr(1, x + 1).get() == 0)
                opencv_imgproc.floodFill(hsv, floodMask, new Point(x, 0), new Scalar(1), new Rect(), lo, hi, flags);
            if (floodMask.ptr(rows, x + 1).get() == 0)
                opencv_imgproc.floodFill(hsv, floodMask, new Point(x, rows - 1), new Scalar(1), new Rect(), lo, hi, flags);
        }
        for (int y = 0; y < rows; y++) {
            if (floodMask.ptr(y + 1, 1).get() == 0)
                opencv_imgproc.floodFill(hsv, floodMask, new Point(0, y), new Scalar(1), new Rect(), lo, hi, flags);
            if (floodMask.ptr(y + 1, cols).get() == 0)
                opencv_imgproc.floodFill(hsv, floodMask, new Point(cols - 1, y), new Scalar(1), new Rect(), lo, hi, flags);
        }
        Mat inv = new Mat();
        Rect roi = new Rect(1, 1, cols, rows);
        opencv_imgproc.threshold(new Mat(floodMask, roi), inv, 0, 255, opencv_imgproc.THRESH_BINARY_INV);
        floodMask.release();
        return inv;
    }

    /**
     * Expands the background mask by including pixels similar to the estimated background color.
     *
     * @param mask  the binary mask
     * @param hsv   the HSV image
     * @param bgHSV the estimated background HSV color
     * @param hTol  hue tolerance
     * @param sTol  saturation tolerance
     * @param vTol  value tolerance
     */
    private static void expandBackground(Mat mask, Mat hsv, Scalar bgHSV, int hTol, int sTol, int vTol) {
        int rows = mask.rows(), cols = mask.cols();
        UByteIndexer midx = mask.createIndexer();
        UByteIndexer hidx = hsv.createIndexer();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if ((midx.get(r, c) & 0xFF) == 255) {
                    int ph = hidx.get(r, c, 0) & 0xFF, ps = hidx.get(r, c, 1) & 0xFF, pv = hidx.get(r, c, 2) & 0xFF;
                    int dH = Math.abs(ph - (int) bgHSV.get(0));
                    if ((dH <= hTol || dH >= 180 - hTol) && Math.abs(ps - (int) bgHSV.get(1)) <= sTol && Math.abs(pv - (int) bgHSV.get(2)) <= vTol)
                        midx.put(r, c, 0);
                }
            }
        midx.release();
        hidx.release();
    }

    /**
     * Applies morphological open and close operations to clean up the mask.
     *
     * @param mask the binary mask to refine
     */
    private static void refineMaskMorphology(Mat mask) {
        Mat k = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, MORPH_KERNEL_SIZE);
        opencv_imgproc.morphologyEx(mask, mask, opencv_imgproc.MORPH_OPEN, k);
        opencv_imgproc.morphologyEx(mask, mask, opencv_imgproc.MORPH_CLOSE, k);
        k.release();
    }

    /**
     * Removes small connected components from the mask that are below the given size.
     *
     * @param mask    the binary mask to clean
     * @param minSize the minimum size of connected regions to keep
     */
    private static void removeSmallNoise(Mat mask, int minSize) {
        Mat labels = new Mat(), stats = new Mat(), cents = new Mat();
        int nLabs = opencv_imgproc.connectedComponentsWithStats(mask, labels, stats, cents, 8, opencv_core.CV_32S);
        if (nLabs <= 1) {
            labels.release();
            stats.release();
            cents.release();
            return;
        }
        IntIndexer sidx = stats.createIndexer();
        IntIndexer lidx = labels.createIndexer();
        UByteIndexer midx = mask.createIndexer();
        Set<Integer> toRemove = new HashSet<>();
        for (int lbl = 1; lbl < nLabs; lbl++)
            if (sidx.get(lbl, opencv_imgproc.CC_STAT_AREA) < minSize) toRemove.add(lbl);
        for (int r = 0; r < labels.rows(); r++)
            for (int c = 0; c < labels.cols(); c++)
                if (toRemove.contains(lidx.get(r, c))) midx.put(r, c, 0);
        sidx.release();
        lidx.release();
        midx.release();
        labels.release();
        stats.release();
        cents.release();
    }

    /**
     * Applies a feathered alpha mask to an image to blend transparency smoothly.
     *
     * @param bgr       the original image in BGR format
     * @param alphaMask the alpha mask to apply
     * @return the resulting BufferedImage with feathered transparency
     */
    private static BufferedImage applyFeatheredMask(Mat bgr, Mat alphaMask) {
        return Utils.applyFeatheredMask(bgr, alphaMask);
    }
}
