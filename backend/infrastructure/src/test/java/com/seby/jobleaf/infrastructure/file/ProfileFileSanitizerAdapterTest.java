package com.seby.jobleaf.infrastructure.file;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileFileSanitizerAdapterTest {

    private static final String SECRET = "SECRET-MESSAGE-123";

    private final ProfileFileSanitizerAdapter sanitizer = new ProfileFileSanitizerAdapter();

    // ------------------------------------------------------------------ helpers that build real files

    private static byte[] png(int width, int height) throws Exception {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static byte[] concat(byte[] first, byte[] second) {
        return ByteBuffer.allocate(first.length + second.length).put(first).put(second).array();
    }

    /**
     * A PNG "tEXt" chunk (where programs store comments): length, type, data, checksum.
     */
    private static byte[] textChunk(String text) {
        byte[] data = ("Comment\0" + text).getBytes(StandardCharsets.ISO_8859_1);
        byte[] type = "tEXt".getBytes(StandardCharsets.ISO_8859_1);
        var crc = new CRC32();
        crc.update(type);
        crc.update(data);
        return ByteBuffer.allocate(12 + data.length).putInt(data.length).put(type).put(data)
                .putInt((int) crc.getValue()).array();
    }

    /**
     * Inserts the chunk right after the PNG header (8 bytes) and the IHDR chunk (25 bytes).
     */
    private static byte[] withChunk(byte[] png, byte[] chunk) {
        return ByteBuffer.allocate(png.length + chunk.length).put(png, 0, 33).put(chunk)
                .put(png, 33, png.length - 33).array();
    }

    /**
     * A tiny PNG whose header claims 50 000 x 50 000 pixels (2.5 billion pixels, about 10 GB once decoded).
     */
    private static byte[] imageBomb() throws Exception {
        byte[] png = png(1, 1);
        ByteBuffer.wrap(png).putInt(16, 50_000).putInt(20, 50_000);
        var crc = new CRC32();
        crc.update(png, 12, 17);                       // checksum over "IHDR" + its 13 data bytes
        ByteBuffer.wrap(png).putInt(29, (int) crc.getValue());
        return png;
    }

    private static boolean contains(byte[] content, String text) {
        return new String(content, StandardCharsets.ISO_8859_1).contains(text);
    }

    private static ProfileFile pdf(String content) {
        return new ProfileFile("cv.pdf", ProfileFileType.PDF, content.getBytes(StandardCharsets.ISO_8859_1));
    }

    // ------------------------------------------------------------------ pictures

    @Test
    @DisplayName("Should rebuild a picture as a fresh JPEG and drop a ZIP hidden after the end of the image")
    void appendedData_removed() throws Exception {
        byte[] hidden = concat(png(200, 100), ("PK\u0003\u0004" + SECRET).getBytes(StandardCharsets.ISO_8859_1));
        var upload = new ProfileFile("me.png", ProfileFileType.PNG, hidden);

        ProfileFile clean = sanitizer.sanitize(upload);

        assertThat(clean.type()).isEqualTo(ProfileFileType.JPEG);
        assertThat(clean.name()).isEqualTo("me.jpg");
        assertThat(contains(clean.content(), SECRET)).as("the hidden data is gone").isFalse();
        var image = ImageIO.read(new ByteArrayInputStream(clean.content()));
        assertThat(image.getWidth()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should drop metadata such as a PNG text chunk")
    void metadata_removed() throws Exception {
        byte[] withSecret = withChunk(png(50, 50), textChunk(SECRET));
        assertThat(contains(withSecret, SECRET)).as("the upload really carries the secret").isTrue();

        ProfileFile clean = sanitizer.sanitize(new ProfileFile("me.png", ProfileFileType.PNG, withSecret));

        assertThat(contains(clean.content(), SECRET)).isFalse();
    }

    @Test
    @DisplayName("Should scale big pictures down to at most 1024 px, keeping the proportions")
    void bigPicture_scaled() throws Exception {
        ProfileFile clean = sanitizer.sanitize(new ProfileFile("me.png", ProfileFileType.PNG, png(2048, 1024)));

        var image = ImageIO.read(new ByteArrayInputStream(clean.content()));
        assertThat(image.getWidth()).isEqualTo(1024);
        assertThat(image.getHeight()).isEqualTo(512);
    }

    @Test
    @DisplayName("Should refuse an image bomb before decoding it")
    void imageBomb_rejected() throws Exception {
        assertThatThrownBy(() -> sanitizer.sanitize(new ProfileFile("bomb.png", ProfileFileType.PNG, imageBomb())))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("profilePicture has too many pixels (max 40 megapixels)");
    }

    @Test
    @DisplayName("Should refuse a file that starts like a PNG but is not a real image")
    void brokenImage_rejected() {
        byte[] fake = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 'n', 'o', 't', ' ', 'a', 'n', ' ', 'i'};

        assertThatThrownBy(() -> sanitizer.sanitize(new ProfileFile("me.png", ProfileFileType.PNG, fake)))
                .hasMessage("profilePicture could not be read as an image");
    }

    // ------------------------------------------------------------------ PDFs

    @Test
    @DisplayName("Should keep a clean PDF unchanged")
    void cleanPdf_kept() {
        var resume = pdf("%PDF-1.7\n1 0 obj << /Type /Catalog >> endobj\n%%EOF\n");

        assertThat(sanitizer.sanitize(resume)).isSameAs(resume);
    }

    @Test
    @DisplayName("Should reject PDFs with scripts, attachments or launch actions")
    void activeContent_rejected() {
        for (String part : new String[]{"/JavaScript", "/JS (app.alert(1))", "/EmbeddedFile", "/Launch"}) {
            var resume = pdf("%PDF-1.7\n1 0 obj << " + part + " >> endobj\n%%EOF\n");
            assertThatThrownBy(() -> sanitizer.sanitize(resume))
                    .as(part)
                    .hasMessage("resume contains scripts, attachments or other active content, which is not allowed");
        }
    }

    @Test
    @DisplayName("Should not confuse similar names with dangerous ones (/JSomething is not /JS)")
    void similarNames_allowed() {
        assertThat(sanitizer.sanitize(pdf("%PDF-1.7\n<< /JSomething 1 >>\n%%EOF"))).isNotNull();
    }

    @Test
    @DisplayName("Should reject data hidden after the end of the PDF, and PDFs without an end")
    void trailingData_rejected() {
        assertThatThrownBy(() -> sanitizer.sanitize(pdf("%PDF-1.7\n%%EOF\nPK\u0003\u0004" + SECRET)))
                .hasMessage("resume contains extra data after the end of the PDF, which is not allowed");
        assertThatThrownBy(() -> sanitizer.sanitize(pdf("%PDF-1.7\n1 0 obj << >> endobj")))
                .hasMessage("resume is not a complete PDF file");
    }
}
