package PngToVectConverter.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Converts a sprite into a vector-based SVG representation.
 *
 * <p>Each fully opaque pixel (alpha = 255) becomes a 1x1 <code>&lt;rect&gt;</code> in the SVG.
 * The result is a pixel-accurate, scalable output matching the sprite's bounding box.
 */
class SpriteVectorizer {

    private static final Logger logger = LoggerFactory.getLogger(SpriteVectorizer.class);

    /**
     * Default constructor.
     */
    SpriteVectorizer() {
        // Default Constructor
    }

    /**
     * Generates an SVG string representation of a given sprite.
     *
     * <p>Each fully opaque pixel in the sprite becomes a 1x1 <code>&lt;rect&gt;</code>
     * in the SVG. The SVG size matches the sprite's image dimensions.
     *
     * @param sprite the sprite to vectorize
     * @return an SVG string, or an empty SVG if the sprite is null or invalid
     */
    static String vectorizeSprite(Sprite sprite) {
        if (sprite == null) {
            logger.warn("Cannot vectorize null sprite. Returning empty SVG.");
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"0\" height=\"0\"></svg>";
        }

        BufferedImage image = sprite.getBufferedImage();
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            logger.warn("Sprite {} has no valid BufferedImage or is empty. Returning empty SVG.", sprite.getLabelId());
            return String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\"></svg>",
                    (image != null ? image.getWidth() : 0), (image != null ? image.getHeight() : 0));
        }

        int width = image.getWidth();
        int height = image.getHeight();
        logger.debug("Vectorizing sprite label={} ({}x{})", sprite.getLabelId(), width, height);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" shape-rendering=\"crispEdges\">",
                width, height, width, height));

        int opaquePixelCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;

                if (alpha == 255) {
                    opaquePixelCount++;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    String hexColor = String.format("#%02X%02X%02X", r, g, b);
                    sb.append(String.format("<rect x=\"%d\" y=\"%d\" width=\"1\" height=\"1\" fill=\"%s\"/>",
                            x, y, hexColor));
                }
            }
        }

        sb.append("</svg>");

        logger.debug("Vectorization complete for sprite {}. Generated SVG with {} opaque pixel rectangles.",
                sprite.getLabelId(), opaquePixelCount);

        return sb.toString();
    }
}
