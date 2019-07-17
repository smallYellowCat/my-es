package com.doudou.es.service.search_v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.doudou.es.service.search_v1.common.EntityForSearch;
import com.doudou.es.service.search_v1.elasticsearch.Document;
import com.doudou.es.service.search_v1.elasticsearch.Elasticsearch;
import com.doudou.es.service.search_v1.elasticsearch.Query;
import com.doudou.es.service.search_v1.elasticsearch.SearchResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 豆豆
 * @date 2019/7/16 14:36
 * @flag 以万物智能，化百千万亿身
 */
@Slf4j
public abstract class AbstractSearchService {

    @Autowired
    Elasticsearch elasticsearch;

    @Value("#{configProperties['elasticsearch.index']}")
    String index;
    @Getter
    String type;

    public AbstractSearchService(String type){
        this.type = type;
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
