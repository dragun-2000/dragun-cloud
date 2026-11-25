package vn.co.cake.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.co.cake.entity.Product;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.service.external.PancakePosService;

/** AM008: cập nhật % giảm giá hàng loạt, gồm nhập 0 để bỏ giảm giá. */
class ProductDiscountUpdateTest {

    private static final List<Long> IDS = Collections.singletonList(1L);

    private ProductRepository productRepository;
    private ProductServiceImpl service;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        service = new ProductServiceImpl(productRepository, mock(PancakePosService.class),
                mock(VariationRepository.class));
        product = new Product();
        product.setPrice(new BigDecimal("200000"));
        product.setDiscount(new BigDecimal("20"));
        product.setDiscountPrice(new BigDecimal("40000"));
        product.setFinalPrice(new BigDecimal("160000"));
        when(productRepository.findAllByIdIn(IDS)).thenReturn(Collections.singletonList(product));
    }

    @Test
    void zeroPercent_removesDiscount() throws CommonServletException {
        service.updateProductsDiscount(IDS, BigDecimal.ZERO);

        assertEquals(0, product.getDiscount().compareTo(BigDecimal.ZERO));
        assertEquals(0, product.getDiscountPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, product.getFinalPrice().compareTo(new BigDecimal("200000")));
        verify(productRepository).saveAll(anyList());
    }

    @Test
    void positivePercent_stillApplies() throws CommonServletException {
        service.updateProductsDiscount(IDS, new BigDecimal("10"));

        assertEquals(0, product.getDiscount().compareTo(new BigDecimal("10")));
        assertEquals(0, product.getFinalPrice().compareTo(new BigDecimal("180000")));
    }

    @Test
    void ninetyNine_isTheMaximum() throws CommonServletException {
        service.updateProductsDiscount(IDS, new BigDecimal("99"));

        assertEquals(0, product.getDiscount().compareTo(new BigDecimal("99")));
    }

    @Test
    void wholeNumberWrittenWithZeroDecimals_isAccepted() throws CommonServletException {
        service.updateProductsDiscount(IDS, new BigDecimal("15.00"));

        assertEquals(0, product.getDiscount().compareTo(new BigDecimal("15")));
    }

    @Test
    void outOfRangeOrDecimalOrMissing_isRejected() {
        assertThrows(CommonServletException.class,
                () -> service.updateProductsDiscount(IDS, new BigDecimal("-1")));
        assertThrows(CommonServletException.class,
                () -> service.updateProductsDiscount(IDS, new BigDecimal("100")));
        assertThrows(CommonServletException.class,
                () -> service.updateProductsDiscount(IDS, new BigDecimal("10.5")));
        assertThrows(CommonServletException.class,
                () -> service.updateProductsDiscount(IDS, new BigDecimal("0.01")));
        assertThrows(CommonServletException.class, () -> service.updateProductsDiscount(IDS, null));
        verify(productRepository, never()).saveAll(any());
    }
}
