package PngToVectConverter;

import PngToVectConverter.internal.Pixelator;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Entry point for the PNG to Vector Converter application.
 * <p>
 * This class initializes the {@link Pixelator} with user-defined parameters
 * and triggers the image conversion process.
 */
public class App {

    /**
     * Constructs a new {@code App}.
     */
    public App() {
        // Default Constructor
    }

    /**
     * Main method that runs the application.
     *
     * @param args Command-line arguments (not used).
     * @throws IOException If there is an error reading or writing files.
     */
    public static void main(String[] args) throws IOException {
        Pixelator.builder()
                .userId("alice")
                .minNoise(5)
                .size(64, 64)
                .topSprites(4)
                .formats("png", "svg");

        Pixelator.process(Paths.get("in.png"), Paths.get("outdir"));
    }
}
