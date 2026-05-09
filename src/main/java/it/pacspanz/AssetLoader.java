package it.pacspanz;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public final class AssetLoader {
    private AssetLoader() {
    }

    public record LoadedImage(BufferedImage image, String resourcePath) {
    }

    public static BufferedImage loadImage(String resourcePath) {
        try (InputStream stream = AssetLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    public static LoadedImage loadFirstAvailable(String loadedLabel, String fallbackLabel, String... resourcePaths) {
        for (String resourcePath : resourcePaths) {
            BufferedImage image = loadImage(resourcePath);
            if (image != null) {
                System.out.println("Caricato " + loadedLabel + ": " + resourcePath);
                return new LoadedImage(image, resourcePath);
            }
        }

        System.out.println(fallbackLabel + " non trovato, uso la risorsa sostitutiva");
        return null;
    }

    public static BufferedImage makeNearWhiteTransparent(BufferedImage source, int threshold) {
        BufferedImage transparentImage = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                if (alpha > 0 && red > threshold && green > threshold && blue > threshold) {
                    transparentImage.setRGB(x, y, 0x00000000);
                } else {
                    transparentImage.setRGB(x, y, argb);
                }
            }
        }

        return transparentImage;
    }

    public static Font loadFont(String resourcePath) {
        try (InputStream stream = AssetLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                System.out.println("Font arcade non trovato, uso il carattere sostitutivo");
                return null;
            }

            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            System.out.println("Font arcade caricato: " + resourcePath);
            return font;
        } catch (IOException | FontFormatException | IllegalArgumentException ex) {
            System.out.println("Font arcade non caricato, uso il carattere sostitutivo");
            return null;
        }
    }
}
