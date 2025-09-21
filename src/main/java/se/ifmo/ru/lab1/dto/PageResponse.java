package se.ifmo.ru.lab1.dto;

import java.util.List;

public class PageResponse<T> {
    
    private List<T> content;
    private PageMetadata page;
    
    public PageResponse(List<T> content, long totalElements, int pageNumber, int pageSize) {
        this.content = content;
        this.page = new PageMetadata(totalElements, pageNumber, pageSize);
    }
    
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public PageMetadata getPage() {
        return page;
    }
    
    public void setPage(PageMetadata page) {
        this.page = page;
    }
    
    public static class PageMetadata {
        private long totalElements;
        private int totalPages;
        private int pageNumber;
        private int pageSize;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;
        
        public PageMetadata(long totalElements, int pageNumber, int pageSize) {
            this.totalElements = totalElements;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
            this.first = pageNumber == 0;
            this.last = pageNumber >= totalPages - 1;
            this.hasNext = !last;
            this.hasPrevious = !first;
        }
        
        // Getters
        public long getTotalElements() {
            return totalElements;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public int getPageNumber() {
            return pageNumber;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public boolean isFirst() {
            return first;
        }
        
        public boolean isLast() {
            return last;
        }
        
        public boolean isHasNext() {
            return hasNext;
        }
        
        public boolean isHasPrevious() {
            return hasPrevious;
        }
    }
}
