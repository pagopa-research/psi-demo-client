package it.lockless.psidemoclient.dto;

import java.util.Set;

public class PsiServerDatasetPageDTO {

    private Integer page;

    private Integer size;

    private Integer entries;

    private Boolean last;

    private Integer totalPages;

    private Integer totalEntries;

    private Set<String> content;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getEntries() {
        return entries;
    }

    public void setEntries(Integer entries) {
        this.entries = entries;
    }

    public Boolean isLast() {
        return last;
    }

    public void setLast(Boolean last) {
        this.last = last;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(Integer totalEntries) {
        this.totalEntries = totalEntries;
    }

    public Boolean getLast() {
        return last;
    }

    public Set<String> getContent() {
        return content;
    }

    public void setContent(Set<String> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ServerDatasetPageDTO{" +
                "page=" + page +
                ", size=" + size +
                ", entries=" + entries +
                ", last=" + last +
                ", totalPages=" + totalPages +
                ", totalEntries=" + totalEntries +
                ", content=" + content +
                '}';
    }
}
