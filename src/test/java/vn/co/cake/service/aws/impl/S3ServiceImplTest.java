package vn.co.cake.service.aws.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class S3ServiceImplTest {

    @Test
    void safeFileName_keepsOriginalName() {
        assertEquals("Áo khoác đen.JPG", S3ServiceImpl.safeFileName("Áo khoác đen.JPG"));
    }

    @Test
    void safeFileName_stripsDirectoriesAndTraversal() {
        assertEquals("evil.jpg", S3ServiceImpl.safeFileName("../../windows/evil.jpg"));
        assertEquals("evil.jpg", S3ServiceImpl.safeFileName("..\\..\\evil.jpg"));
        assertEquals("a_b.jpg", S3ServiceImpl.safeFileName("a:b.jpg"));
        assertEquals("image", S3ServiceImpl.safeFileName(".."));
        assertEquals("image", S3ServiceImpl.safeFileName(null));
    }
}
