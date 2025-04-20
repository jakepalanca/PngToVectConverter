package jakepalanca.image_pixelator.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Extracts distinct sprites from a labeled pixel grid and optionally merges any extra
 * leftover sprites into the closest main sprite based on center-of-mass distance.
 */
class SpriteExtractor {
    private static final Logger logger = LoggerFactory.getLogger(SpriteExtractor.class);

    /**
     * Minimum number of pixels required for a region to be considered a valid sprite.
     */
    private static final int MIN_PIXEL_COUNT_THRESHOLD = 10;

    /**
     * Maximum distance between centers of mass to allow leftover sprites to be merged together.
     */
    private static final double LEFTOVER_UNIFY_DISTANCE = 20.0;

    /**
     * Default constructor.
     */
    SpriteExtractor() {
        // Default Constructor
    }

    /**
     * Extracts sprites from a labeled pixel matrix. If the number of sprites exceeds the expected count
     * and leftover merging is enabled, the excess sprites will be merged into their nearest main counterparts.
     *
     * @param image         the source image (used to retrieve ARGB values)
     * @param labeledPixels a 2D label matrix (0 = background, >0 = sprite label)
     * @param expectedCount number of top sprites to retain
     * @param config        pixelator configuration object
     * @return a list of final sprite objects
     */
    static List<Sprite> extractSprites(BufferedImage image,
                                       int[][] labeledPixels,
                                       int expectedCount,
                                       PixelatorConfig config) {
        logger.info("Starting sprite extraction...");

        if (image == null || labeledPixels == null ||
                labeledPixels.length == 0 || labeledPixels[0].length == 0) {
            logger.error("Invalid input provided (null or empty image/labels).");
            return Collections.emptyList();
        }

        int height = labeledPixels.length;
        int width = labeledPixels[0].length;
        Map<Integer, Sprite> spriteMap = new HashMap<>();

        // Group pixels into sprites
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = labeledPixels[y][x];
                if (label > 0) {
                    Sprite sprite = spriteMap.computeIfAbsent(label, Sprite::new);
                    try {
                        int argb = image.getRGB(x, y);
                        sprite.addPixel(x, y, argb);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        logger.error("Coordinate ({}, {}) out of bounds while reading color for label {}", x, y, label, e);
                    }
                }
            }
        }

        // Compute center of mass
        for (Sprite sprite : spriteMap.values()) {
            sprite.calculateCenterOfMass();
        }

        List<Sprite> allSprites = new ArrayList<>(spriteMap.values());
        allSprites.removeIf(sprite -> sprite.getPixelCount() < MIN_PIXEL_COUNT_THRESHOLD);
        allSprites.sort(Comparator.comparingInt(Sprite::getPixelCount).reversed());
        logger.debug("Extracted {} sprites (after noise filtering).", allSprites.size());

        // Return early if merging not needed
        if (allSprites.size() <= expectedCount || !config.isEnableLeftoverMerge()) {
            logger.info("Sprite extraction complete; no merging required.");
            return allSprites;
        }

        // Split main and leftover sprites
        List<Sprite> mainSprites = new ArrayList<>(allSprites.subList(0, expectedCount));
        List<Sprite> leftovers = new ArrayList<>(allSprites.subList(expectedCount, allSprites.size()));

        // Merge close leftover sprites
        List<Sprite> unifiedLeftovers = unifyCloseSprites(leftovers);

        // Merge unified leftovers into closest main sprite
        for (Sprite leftover : unifiedLeftovers) {
            leftover.calculateCenterOfMass();
            Sprite closestMain = findClosestSprite(mainSprites, leftover.getCenterX(), leftover.getCenterY());

            if (closestMain == null) {
                logger.warn("No main sprite found for leftover {}", leftover.getLabelId());
                continue;
            }

            for (Sprite.PixelData pd : leftover.getPixels()) {
                closestMain.addPixel(pd.x(), pd.y(), pd.argb());
            }
            closestMain.calculateCenterOfMass();
        }

        logger.info("Merging complete. Returning {} final sprites.", mainSprites.size());
        return mainSprites;
    }

    /**
     * Finds the sprite in the list whose center is closest to the given coordinates.
     *
     * @param candidates list of candidate sprites
     * @param x          target x-coordinate
     * @param y          target y-coordinate
     * @return the closest sprite by center-of-mass distance
     */
    private static Sprite findClosestSprite(List<Sprite> candidates, double x, double y) {
        Sprite closest = null;
        double minDist = Double.MAX_VALUE;

        for (Sprite sprite : candidates) {
            double dx = sprite.getCenterX() - x;
            double dy = sprite.getCenterY() - y;
            double distSq = dx * dx + dy * dy;

            if (distSq < minDist) {
                minDist = distSq;
                closest = sprite;
            }
        }

        return closest;
    }

    /**
     * Unifies leftover sprites that are within a given distance of each other
     * based on center-of-mass distance.
     *
     * @param leftoverSprites list of leftover sprites
     * @return a list of unified leftover sprite clusters
     */
    private static List<Sprite> unifyCloseSprites(List<Sprite> leftoverSprites) {
        if (leftoverSprites.isEmpty()) return leftoverSprites;

        List<Sprite> working = new ArrayList<>(leftoverSprites);
        boolean mergedSomething = true;

        while (mergedSomething) {
            mergedSomething = false;

            outer:
            for (int i = 0; i < working.size(); i++) {
                Sprite s1 = working.get(i);
                for (int j = i + 1; j < working.size(); j++) {
                    Sprite s2 = working.get(j);

                    double dx = s1.getCenterX() - s2.getCenterX();
                    double dy = s1.getCenterY() - s2.getCenterY();
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < SpriteExtractor.LEFTOVER_UNIFY_DISTANCE) {
                        for (Sprite.PixelData pd : s2.getPixels()) {
                            s1.addPixel(pd.x(), pd.y(), pd.argb());
                        }
                        s1.calculateCenterOfMass();
                        working.remove(j);
                        mergedSomething = true;
                        break outer;
                    }
                }
            }
        }

        return working;
    }
}
