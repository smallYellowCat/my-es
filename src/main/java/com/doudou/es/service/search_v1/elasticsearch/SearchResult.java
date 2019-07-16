package com.doudou.es.service.search_v1.elasticsearch;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 豆豆
 * @date 2019/7/15 16:17
 * @flag 以万物智能，化百千万亿身
 */
@Setter
@Getter
@Accessors(chain = true)
@NoArgsConstructor
public class SearchResult {
    private int hits;
    private long totalHits;
    private List<Document> documents = new ArrayList<>();

    public static final int MAX_PAGE = 100;

    public SearchResult(int hits, long totalHits){
        this.hits = hits;
        this.totalHits = totalHits;
    }

    public boolean addDocument(Document document){
        return this.documents.add(document);
    }

    public void parseDocument(){
        for (Document document : documents){
            document.removePrefix();
        }
    }
}
