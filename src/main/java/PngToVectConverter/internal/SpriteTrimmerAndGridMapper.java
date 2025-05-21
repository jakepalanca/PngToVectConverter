package PngToVectConverter.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Trims, scales, centers, and cleans up a sprite image for final output.
 *
 * <p>Steps include:
 * <ol>
 *     <li>Cropping to bounding box</li>
 *     <li>Computing center of mass</li>
 *     <li>Scaling to fit within target dimensions</li>
 *     <li>Centering with clamped offsets</li>
 *     <li>Removing near-white edge pixels</li>
 *     <li>Optional glow removal</li>
 *     <li>Applying alpha threshold</li>
 *     <li>Updating the sprite</li>
 * </ol>
 */
class SpriteTrimmerAndGridMapper {

    private static final Logger logger = LoggerFactory.getLogger(SpriteTrimmerAndGridMapper.class);

    private static final float EDGE_FUZZ_MIN_BRIGHTNESS = 0.80f;
    private static final float EDGE_FUZZ_MAX_SATURATION = 0.20f;

    /**
     * Default constructor.
     */
    SpriteTrimmerAndGridMapper() {
        // Default Constructor
    }

    /**
     * Processes the sprite by cropping, scaling, centering, and cleaning it.
     *
     * @param sprite the sprite to process
     * @param config configuration for pixel dimensions and thresholds
     */
    static void trimAndScaleSprite(Sprite sprite, PixelatorConfig config) {
        logger.debug("Trimming/scaling sprite ID: {}", sprite.getLabelId());

        BufferedImage cropped = sprite.getBufferedImage();
        if (cropped == null || cropped.getWidth() == 0 || cropped.getHeight() == 0) {
            logger.warn("Sprite {} has empty bounding box. Returning a transparent {}x{} image.",
                    sprite.getLabelId(), config.getTargetWidth(), config.getTargetHeight());
            BufferedImage empty = new BufferedImage(config.getTargetWidth(), config.getTargetHeight(), BufferedImage.TYPE_INT_ARGB);
            Utils.replaceSpritePixels(sprite, empty);
            sprite.calculateCenterOfMass();
            return;
        }

        int croppedW = cropped.getWidth();
        int croppedH = cropped.getHeight();
        logger.debug("Sprite {} bounding box => {}x{}", sprite.getLabelId(), croppedW, croppedH);

        double sumX = 0, sumY = 0;
        int opaqueCount = 0;
        for (int y = 0; y < croppedH; y++) {
            for (int x = 0; x < croppedW; x++) {
                int alpha = (cropped.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha != 0) {
                    sumX += x;
                    sumY += y;
                    opaqueCount++;
                }
            }
        }

        if (opaqueCount == 0) {
            logger.warn("Sprite {}: All pixels are transparent after bounding box. Returning empty.",
                    sprite.getLabelId());
            BufferedImage empty = new BufferedImage(config.getTargetWidth(), config.getTargetHeight(), BufferedImage.TYPE_INT_ARGB);
            Utils.replaceSpritePixels(sprite, empty);
            sprite.calculateCenterOfMass();
            return;
        }

        double croppedCOMx = sumX / opaqueCount;
        double croppedCOMy = sumY / opaqueCount;

        double scale = config.getUniformScale() > 0
                ? config.getUniformScale()
                : Math.min(
                (double) config.getTargetWidth() / croppedW,
                (double) config.getTargetHeight() / croppedH);

        int scaledW = Math.max(1, (int) Math.round(croppedW * scale));
        int scaledH = Math.max(1, (int) Math.round(croppedH * scale));

        logger.debug("Sprite {} scale factor => {} => scaled dimensions {}x{}",
                sprite.getLabelId(),
                String.format("%.3f", scale),
                scaledW, scaledH);

        BufferedImage scaledImage = partialCoverageScale(cropped, scaledW, scaledH);

        int offsetX;
        int offsetY;

        BufferedImage finalImage = new BufferedImage(config.getTargetWidth(), config.getTargetHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gFinal = finalImage.createGraphics();
        gFinal.setComposite(AlphaComposite.Clear);
        gFinal.fillRect(0, 0, config.getTargetWidth(), config.getTargetHeight());
        gFinal.setComposite(AlphaComposite.SrcOver);

        offsetX = (config.getTargetWidth() - scaledW) / 2;
        offsetY = (config.getTargetHeight() - scaledH) / 2;

        offsetX = Math.max(0, Math.min(offsetX, config.getTargetWidth() - scaledW));
        offsetY = Math.max(0, Math.min(offsetY, config.getTargetHeight() - scaledH));

        gFinal.drawImage(scaledImage, offsetX, offsetY, null);
        gFinal.dispose();

        if (config.isEnableEdgeTrim()) {
            removeNearWhiteEdges(finalImage);
        }

        if (!config.isDisableGlowRemovalInTrimmer()) {
            logger.warn("Applying secondary glow removal (discouraged) for sprite {}...", sprite.getLabelId());
            removeGlowFromEdges(finalImage);
        }

        int cutoff = config.getAlphaCutoff();
        if (cutoff > 0) {
            applyAlphaThresholdAndSolidify(finalImage, cutoff);
        }

        Utils.replaceSpritePixels(sprite, finalImage);
        sprite.calculateCenterOfMass();
        logger.info("Trim/scale done. Sprite {} => final {}x{}", sprite.getLabelId(), config.getTargetWidth(), config.getTargetHeight());
    }

    /**
     * Scales the input image using nearest-neighbor to preserve sharp edges.
     *
     * @param source source image
     * @param finalW target width
     * @param finalH target height
     * @return scaled image
     */
    private static BufferedImage partialCoverageScale(BufferedImage source, int finalW, int finalH) {
        if (source == null) return null;
        BufferedImage scaled = new BufferedImage(finalW, finalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, finalW, finalH, null);
        g.dispose();
        return scaled;
    }

    /**
     * Removes near-white edge pixels using a BFS starting from the image borders.
     *
     * @param image image to modify
     */
    private static void removeNearWhiteEdges(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        boolean[][] visited = new boolean[h][w];
        Queue<Point> queue = new LinkedList<>();

        for (int x = 0; x < w; x++) {
            queue.add(new Point(x, 0));
            visited[0][x] = true;
            queue.add(new Point(x, h - 1));
            visited[h - 1][x] = true;
        }
        for (int y = 0; y < h; y++) {
            queue.add(new Point(0, y));
            visited[y][0] = true;
            queue.add(new Point(w - 1, y));
            visited[y][w - 1] = true;
        }

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int removedCount = 0;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int px = p.x, py = p.y;
            if (px < 0 || py < 0 || px >= w || py >= h) continue;

            int argb = image.getRGB(px, py);
            int alpha = (argb >>> 24) & 0xFF;
            float[] hsb = argbToHSB(argb);

            if (alpha != 0 && hsb[2] >= SpriteTrimmerAndGridMapper.EDGE_FUZZ_MIN_BRIGHTNESS && hsb[1] <= SpriteTrimmerAndGridMapper.EDGE_FUZZ_MAX_SATURATION) {
                image.setRGB(px, py, 0x00000000);
                removedCount++;
            }

            for (int[] d : directions) {
                int nx = px + d[0], ny = py + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.add(new Point(nx, ny));
                }
            }
        }

        if (removedCount > 0) {
            logger.debug("removeNearWhiteEdges => removed {} near-white pixels from edges.", removedCount);
        }
    }

    /**
     * Converts an ARGB color to HSB.
     *
     * @param argb ARGB color
     * @return HSB float array
     */
    private static float[] argbToHSB(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = (argb) & 0xFF;
        return Color.RGBtoHSB(r, g, b, null);
    }

    /**
     * Applies an alpha cutoff to remove semi-transparent pixels.
     *
     * @param image  image to modify
     * @param cutoff alpha threshold
     */
    private static void applyAlphaThresholdAndSolidify(BufferedImage image, int cutoff) {
        int w = image.getWidth();
        int h = image.getHeight();
        int toTransparent = 0;
        int toOpaque = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) continue;

                if (alpha < cutoff) {
                    image.setRGB(x, y, 0x00000000);
                    toTransparent++;
                } else {
                    int rgb = argb & 0x00FFFFFF;
                    image.setRGB(x, y, 0xFF000000 | rgb);
                    toOpaque++;
                }
            }
        }

        logger.debug("Alpha threshold done: {} => transparent, {} => opaque.", toTransparent, toOpaque);
    }

    /**
     * Performs a secondary glow removal using background color estimation.
     *
     * @param image image to clean
     */
    private static void removeGlowFromEdges(BufferedImage image) {
        logger.warn("Executing removeGlowFromEdges - generally discouraged.");
        float[] bgHSB = estimateBackgroundHSBFromCorners(image);
        floodFillTransparentEdges(image, bgHSB);
    }

    /**
     * Estimates background color by averaging the HSB values of the image corners.
     *
     * @param img image to sample
     * @return average HSB values
     */
    private static float[] estimateBackgroundHSBFromCorners(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w == 0 || h == 0) return new float[]{0f, 0f, 0f};

        List<float[]> hsbs = List.of(
                argbToHSB(img.getRGB(0, 0)),
                argbToHSB(img.getRGB(w - 1, 0)),
                argbToHSB(img.getRGB(0, h - 1)),
                argbToHSB(img.getRGB(w - 1, h - 1))
        );

        double sumH = 0, sumS = 0, sumB = 0;
        for (float[] hsb : hsbs) {
            sumH += hsb[0];
            sumS += hsb[1];
            sumB += hsb[2];
        }

        return new float[]{
                (float) (sumH / hsbs.size()),
                (float) (sumS / hsbs.size()),
                (float) (sumB / hsbs.size())
        };
    }

    /**
     * Removes edge pixels close to a background HSB value using BFS.
     *
     * @param image image to modify
     * @param bgHSB background HSB color
     */
    private static void floodFillTransparentEdges(BufferedImage image,
                                                  float[] bgHSB) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean[][] visited = new boolean[h][w];
        Queue<Point> queue = new LinkedList<>();

        for (int x = 0; x < w; x++) {
            queue.add(new Point(x, 0));
            visited[0][x] = true;
            queue.add(new Point(x, h - 1));
            visited[h - 1][x] = true;
        }
        for (int y = 0; y < h; y++) {
            queue.add(new Point(0, y));
            visited[y][0] = true;
            queue.add(new Point(w - 1, y));
            visited[y][w - 1] = true;
        }

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int px = p.x, py = p.y;
            int argb = image.getRGB(px, py);
            int alpha = (argb >>> 24) & 0xFF;
            boolean remove;

            if (alpha != 0) {
                float[] hsb = argbToHSB(argb);
                float dh = Math.min(Math.abs(hsb[0] - bgHSB[0]), 1f - Math.abs(hsb[0] - bgHSB[0]));
                float ds = Math.abs(hsb[1] - bgHSB[1]);
                float db = Math.abs(hsb[2] - bgHSB[2]);
                remove = dh <= (float) 0.1 && ds <= (float) 0.25 && db <= (float) 0.25;
            } else {
                remove = true;
            }

            if (remove && alpha != 0) {
                image.setRGB(px, py, 0x00000000);
            }

            for (int[] d : dirs) {
                int nx = px + d[0], ny = py + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.add(new Point(nx, ny));
                }
            }
        }

        logger.warn("Finished secondary glow remove BFS from edges.");
    }
}
