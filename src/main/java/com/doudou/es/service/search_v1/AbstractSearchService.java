package com.doudou.es.service.search_v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.doudou.es.service.search_v1.common.EntityForSearch;
import com.doudou.es.service.search_v1.common.SearchDate;
import com.doudou.es.service.search_v1.elasticsearch.Document;
import com.doudou.es.service.search_v1.elasticsearch.Elasticsearch;
import com.doudou.es.service.search_v1.elasticsearch.Query;
import com.doudou.es.service.search_v1.elasticsearch.SearchResult;
import com.doudou.es.util.StringParse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.fielddata.FieldData;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author 豆豆
 * @date 2019/7/16 14:36
 * @flag 以万物智能，化百千万亿身
 */
@Slf4j
public abstract class AbstractSearchService {

    @Autowired
    Elasticsearch elasticsearch;

    @Getter
    String index;
    @Getter
    String type;

    public AbstractSearchService(String index, String type){
        this.type = type;
        this.index =index;
    }

    /**
     *
     * @param entityForSearch
     * @param <T>
     */
    public <T extends EntityForSearch> void insert(T entityForSearch) {
        if (entityForSearch == null) return;
        Document document = buildDocument(entityForSearch, entityForSearch.getId());
        elasticsearch.putDocument(document);
    }

    /**
     *
     * @param entityForSearch
     * @param <T>
     */
    public <T extends EntityForSearch> void update(T entityForSearch) {
        if (entityForSearch == null) return;
        Document document = buildDocument(entityForSearch, entityForSearch.getId());
        document.setAction(Document.UPDATE);
        elasticsearch.putDocument(document);
    }

    /**
     *
     * @param id
     * @throws IOException
     */
    public void deleteById(Integer id) throws IOException {
        elasticsearch.deleteDocument(index, type, String.valueOf(id));
    }


    /**
     *
     * @param queryBuilder
     * @param pageNo
     * @param pageSize
     * @return
     * @throws IOException
     */
    public SearchResult search(QueryBuilder queryBuilder, int pageNo, int pageSize) throws IOException {
        Query query = new Query()
                .setIndex(index)
                .setType(type)
                .setQueryBuilder(queryBuilder)
                .setFrom((pageNo - 1) * pageSize)
                .setSize(pageSize);
        SearchResult result = elasticsearch.search(query);
        result.parseDocument();
        return result;
    }

    private void addPrefix(Map<String, Object> map) {
        addPrefix(map, type);
    }

    private void addPrefix(Map<String, Object> map, String prefix) {
        Map<String, Object> m = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            m.put(prefix + "_" + entry.getKey(), entry.getValue());
        }
        map.clear();
        map.putAll(m);
    }

    public Document buildDocument(Object object, Integer id) {
        Map<String, Object> map = JSON.parseObject(JSON.toJSONStringWithDateFormat(object, "yyyy-MM-dd HH:mm:ss"), new TypeReference<Map<String, Object>>(){});
        addPrefix(map);
        Document document = new Document (
                index,
                type,
                String.valueOf(id),
                map
        );
        return document;
    }

    public Map<String, Class> buildFields(int fieldType, LinkedHashMap<String, Class> fieldMap){
        if (0 == fieldType){
            return fieldMap;
        }

        int i = 1;
        Map<String, Class> result = new HashMap<>();
        for (Map.Entry<String, Class> entry : fieldMap.entrySet()){
            if (i == fieldType){
                result.put(entry.getKey(), entry.getValue());
            }
            i++;
        }

        return result;
    }

    /**
     *
     * @param fieldType
     * @param fieldValue
     * @param boolQueryBuilder
     * @param fieldMap
     */
    public void buildFieldQuery(Integer fieldType, String fieldValue, BoolQueryBuilder boolQueryBuilder, Map<String, Class> fieldMap){
        if (fieldType != null && !StringUtils.isEmpty(fieldValue)){
            BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();
            fieldValue = StringParse.convertParam(fieldValue);
            log.debug("fieldValue : " + fieldValue);
            String finalFieldValue = fieldValue;
            fieldMap.forEach((k, v) -> {
                if (v == Integer.class && StringUtils.isNumeric(finalFieldValue)){
                    if (!(new BigDecimal(finalFieldValue).compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)){
                        log.debug(type + "_" + k + " " + finalFieldValue);
                        QueryBuilder columnQuery = QueryBuilders.matchQuery(type + "_" + k, finalFieldValue);
                        fieldBoolQuery.should(columnQuery);
                    }
                }
            });
            boolQueryBuilder.must(fieldBoolQuery);
        }

    }

    public void buildTermQuery(String key, Object value, BoolQueryBuilder boolQueryBuilder, BoolQueryType boolQueryType){
        //no need to check null
        if (value instanceof String){
            String fieldValue = (String) value;
            value = StringParse.convertParam(fieldValue);
            log.debug(key + " : " + value);
            log.debug("type " + type);
            QueryBuilder termquery = QueryBuilders.termQuery(type + "_" + key, value);
            buildQuery(boolQueryBuilder, termquery, boolQueryType);

        }
    }

    public void buildPrefixQuery(String key, String value, BoolQueryBuilder boolQueryBuilder, BoolQueryType boolQueryType){
        log.debug(key + " : " + value);
        log.debug("type " + type);
        QueryBuilder prefixQuery = QueryBuilders.prefixQuery(type + "_" + key, value);
        buildQuery(boolQueryBuilder, prefixQuery, boolQueryType);

    }



    private void buildQuery(BoolQueryBuilder boolQueryBuilder, QueryBuilder query, BoolQueryType boolQueryType){
        switch (boolQueryType){
            case must:
                boolQueryBuilder.must(query);
                break;
            case filter:
                boolQueryBuilder.filter(query);
                break;
            case should:
                boolQueryBuilder.should(query);
                break;
            case mustnot:
                boolQueryBuilder.mustNot(query);
                break;
            default:
                break;
        }
    }

    /**
     *
     * @param id
     * @param size
     * @return
     */
    public abstract List<? extends EntityForSearch> selectAll(Integer id, Integer size);

    /**
     *
     */
    public enum BoolQueryType {
        must, filter, should, mustnot
    }


}
