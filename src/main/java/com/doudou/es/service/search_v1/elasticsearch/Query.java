package com.doudou.es.service.search_v1.elasticsearch;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

/**
 * @author 豆豆
 * @date 2019/7/15 16:13
 * @flag 以万物智能，化百千万亿身
 */
@Setter
@Getter
@Accessors(chain = true)
public class Query {
    private String index;
    private String type;
    private QueryBuilder queryBuilder;
    private HighlightBuilder highlightBuilder;
    private Integer from;
    private Integer size;
}
