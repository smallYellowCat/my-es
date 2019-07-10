package com.doudou.es.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.doudou.es.common.*;
import com.doudou.es.elasticsearch.Document;
import com.doudou.es.elasticsearch.ElasticSearch;
import com.doudou.es.elasticsearch.Query;
import com.doudou.es.elasticsearch.SearchResult;
import com.doudou.es.pagehelper.PageContent;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wangqi on 2017/2/28.
 */
@Slf4j
public abstract class AbstractSearchService {
    @Autowired
    ElasticSearch elasticSearch;

    @Value("#{configProperties['elasticsearch.index']}")
    String index;
    String type;

    public AbstractSearchService(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * 根据queryBuilder的通用搜索
     * @param queryBuilder
     * @param pageNo
     * @param pageSize
     * @return
     */
    public SearchResult search(QueryBuilder queryBuilder, int pageNo, int pageSize) {
        Query query = new Query()
                .setIndex(index)
                .setType(type)
                .setQueryBuilder(queryBuilder)
                .setFrom((pageNo - 1) * pageSize)
                .setSize(pageSize);
        SearchResult result = elasticSearch.search(query);
        result.parseDocuments();
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

    public abstract List<? extends EntityForSearch> selectAll(Integer id, Integer size);

    class Sync {
        ExecutorService pool = Executors.newCachedThreadPool();
        volatile Integer id = 0;
        Integer size = 10;

        Integer totalSize = 0;
        ReentrantLock lock = new ReentrantLock();

        class SyncThread implements Runnable {
            @Override
            public void run() {
                List<? extends EntityForSearch> list;
                lock.lock();
                try {
                    list = selectAll(id, size);
                    if (list.size() <= 0) {
                        pool.shutdown();
                        return;
                    }
                    totalSize += list.size();
                    id = list.get(list.size() - 1).getId();
                } finally {
                    lock.unlock();
                }
                pool.execute(new SyncThread());
                for (EntityForSearch entityForSearch : list) {
                    Document document = buildDocument(entityForSearch, entityForSearch.getId());
                    elasticSearch.putDocument(document);
                }
            }
        }

        public void run() {
            new Thread(new SyncThread()).start();
            while (!pool.isTerminated()){}
            System.out.println("totalSize: " + totalSize);
        }

    }

    public void sync() {
//        Integer id = 0;
//        Integer size = 10;
//        while (true) {
//            List<? extends EntityForSearch> list = selectAll(id, size);
//            log.debug("list size: " + list.size());
//            if (list.size() <= 0) break;
//            for (EntityForSearch entityForSearch : list) {
//                Document document = buildDocument(entityForSearch, entityForSearch.getId());
//                elasticSearch.putDocument(document);
//            }
//            id = list.get(list.size() - 1).getId();
//        }

        new Sync().run();
    }

    public <T extends EntityForSearch> void insert(T entityForSearch) {
        if (entityForSearch == null) return;
        Document document = buildDocument(entityForSearch, entityForSearch.getId());
        elasticSearch.putDocument(document);
    }

    public <T extends EntityForSearch> void update(T entityForSearch) {
        if (entityForSearch == null) return;
        Document document = buildDocument(entityForSearch, entityForSearch.getId());
        document.setAction(Document.UPDATE);
        elasticSearch.putDocument(document);
    }

    public void deleteById(Integer id) {
        elasticSearch.deleteDocument(index, type, String.valueOf(id));
    }

    public enum BoolQueryType {
        must, filter, should, mustnot
    }

    public Map<String, Class> buildFields(Integer fieldType, LinkedHashMap<String, Class> fieldMap) {
        if (fieldType == 0)
            return fieldMap;

        int i = 1;
        Map<String, Class> result = new HashMap<>();
        for (Map.Entry<String, Class> entry : fieldMap.entrySet()) {
            if (i == fieldType) {
                result.put(entry.getKey(), entry.getValue());
            }
            i++;
        }
        return result;
    }

    public void buildFieldQuery(Integer fieldType, String fieldValue, BoolQueryBuilder boolQuery, Map<String, Class> fieldMap) {
        if (fieldType != null && !StringUtils.isEmpty(fieldValue)) {

            BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();
            try {
                if(fieldValue.equals(new String(fieldValue.getBytes("iso8859-1"), "iso8859-1")))
                    fieldValue = StringParse.convertGetParam(fieldValue);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            log.debug("fieldValue: " + fieldValue);

            for (Map.Entry<String, Class> entry : fieldMap.entrySet()) {
                String field = entry.getKey();
                Class fieldClass = entry.getValue();

                if (fieldClass == Integer.class) {
                    if (!org.apache.commons.lang3.StringUtils.isNumeric(fieldValue)) continue;
                    if (new BigDecimal(fieldValue).compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) continue;
                }

//                if ((field.equals("id") || field.equals("articleId")) && (!TrsServerUtil.isNumeric(fieldValue) || new BigDecimal(fieldValue).compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)) continue;
                log.debug(type + "_" + field + " " + fieldValue);
                QueryBuilder columnQuery = QueryBuilders.matchQuery(type + "_" + field, fieldValue);
                fieldBoolQuery.should(columnQuery);
            }
            boolQuery.must(fieldBoolQuery);
        }
    }

    public static abstract class PageResultHandler {
        public void parseDate(Map<String, Object> map, String key) {
            parseDate(map, key, "yyyy-MM-dd HH:mm:ss");
        }

        public void parseDate(Map<String, Object> map, String key, String format) {
            if (map.containsKey(key)) {
                map.replace(key, DateUtils.getTimeStamp((String) map.get(key), format));
            }
        }

        public abstract Object documentHandle(Document document);
    }

    public PageContent buildPageResult(SearchResult searchResult, int pageNo, int pageSize) {
        return buildPageResult(searchResult, pageNo, pageSize, null);
    }

    public PageContent buildPageResult(SearchResult searchResult, int pageNo, int pageSize, PageResultHandler pageResultHandler) {
        List<Document> documents = searchResult.getDocuments();
        List<Object> sourceList = new ArrayList<>();
        for (Document document : documents) {
            Object result;
            if (pageResultHandler == null)
                result = document.getSourceAsMap();
            else
                result = pageResultHandler.documentHandle(document);
            sourceList.add(result);
        }

        Integer hits = searchResult.getHits();
        Long totalHits = searchResult.getTotalHits();

        int totalPage = (int) Math.ceil(totalHits/(pageSize*1.0));
        totalPage = totalPage < SearchResult.MAX_PAGE ? totalPage : SearchResult.MAX_PAGE;
        int totalCount = totalHits.intValue();
        PageContent pageContent = new PageContent(sourceList, pageNo, pageSize, totalCount, totalPage);
        return pageContent;
    }

    public void buildTermQuery(String key, Object value, BoolQueryBuilder boolQuery, BoolQueryType boolQueryType) {
        if (value != null) {
            if (value instanceof String) {
                try {
                    String fieldValue = (String) value;
                    if (fieldValue.equals(new String(fieldValue.getBytes("iso8859-1"), "iso8859-1"))) {
                        value = StringParse.convertGetParam(fieldValue);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            log.debug(key + " : " + value);
            log.debug("type " + type);
            QueryBuilder termQuery = QueryBuilders.termQuery(type + "_" + key, value);
            switch (boolQueryType) {
                case must:
                    boolQuery.must(termQuery);
                    break;
                case filter:
                    boolQuery.filter(termQuery);
                    break;
                case should:
                    boolQuery.should(termQuery);
                    break;
                case mustnot:
                    boolQuery.mustNot(termQuery);
                    break;
            }
        }
    }

    public void buildPrefixQuery(String key, String value, BoolQueryBuilder boolQuery, BoolQueryType boolQueryType){
        log.debug(key + " : " + value);
        log.debug("type " + type);
        QueryBuilder prefixQuery = QueryBuilders.prefixQuery(type + "_" + key, value);
        switch (boolQueryType) {
            case must:
                boolQuery.must(prefixQuery);
                break;
            case filter:
                boolQuery.filter(prefixQuery);
                break;
            case should:
                boolQuery.should(prefixQuery);
                break;
            case mustnot:
                boolQuery.mustNot(prefixQuery);
                break;
        }
    }

    public void buildTimeQuery(String key, Long startTime, Long endTime, Integer selectDay, BoolQueryBuilder boolQuery) {
        buildTimeQuery(key, startTime, endTime, selectDay, boolQuery, "yyyy-MM-dd HH:mm:ss");
    }

    public void buildTimeQuery(String key, Long startTime, Long endTime, Integer selectDay, BoolQueryBuilder boolQuery, String format) {
        SearchDate searchDate = new SearchDate();
        WebTimeUtil.convertSelectTime(searchDate, startTime, endTime, selectDay);
        Date startDate = searchDate.getStartTime(), endDate = searchDate.getEndTime();
        if (startDate != null && endDate != null) {
//			startDate = DateUtils.dateStart(startDate);
//			endDate = DateUtils.dateEnd(endDate);
            log.debug("startDate: " + DateUtils.getDateString(startDate, format));
            log.debug("endDate: " + DateUtils.getDateString(endDate, format));
            RangeQueryBuilder dateRangeQuery = QueryBuilders.rangeQuery(type + "_" + key)
                    .gte(DateUtils.getDateString(startDate, format))
                    .lte(DateUtils.getDateString(endDate, format));
            boolQuery.filter(dateRangeQuery);
        }
    }
}
