package vn.co.cake.controller.AM003;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import vn.co.cake.common.BaseConst;
import vn.co.cake.enums.Authorities;
import vn.co.cake.repository.CategoryRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.request.ProductCreateRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.ProductService;
import vn.co.cake.service.VariationService;
import vn.co.cake.service.aws.S3Service;

/**
 * Characterization tests for the image-upload blocks of {@link AM003Controller#createProduct} and
 * {@link AM003Controller#updateProduct}: upload order, the URL each setter receives, skipped files and
 * exception propagation.
 */
class AM003ControllerUploadTest {

    private ProductService productService;
    private S3Service s3Service;
    private AM003Controller controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws IOException {
        productService = mock(ProductService.class);
        s3Service = mock(S3Service.class);
        controller = new AM003Controller(mock(AccountService.class), productService, s3Service,
                mock(CategoryRepository.class), mock(VariationRepository.class), mock(VoucherRepository.class),
                mock(VariationService.class));
        session = new MockHttpSession();
        AdminLoginInfo admin = mock(AdminLoginInfo.class);
        when(admin.getAuthority()).thenReturn(Authorities.ROLE_ADMIN);
        session.setAttribute(BaseConst.ADMIN_SESSION, admin);
        when(s3Service.uploadImageToS3(any())).thenAnswer(inv -> "url-" + ((MultipartFile) inv.getArgument(0)).getName());
    }

    private static MultipartFile file(String name) {
        return new MockMultipartFile(name, name + ".png", "image/png", new byte[] {1, 2, 3});
    }

    private static MultipartFile emptyFile(String name) {
        return new MockMultipartFile(name, name + ".png", "image/png", new byte[0]);
    }

    private static MultipartFile[] allFiles() {
        MultipartFile[] files = new MultipartFile[10];
        for (int i = 0; i < files.length; i++) {
            files[i] = file("f" + i);
        }
        return files;
    }

    private ResponseEntity<String> create(MultipartFile[] f) {
        return controller.createProduct("Name", "CODE", null, null, null, null, null, null, "100000", "10", 5L,
                Collections.singletonList("1"), null, null, null, null,
                f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9],
                new MockHttpServletResponse(), session);
    }

    private ResponseEntity<String> update(MultipartFile[] f) {
        return controller.updateProduct(7L, "Name", "CODE", null, null, null, null, null, null, "100000", "10", 5L,
                Collections.singletonList("1"), null, null, null, null,
                f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9],
                new MockHttpServletResponse(), session);
    }

    private static void assertAllUrls(ProductCreateRequest r) {
        assertEquals("url-f0", r.getImageUrl());
        assertEquals("url-f1", r.getImage1Url());
        assertEquals("url-f2", r.getImage2Url());
        assertEquals("url-f3", r.getImage3Url());
        assertEquals("url-f4", r.getImage4Url());
        assertEquals("url-f5", r.getImage5Url());
        assertEquals("url-f6", r.getImage6Url());
        assertEquals("url-f7", r.getImage7Url());
        assertEquals("url-f8", r.getImage8Url());
        assertEquals("url-f9", r.getImage9Url());
    }

    private void assertUploadOrder(MultipartFile[] files) throws IOException {
        InOrder order = inOrder(s3Service);
        for (MultipartFile f : files) {
            order.verify(s3Service).uploadImageToS3(f);
        }
    }

    @Test
    void create_uploadsAllTenFilesInOrderAndSetsEachUrl() throws Exception {
        MultipartFile[] files = allFiles();

        ResponseEntity<String> result = create(files);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertUploadOrder(files);
        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).create(captor.capture());
        assertAllUrls(captor.getValue());
    }

    @Test
    void update_uploadsAllTenFilesInOrderAndSetsEachUrl() throws Exception {
        MultipartFile[] files = allFiles();

        ResponseEntity<String> result = update(files);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertUploadOrder(files);
        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).update(captor.capture());
        assertEquals(7L, captor.getValue().getId());
        assertAllUrls(captor.getValue());
    }

    @Test
    void create_skipsNullAndEmptyFiles() throws Exception {
        MultipartFile[] files = allFiles();
        files[1] = null;
        files[2] = emptyFile("f2");
        files[9] = null;

        create(files);

        verify(s3Service, times(7)).uploadImageToS3(any());
        verify(s3Service, never()).uploadImageToS3(files[2]);
        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).create(captor.capture());
        ProductCreateRequest r = captor.getValue();
        assertEquals("url-f0", r.getImageUrl());
        assertNull(r.getImage1Url());
        assertNull(r.getImage2Url());
        assertEquals("url-f3", r.getImage3Url());
        assertEquals("url-f8", r.getImage8Url());
        assertNull(r.getImage9Url());
    }

    @Test
    void update_skipsNullAndEmptyFiles() throws Exception {
        MultipartFile[] files = new MultipartFile[10];
        files[0] = emptyFile("f0");
        files[5] = file("f5");

        update(files);

        verify(s3Service, times(1)).uploadImageToS3(any());
        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).update(captor.capture());
        ProductCreateRequest r = captor.getValue();
        assertNull(r.getImageUrl());
        assertEquals("url-f5", r.getImage5Url());
        assertNull(r.getImage6Url());
    }

    @Test
    void create_uploadFailureStopsLaterUploadsAndReturnsItsMessage() throws Exception {
        MultipartFile[] files = allFiles();
        when(s3Service.uploadImageToS3(files[2])).thenThrow(new IOException("disk full"));

        ResponseEntity<String> result = create(files);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("disk full", result.getBody());
        verify(s3Service, times(3)).uploadImageToS3(any());
        verify(s3Service, never()).uploadImageToS3(files[3]);
        verify(productService, never()).create(any());
    }

    @Test
    void update_uploadFailureStopsLaterUploadsAndReturnsGenericMessage() throws Exception {
        MultipartFile[] files = allFiles();
        when(s3Service.uploadImageToS3(files[9])).thenThrow(new IOException("disk full"));

        ResponseEntity<String> result = update(files);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        // same bytes as the AM003Controller literal, which is stored in decomposed Unicode
        assertEquals("C\u00e2\u0323p Nh\u00e2\u0323p Sa\u0309n Ph\u00e2\u0309m \u0110a\u0303 Th\u00e2\u0301t Ba\u0323i!", result.getBody());
        verify(s3Service, times(10)).uploadImageToS3(any());
        verify(productService, never()).update(any());
    }
}
