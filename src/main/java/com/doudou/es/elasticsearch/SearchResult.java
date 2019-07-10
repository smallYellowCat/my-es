package com.doudou.es.elasticsearch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangqi on 2017/3/20.
 */
public class SearchResult {
    private int hits;
    private long totalHits;
    private List<Document> documents = new ArrayList<>();

    public static final int MAX_PAGE = 100;

    public SearchResult() {
    }

    public SearchResult(int hits, long totalHits) {
        this.hits = hits;
        this.totalHits = totalHits;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public void addDocument(Document document) {
        this.documents.add(document);
    }

    public void parseDocuments() {
        for (Document document : documents) {
            document.removePrefix();
        }
    }
}
