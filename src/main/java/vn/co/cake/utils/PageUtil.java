package vn.co.cake.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * PageUtil
 */
public class PageUtil implements Pageable {

    private int size;
    private Pageable pageable;
    private Integer currentPage;

    public PageUtil(int size, Pageable pageable) {
        this.size = size;
        this.pageable = pageable;
    }
    public PageUtil(int size, Integer currentPage, Pageable pageable){
        this.size = size;
        this.currentPage = currentPage;
        this.pageable = pageable;
    }

    @Override
    public int getPageNumber() {
        if (currentPage == null) return pageable.getPageNumber();
        else return currentPage;
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        if (currentPage == null) return (long) pageable.getPageNumber() * (long) size;
        else return (long) currentPage * (long) size;
    }

    @Override
    public Sort getSort() {
        return pageable.getSort();
    }

    @Override
    public Pageable next() {
        return pageable.next();
    }

    @Override
    public Pageable previousOrFirst() {
        return pageable.previousOrFirst();
    }

    @Override
    public Pageable first() {
        return pageable.first();
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return null;
    }

    @Override
    public boolean hasPrevious() {
        return pageable.hasPrevious();
    }
}
