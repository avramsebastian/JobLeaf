package com.seby.jobleaf.infrastructure.file;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.domain.port.out.profile.FileSanitizerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Content Disarm and Reconstruction (CDR): instead of trying to find hidden content, rebuild the file so nothing
 * hidden can survive.
 * <ul>
 *     <li><b>Pictures</b> are decoded to pixels, scaled to at most {@value #MAX_SIDE} px and written as a brand-new
 *     JPEG. Only the pixels are copied, so metadata (EXIF, GPS, PNG text chunks) and data appended after the image
 *     are left behind, and scaling plus JPEG compression change every pixel slightly, which destroys data hidden in
 *     the lowest bits of the pixels.</li>
 *     <li><b>PDFs</b> can not be rebuilt without a PDF library, so they are checked instead: no scripts, embedded files
 *     or launch actions, and nothing after the end marker {@code %%EOF}.</li>
 * </ul>
 */
@Slf4j
@Component
public class ProfileFileSanitizerAdapter implements FileSanitizerPort {

    /** Longest side of a stored profile picture. */
    static final int MAX_SIDE = 1024;
    /** Refuse to decode bigger images: a tiny file can claim huge dimensions and fill the memory ("image bomb"). */
    static final long MAX_PIXELS = 40_000_000L;

    /**
     * PDF names for content that runs or hides something. The lookahead makes sure the whole name matches
     * ({@code /JS} but not {@code /JSomething}).
     */
    private static final Pattern DANGEROUS_PDF_CONTENT = Pattern.compile(
            "/(JavaScript|JS|Launch|EmbeddedFile|EmbeddedFiles|RichMedia|XFA)(?![A-Za-z0-9])");
    private static final String PDF_END_MARKER = "%%EOF";

    @Override
    public ProfileFile sanitize(ProfileFile file) {
        return switch (file.type()) {
            case PNG, JPEG -> rebuildPicture(file);
            case PDF -> checkPdf(file);
        };
    }

    // ---------------------------------------------------------------- pictures

    private ProfileFile rebuildPicture(ProfileFile file) {
        checkDimensions(file.content());

        BufferedImage original = decode(file.content());
        int[] size = scaledSize(original.getWidth(), original.getHeight());

        // A new, empty image: only what we draw into it will exist in the stored file
        BufferedImage copy = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.setColor(Color.WHITE);                       // transparent parts of a PNG become white
            graphics.fillRect(0, 0, size[0], size[1]);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(original, 0, 0, size[0], size[1], null);
        } finally {
            graphics.dispose();
        }

        byte[] rebuilt = encodeJpeg(copy);
        log.info("Rebuilt profile picture {}: {}x{} {} ({} bytes) -> {}x{} JPEG ({} bytes)", file.name(),
                original.getWidth(), original.getHeight(), file.type(), file.size(), size[0], size[1], rebuilt.length);
        return new ProfileFile(asJpegName(file.name()), ProfileFileType.JPEG, rebuilt);
    }

    /**
     * Reads only the width and height from the file header, before decoding, so an image bomb is refused without
     * ever allocating its pixels.
     */
    private static void checkDimensions(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidPicture();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw DomainValidationException.invalidField("profilePicture",
                            "profilePicture has too many pixels (max 40 megapixels)");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw invalidPicture();
        }
    }

    private static BufferedImage decode(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw invalidPicture();
            }
            return image;
        } catch (IOException e) {
            // Correct magic bytes but broken inside: not a usable image
            throw invalidPicture();
        }
    }

    /**
     * @return {width, height} with the longest side at most {@value #MAX_SIDE}, keeping the proportions
     */
    static int[] scaledSize(int width, int height) {
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(width, height));
        return new int[]{Math.max(1, (int) Math.round(width * scale)), Math.max(1, (int) Math.round(height * scale))};
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        try (var output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "jpg", output)) {
                throw new IllegalStateException("No JPEG writer available");
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write the rebuilt picture", e);
        }
    }

    /**
     * "me.png" becomes "me.jpg": the stored file really is a JPEG now.
     */
    static String asJpegName(String name) {
        int dot = name.lastIndexOf('.');
        return (dot < 0 ? name : name.substring(0, dot)) + ".jpg";
    }

    private static DomainValidationException invalidPicture() {
        return DomainValidationException.invalidField("profilePicture", "profilePicture could not be read as an image");
    }

    // ---------------------------------------------------------------- PDFs

    private ProfileFile checkPdf(ProfileFile file) {
        // ISO-8859-1 turns every byte into exactly one character, so the raw bytes can be searched like text
        String text = new String(file.content(), StandardCharsets.ISO_8859_1);

        var dangerous = DANGEROUS_PDF_CONTENT.matcher(text);
        if (dangerous.find()) {
            log.warn("Rejected resume {}: contains {}", file.name(), dangerous.group());
            throw DomainValidationException.invalidField("resume",
                    "resume contains scripts, attachments or other active content, which is not allowed");
        }

        int end = text.lastIndexOf(PDF_END_MARKER);
        if (end < 0) {
            throw DomainValidationException.invalidField("resume", "resume is not a complete PDF file");
        }
        if (!text.substring(end + PDF_END_MARKER.length()).isBlank()) {
            log.warn("Rejected resume {}: data after the end of the PDF", file.name());
            throw DomainValidationException.invalidField("resume",
                    "resume contains extra data after the end of the PDF, which is not allowed");
        }
        return file;
    }
}
