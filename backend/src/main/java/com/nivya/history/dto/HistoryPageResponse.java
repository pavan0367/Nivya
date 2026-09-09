package com.nivya.history.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated response wrapper for Parent History.
 * Enforces bounded data delivery without loading unlimited records.
 */
public class HistoryPageResponse {

    private List<HistoryEventDto> items = new ArrayList<>();
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    public HistoryPageResponse() {
    }

    public HistoryPageResponse(List<HistoryEventDto> items, int currentPage, int totalPages,
                               long totalElements, int pageSize, boolean hasNext, boolean hasPrevious) {
        this.items = items != null ? items : new ArrayList<>();
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public List<HistoryEventDto> getItems() {
        return items;
    }

    public void setItems(List<HistoryEventDto> items) {
        this.items = items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
