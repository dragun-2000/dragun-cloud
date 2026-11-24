package vn.co.cake.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Characterization tests for {@link PageUtil}.
 */
class PageUtilTest {

    private static final Sort SORT = Sort.by("name");

    @Test
    void withoutCurrentPageUsesDelegatePageNumberAndOwnSize() {
        PageUtil page = new PageUtil(5, PageRequest.of(2, 10, SORT));

        assertEquals(2, page.getPageNumber());
        assertEquals(5, page.getPageSize());
        assertEquals(10L, page.getOffset());
        assertEquals(SORT, page.getSort());
        assertTrue(page.hasPrevious());
    }

    @Test
    void currentPageOverridesDelegatePageNumber() {
        PageUtil page = new PageUtil(5, 3, PageRequest.of(1, 10));

        assertEquals(3, page.getPageNumber());
        assertEquals(15L, page.getOffset());
    }

    @Test
    void navigationDelegatesToWrappedPageableIgnoringOwnSize() {
        PageUtil page = new PageUtil(5, 3, PageRequest.of(1, 10));

        // current behavior: next/previous/first come from the wrapped pageable (size 10, page 1)
        Pageable next = page.next();
        assertEquals(2, next.getPageNumber());
        assertEquals(10, next.getPageSize());
        assertEquals(0, page.previousOrFirst().getPageNumber());
        assertEquals(0, page.first().getPageNumber());
        // current behavior: withPage always returns null
        assertNull(page.withPage(4));
    }
}
