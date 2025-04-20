package jakepalanca.image_pixelator.internal;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class PixelatorService {
    private final PixelatorConfig config;

    public PixelatorService(PixelatorConfig config) {
        this.config = config;
    }

    public void processImage(Path inputImage, Path outputDir) throws IOException {
        BufferedImage original = ImageIO.read(inputImage.toFile());
        Files.createDirectories(outputDir);

        // 1) Preprocess
        BufferedImage pre = Utils.hasTransparentPixel(original)
                ? original
                : ImagePreprocessor.preprocess(original, config);
        assert pre != null;
        ImageIO.write(pre, "png", outputDir.resolve("step1_preprocessed.png").toFile());

        // 2) Connected components
        int[][] labels = ConnectedComponentDetector.detect(pre);
        BufferedImage ccVis = Utils.visualizeComponents(pre, labels);
        assert ccVis != null;
        ImageIO.write(ccVis, "png", outputDir.resolve("step2_components.png").toFile());

        // 3) Extract sprites
        List<Sprite> sprites = SpriteExtractor.extractSprites(
                pre, labels, config.getTopNSprites(), config);
        BufferedImage spriteVis = Utils.visualizeSprites(pre, sprites);
        assert spriteVis != null;
        ImageIO.write(spriteVis, "png", outputDir.resolve("step3_sprites.png").toFile());

        // 4) Trim, scale, and write
        for (int i = 0; i < sprites.size(); i++) {
            Sprite s = sprites.get(i);
            SpriteTrimmerAndGridMapper.trimAndScaleSprite(s, config);
            BufferedImage bi = s.getBufferedImage();
            ImageIO.write(bi, "png", outputDir.resolve("sprite_" + (i + 1) + ".png").toFile());
            if (config.getOutputFormats().contains("svg")) {
                String svg = SpriteVectorizer.vectorizeSprite(s);
                Files.writeString(outputDir.resolve("sprite_" + (i + 1) + ".svg"), svg);
            }
        }
    }
}
