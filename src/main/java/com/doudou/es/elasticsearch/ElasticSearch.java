package com.doudou.es.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.elasticsearch.index.reindex.BulkByScrollResponse;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;


/**
 * Created by wangqi on 2016/12/8.
 */
@Component
@Slf4j
public class ElasticSearch {
    private static final int MAXIMUM_CAPACITY = 128;

    private int capacity;
    private Node[] table;
    private TransportClient client;
    private IndicesAdminClient indicesAdminClient;

    public ElasticSearch(String clusterName,
                         String clusterNodes,
                         int concurrency) throws UnknownHostException {
        capacity = Math.min(tableSizeFor(concurrency), MAXIMUM_CAPACITY);

        String[] nodes = clusterNodes.split(",");
        TransportAddress[] addresses = new TransportAddress[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            String[] tuple = nodes[i].split(":");
            addresses[i] = new TransportAddress(InetAddress.getByName(tuple[0]), Integer.valueOf(tuple[1]));
        }
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put("client.transport.sniff", true)
                .build();
        client = new PreBuiltTransportClient(settings).addTransportAddresses(addresses);
        indicesAdminClient = client.admin().indices();
    }




    @Autowired
    public ElasticSearch(@Value("#{configProperties['elasticsearch.cluster-name']}") String clusterName,
                         @Value("#{configProperties['elasticsearch.cluster-nodes']}") String clusterNodes) throws UnknownHostException {
        this(clusterName, clusterNodes, MAXIMUM_CAPACITY);
        log.debug(" ====== es host: " + clusterNodes);
    }

    @PostConstruct
    public void prepareIndex() {
        table = new Node[capacity];
        for (int i = 0; i < table.length; i++) {
            table[i] = new Node();
        }
    }

    @PreDestroy
    public void close() {
        if (table != null) {
            for (Node aTable : table) {
                aTable.stop();
            }

            while(true) {
                boolean terminated = true;
                for (Node aTable : table) {
                    if (!aTable.isTerminated()) {
                        terminated = false;
                    }
                }
                if (terminated) {
                    break;
                }
            }
        }

        client.close();
    }


    private int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }

    private int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    private int indexFor(int h, int length) {
        return h & (length - 1);
    }

    public void refresh(String index) {
        client.admin().indices().prepareRefresh(index).get();
    }

    /**
     * 向ES中添加文档
     * @param document
     * @throws InterruptedException
     */
    public void putDocument(Document document) {
        if (document == null) {
            return;
        }

        int hash = hash(document.getId().hashCode());
        int i = indexFor(hash, table.length);
        table[i].addIndex(document);
    }

    /**
     * 获取文档
     * @param index
     * @param type
     * @param id
     * @return
     */
    public Document getDocument(String index, String type, String id) {
        GetResponse response = client.prepareGet(index, type, id).get();
        Document idx = new Document();
        idx.setIndex(response.getIndex());
        idx.setType(response.getType());
        idx.setId(response.getId());
        idx.setVersion(response.getVersion());
        idx.setExists(response.isExists());
        idx.setSource(response.getSourceAsMap());
        return idx;
    }

    /**
     * 删除文档
     * @param index
     * @param type
     * @param id
     */
    public void deleteDocument(String index, String type, String id) {
        DeleteResponse response = client.prepareDelete(index, type, id).get();
//        System.out.println(response.toString());
    }

    /**
     * 插入文档
     * @param document
     */
    public void insertDocument(Document document) {
        try {
            client.prepareIndex(document.getIndex(), document.getType(), document.getId())
                    .setSource(document.getSource()).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                    .get(new TimeValue(5, TimeUnit.SECONDS));
        } catch (ElasticsearchTimeoutException e) {
            System.out.println("out of time");
        } catch (Exception e) {
            System.out.println("insert elasticsearch exception: " + e);
        }
    }

    /**
     * 更新文档
     * @param document
     */
    public void updateDocument(Document document) {
        System.out.println("======updateDocument=====");
        try {
            client.prepareUpdate(document.getIndex(), document.getType(), document.getId())
                    .setDoc(document.getSource()).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                    .get(new TimeValue(5, TimeUnit.SECONDS));
        } catch (ElasticsearchTimeoutException e) {
            System.out.println("out of time");
        } catch (Exception e) {
            System.out.println("insert elasticsearch exception: " + e);
        }
    }


    /**
     * 根据查询结果删除文档
     * @param index
     * @param qb
     */
    public void deleteByQuery(String index, QueryBuilder qb) {
        BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .filter(qb)
                .source(index)
                .get();


        long deleted = response.getDeleted();
        System.out.println(deleted);

    }

    /**
     * 根据alias获取index
     * @param alias
     * @return
     */
    public List<String> getIndexByAlias(String alias) {
        ActionFuture<GetAliasesResponse> actionFuture = indicesAdminClient.getAliases(new GetAliasesRequest().aliases(alias));
        GetAliasesResponse response = actionFuture.actionGet();
        ImmutableOpenMap<String, List<AliasMetaData>> map = response.getAliases();

        Iterator<String> keysIt = map.keysIt();
        List<String> indices = new ArrayList<>();

        while (keysIt.hasNext()) {
            String key = keysIt.next();
            indices.add(key);
        }
        return indices;
    }

    /**
     * 获取所有的index
     */
    public List<String> getIndices() {
        GetIndexResponse response = indicesAdminClient.getIndex(new GetIndexRequest()).actionGet();
        return Arrays.asList(response.getIndices());
    }

    /**
     * 创建索引
     * @param index
     */
    public void createIndex(String index) {
        indicesAdminClient.prepareCreate(index).get();
    }


    /**
     * 创建索引，加入指定的mapping
     * @param index
     * @param mapping
     */
    public void createIndex(String index, String mapping) throws IOException {
        System.out.println(mapping);
        CreateIndexRequestBuilder indexRequestBuilder = indicesAdminClient.prepareCreate(index)
                .setSource(mapping,XContentType.JSON);

        indexRequestBuilder.get();
    }





    /**
     * 增加mapping
     * @param index
     * @param type
     * @param mapping
     */
    public void putMapping(String index, String type, String mapping) {
        indicesAdminClient.preparePutMapping(index)
                .setType(type)
                .setSource(mapping)
                .get();
    }

    /**
     * 创建索引以及别名，加入指定的mapping
     * @param alias
     * @param index
     * @param mapping
     */
    public void createIndex(String alias, String index, String mapping) throws IOException {
        createIndex(index, mapping);
        createAlias(alias, index);
    }

    /**
     * 创建别名
     * @param alias
     * @param index
     */
    public void createAlias(String alias, String index) {
        indicesAdminClient.prepareAliases().addAlias(index, alias).execute().actionGet();
    }

    /**
     * 删除别名
     * @param alias
     * @param indices
     */
    public void deleteAlias(String alias, String[] indices) {
        if (indices.length > 0)
            indicesAdminClient.prepareAliases().removeAlias(indices, alias).execute().actionGet();
    }

    /**
     * 创建index以及mapping
     * @param alias
     * @param mapping
     */
    public void createIndexMapping(String alias, String mapping) throws IOException {
        String index = alias + "_" + getDateString();
        List<String> indices = getIndexByAlias(alias);

        // 创建index
        createIndex(index, mapping);
        // 删除alias下老的index
        deleteAlias(alias, indices.toArray(new String[0]));
        // 增加alias
        createAlias(alias, index);

    }


    /**
     * 删除索引
     * @param index
     */
    public void deleteIndex(String index) {
        indicesAdminClient.prepareDelete(index).get();
    }

    private String getDateString() {
        LocalDateTime time = LocalDateTime.now();
        return time.format(DateTimeFormatter.ofPattern("yyyMMddHHmmss"));
    }

    private List<Document> responseToDocuments(SearchResponse response) {
        List<Document> documents = new ArrayList<>();

        for (SearchHit hit : response.getHits()) {
            Document document = new Document();
            document.setId(hit.getId());
            document.setIndex(hit.getIndex());
            document.setType(hit.getType());
            document.setExists(true);
            document.setVersion(hit.getVersion());
            document.setSource(hit.getSourceAsMap());
            document.setScore(hit.getScore());
            document.setHighlightFields(hit.getHighlightFields());
            documents.add(document);
        }
        return documents;
    }

    /**
     * 分页查询
     * @param index
     * @param type
     * @param qb
     * @param from
     * @param size
     * @return
     */
    public SearchResult searchWithPage(String index, String type, QueryBuilder qb, HighlightBuilder highlightBuilder, int from, int size) {
        SearchResponse response = client.prepareSearch(index)
                .setTypes(type)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb)
                .highlighter(highlightBuilder)
                .setFrom(from)
                .setSize(size)
                .get();

        System.out.println("hits: " + response.getHits().getHits().length);
        System.out.println("totalHits: " + response.getHits().getTotalHits());

        List<Document> documents = responseToDocuments(response);
//        List<Document> documents = new ArrayList<>();
//
//        for (SearchHit hit : response.getHits()) {
//            Document document = new Document();
//            document.setId(hit.getId());
//            document.setIndex(hit.getIndex());
//            document.setExists(true);
//            document.setVersion(hit.getVersion());
//            document.setSource(hit.getSource());
//            document.setScore(hit.getScore());
//            document.setHighlightFields(hit.getHighlightFields());
//            System.out.println(hit.getHighlightFields());
//            documents.add(document);
//        }
//        Map<String, Object> result = new HashMap<>();
//        result.put("indices", indices);
//        result.put("hits", response.getHits().getHits().length);
//        result.put("totalHits", response.getHits().getTotalHits());
        SearchResult result = new SearchResult();
        result.setHits(response.getHits().getHits().length);
        result.setTotalHits(response.getHits().getTotalHits());
        result.setDocuments(documents);
        return result;
    }

    /**
     * 搜索
     * @param query
     * @return
     */
    public SearchResult search(Query query) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch();
        if (query.getIndex() != null) searchRequestBuilder.setIndices(query.getIndex());
        if (query.getType() != null) searchRequestBuilder.setTypes(query.getType());
        if (query.getQueryBuilder() != null) searchRequestBuilder.setQuery(query.getQueryBuilder());
        if (query.getHighlightBuilder() != null) searchRequestBuilder.highlighter(query.getHighlightBuilder());
        if (query.getFrom() != null) searchRequestBuilder.setFrom(query.getFrom());
        if (query.getSize() != null) searchRequestBuilder.setSize(query.getSize());

        searchRequestBuilder.setPreference("_replica_first");

        SearchResponse response = searchRequestBuilder.get();

        System.out.println("hits: " + response.getHits().getHits().length);
        System.out.println("totalHits: " + response.getHits().getTotalHits());

        List<Document> documents = responseToDocuments(response);
//        List<Document> documents = new ArrayList<>();
//
//        for (SearchHit hit : response.getHits()) {
//            Document document = new Document();
//
////            System.out.println(hit.getId());
////            System.out.println(hit.getScore());
////            System.out.println(hit.getSourceAsString());
//
//            document.setId(hit.getId());
//            document.setIndex(hit.getIndex());
//            document.setExists(true);
//            document.setVersion(hit.getVersion());
//            document.setSource(hit.getSource());
//            document.setScore(hit.getScore());
//            document.setHighlightFields(hit.getHighlightFields());
//            documents.add(document);
//        }

        SearchResult result = new SearchResult();
        result.setHits(response.getHits().getHits().length);
        result.setTotalHits(response.getHits().getTotalHits());
        result.setDocuments(documents);
        return result;
    }
    class Node {

        private ExecutorService pool;

//        public Node() {
//            pool = Executors.newSingleThreadExecutor();
//        }
        public Node() {
            pool = new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            if (!executor.isShutdown()) {
                                try {
                                    executor.getQueue().put(r);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
        }

        public void addIndex(Document document) {
            Worker worker = new Worker(document);
            pool.execute(worker);
        }

        public void stop() {
            pool.shutdown();
        }

        public boolean isTerminated() {
            return pool.isTerminated();
        }

        class Worker extends Thread {
            private Document document;
            public Worker(Document document) {
                this.document = document;
            }
            @Override
            public void run() {
                if (document != null) {
//                  System.out.println(document.getAction());
                    log.info(Thread.currentThread().getName() + " id: " + document.getId() + " type: " + document.getType());
                    try {
                        if (document.getAction() == Document.INDEX) {
                            client.prepareIndex(document.getIndex(), document.getType(), document.getId())
                                    .setSource(document.getSource())
                                    .get(new TimeValue(5, TimeUnit.SECONDS));
                        } else if (document.getAction() == Document.UPDATE) {
                            client.prepareUpdate(document.getIndex(), document.getType(), document.getId())
                                    .setDoc(document.getSource()).setUpsert(document.getSource())
                                    .get(new TimeValue(5, TimeUnit.SECONDS));
                        }
                    } catch (ElasticsearchTimeoutException e) {
                        log.error("out of time");
                    } catch (Exception e) {
                        log.error("insert elasticsearch exception: " + e);
                    }
                }
            }
        }

    }


}
