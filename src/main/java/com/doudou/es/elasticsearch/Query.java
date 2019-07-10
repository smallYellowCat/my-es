package com.doudou.es.elasticsearch;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

/**
 * Created by wangqi on 2017/4/18.
 */
public class Query {
    private String index;
    private String type;
    private QueryBuilder queryBuilder;
    private HighlightBuilder highlightBuilder;
    private Integer from;
    private Integer size;

    public Query setIndex(String index) {
        this.index = index;
        return this;
    }

    public String getIndex() {
        return index;
    }

    public Query setType(String type) {
        this.type = type;
        return this;
    }

    public String getType() {
        return type;
    }

    public QueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    public Query setQueryBuilder(QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        return this;
    }

    public HighlightBuilder getHighlightBuilder() {
        return highlightBuilder;
    }

    public Query setHighlightBuilder(HighlightBuilder highlightBuilder) {
        this.highlightBuilder = highlightBuilder;
        return this;
    }

    public Integer getFrom() {
        return from;
    }

    public Query setFrom(Integer from) {
        this.from = from;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public Query setSize(Integer size) {
        this.size = size;
        return this;
    }
}
