package com.website.gis.core.storage;

import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString());
    }

    private byte[] createTestPngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createTestJpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    @Test
    void whenStoreValidPngImage_thenSuccessAndOptimized() throws Exception {
        byte[] pngBytes = createTestPngBytes(2000, 1000);
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.png", "image/png", pngBytes
        );

        StoredFile stored = storageService.store(file, "ocop");

        assertNotNull(stored);
        assertEquals("sample.png", stored.originalFileName());
        assertEquals("image/png", stored.contentType());
        assertTrue(stored.storedFileName().startsWith("ocop/"));
        assertTrue(stored.publicUrl().startsWith("/api/files/ocop/"));

        // Verify file exists on disk
        Resource resource = storageService.loadAsResource(stored.storedFileName());
        assertTrue(resource.exists());

        // Verify image was resized to max 1600 width
        BufferedImage savedImage = ImageIO.read(resource.getFile());
        assertNotNull(savedImage);
        assertTrue(savedImage.getWidth() <= 1600);
    }

    @Test
    void whenStoreValidJpegImage_thenSuccess() throws Exception {
        byte[] jpgBytes = createTestJpegBytes(800, 600);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpgBytes
        );

        StoredFile stored = storageService.store(file, null);

        assertNotNull(stored);
        assertEquals("image/jpeg", stored.contentType());
        assertFalse(stored.storedFileName().contains("/"));
    }

    @Test
    void whenStoreValidPdfDocument_thenSuccess() throws Exception {
        // PDF magic header: %PDF
        byte[] pdfBytes = "%PDF-1.4 sample pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", pdfBytes
        );

        StoredFile stored = storageService.store(file, "documents");

        assertNotNull(stored);
        assertEquals("application/pdf", stored.contentType());
        assertTrue(stored.storedFileName().startsWith("documents/"));
    }

    @Test
    void whenStoreSpoofedFile_thenRejectWithBadRequest() {
        // Text file named as .png
        byte[] fakeBytes = "This is a plain text file pretending to be PNG".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", fakeBytes
        );

        assertThrows(BadRequestException.class, () -> storageService.store(file, "test"));
    }

    @Test
    void whenPathTraversalAttempt_thenRejectWithBadRequest() {
        assertThrows(BadRequestException.class, () -> storageService.loadAsResource("../secret.txt"));
        assertThrows(BadRequestException.class, () -> storageService.loadAsResource("..\\secret.txt"));
    }

    @Test
    void whenFileNotFound_thenThrowResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> storageService.loadAsResource("non-existent.png"));
    }

    @Test
    void whenDeleteStoredFile_thenFileRemoved() throws Exception {
        byte[] pngBytes = createTestPngBytes(100, 100);
        MockMultipartFile file = new MockMultipartFile(
                "file", "to-delete.png", "image/png", pngBytes
        );

        StoredFile stored = storageService.store(file, null);
        Resource resource = storageService.loadAsResource(stored.storedFileName());
        assertTrue(resource.exists());

        storageService.delete(stored.storedFileName());
        assertThrows(ResourceNotFoundException.class, () -> storageService.loadAsResource(stored.storedFileName()));
    }
}
