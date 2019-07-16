package com.doudou.es.service.search_v1.elasticsearch;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author 豆豆
 * @date 2019/7/13 17:43
 * @flag 以万物智能，化百千万亿身
 */
@Component
@Slf4j
@NoArgsConstructor
public class Elasticsearch {
    private static final int MAXIMUM_CAPACITY = 128;

    private int capacity;
    private Node[] table;
    private RestHighLevelClient client;
    private IndicesClient indicesClient;

    public Elasticsearch(String clusterName, String clusterNodes, int concurrency){
        capacity = tableSizeFor(concurrency);

        String[] nodes = clusterNodes.split(",");

        HttpHost[] hosts = new HttpHost[nodes.length];
        for (int i = 0; i < nodes.length; i++){
            String[] tuple = nodes[i].split(":");
            hosts[i] = new HttpHost(tuple[0], Integer.valueOf(tuple[1]), "http");
        }

        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put("", true)
                .build();
        client = new RestHighLevelClient(RestClient.builder(hosts));
        indicesClient = client.indices();
        //indicesAdminClient = client.indices();
        //Sniffer sniffer = Sniffer.builder(client).build();
        //indicesAdminClient = client.

    }

    @Autowired
    public Elasticsearch(@Value("#{configProperties['elasticsearch.cluster-name']}") String clusterName,
                         @Value("#{configProperties['elasticsearch.cluster-nodes']}") String clusterNodes) throws UnknownHostException {
        this(clusterName, clusterNodes, MAXIMUM_CAPACITY);
        log.debug(" ====== es host: " + clusterNodes);
    }

    /**
     * insert or update document into ES
     * @param document
     */
    public void putDocument(Document document){
        if (document != null){
            int hash = hash(document.getId().hashCode());
            int i = indexFor(hash, table.length);
            table[i].addIndex(document);
        }

    }

    /**
     * fetch document
     * @param index
     * @param type
     * @param id
     * @return
     * @throws IOException
     */
    public Document getDocument(String index, String type, String id) throws IOException {
        GetRequest request = new GetRequest(index)
                .type(type)
                .id(id);
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        Document document = new Document()
                .setIndex(response.getIndex())
                .setType(response.getType())
                .setId(response.getId())
                .setVersion(response.getVersion())
                .setExists(response.isExists())
                .setSource(response.getSource());
        return document;
    }

    /**
     * delete document
     * @param index
     * @param type
     * @param id
     * @throws IOException
     */
    public void deleteDocument(String index, String type, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(index)
                .type(type)
                .id(id);
        client.delete(request, RequestOptions.DEFAULT);

    }

    public void deleteByQuery(String index, QueryBuilder queryBuilder){
        DeleteByQueryRequest request = new DeleteByQueryRequest();
        request.indices(index);


        //client.deleteByQuery()
    }

    /**
     *
     * @param alias
     * @return
     * @throws IOException
     */
    public List<String> getIndexByAlias(String alias) throws IOException {
        GetAliasesRequest getAliasesRequest = new GetAliasesRequest()
                .aliases(alias);
        GetAliasesResponse response = client.indices().getAlias(getAliasesRequest, RequestOptions.DEFAULT);
        Map<String, Set<AliasMetaData>> map = new HashMap<>(response.getAliases());

        List<String> indices = new ArrayList<>(map.size());
        map.forEach((k, v) -> indices.add(k));
        return indices;

    }


    /**
     * get all indices
     * @return
     * @throws IOException
     */
    public List<String> getIndices() throws IOException {
        GetIndexRequest request = new GetIndexRequest();
        GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
        return Arrays.asList(response.getIndices());
    }


    /**
     * async execute create index
     * @param index
     */
    public void createIndex(String index) {
        CreateIndexRequest request = new CreateIndexRequest(index);
        client.indices().createAsync(request, RequestOptions.DEFAULT, new ActionListener<CreateIndexResponse>() {

            @Override
            public void onResponse(CreateIndexResponse createIndexResponse) {
                log.info("create index : " + index + "successfully, waiting for all of the nodes  acknowledge the request");
            }

            @Override
            public void onFailure(Exception e) {
                log.error("create index : " + index + " occurred exception " + e, e);
            }
        });
    }

    /**
     *
     * @param index
     * @param mapping
     */
    public void createIndex(String index, String mapping){
        log.info("create index " + index + " with mapping " + mapping);
        CreateIndexRequest request = new CreateIndexRequest(index)
                .source(mapping, XContentType.JSON);
        client.indices().createAsync(request, RequestOptions.DEFAULT, new ActionListener<CreateIndexResponse>() {

            @Override
            public void onResponse(CreateIndexResponse createIndexResponse) {
                log.info("create index : " + index + "successfully, waiting for all of the nodes  acknowledge the request");
            }

            @Override
            public void onFailure(Exception e) {
                log.error("create index : " + index + " occurred exception " + e, e);
            }
        });
    }

    /**
     *
     * @param index
     * @param type
     * @param mapping
     */
    public void putMapping(String index, String type, String mapping){
        PutMappingRequest request = new PutMappingRequest(index)
                .type(type).source(mapping, XContentType.JSON);
        indicesClient.putMappingAsync(request, RequestOptions.DEFAULT, new ActionListener<AcknowledgedResponse>(){

            @Override
            public void onResponse(AcknowledgedResponse acknowledgedResponse) {
                log.info("put mapping in index : " + index +  " , type : " + type + " , mapping : " + mapping
                        + " successfully! ");
            }


            @Override
            public void onFailure(Exception e) {
                log.error("put mapping in index : " + index +  " , type : " + type + " , mapping : " + mapping
                        + " occurred " + e, e);
            }
        });
    }

    /**
     *
     * @param alias
     * @param index
     */
    public void createAlias(String alias, String index){
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        AliasActions aliasActions = new AliasActions(Type.ADD)
                .index(index).alias(alias);
        request.addAliasAction(aliasActions);
        indicesClient.updateAliasesAsync(request, RequestOptions.DEFAULT, new ActionListener<AcknowledgedResponse>() {
            @Override
            public void onResponse(AcknowledgedResponse acknowledgedResponse) {
                log.info("add alias " + alias + " to index " + index + " successfully!");
            }

            @Override
            public void onFailure(Exception e) {
                log.error("add alias " + alias + " to index " + index + " occurred exception " + e, e);
            }
        });
    }

    /**
     *
     * @param alias
     * @param index
     * @param mapping
     */
    public void createIndex(String alias, String index, String mapping){
        createIndex(index, mapping);
        createIndex(alias, index);
    }


    /**
     * delete index
     * @param index
     */
    public void deleteIndex(String index){
        DeleteIndexRequest request = new DeleteIndexRequest(index);
        indicesClient.deleteAsync(request, RequestOptions.DEFAULT, new ActionListener<AcknowledgedResponse>() {
            @Override
            public void onResponse(AcknowledgedResponse acknowledgedResponse) {
                log.info("delete index " + index + " successfully!");
            }

            @Override
            public void onFailure(Exception e) {
                log.error("delete index " + index + " occurred exception " + e, e);
            }
        });
    }

    /**
     *
     * @param index
     * @param type
     * @param from
     * @param size
     * @param queryBuilder
     * @param highlightBuilder
     * @return
     * @throws IOException
     */
    public SearchResult searchWithPage(String index, String type, int from, int size,
                                       QueryBuilder queryBuilder, HighlightBuilder highlightBuilder) throws IOException {
        SearchRequest request = new SearchRequest(index)
                .types(type)
                .searchType(SearchType.DFS_QUERY_THEN_FETCH);
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(queryBuilder);
        builder.from(from);
        builder.size(size);
        if (null != highlightBuilder) {
            builder.highlighter(highlightBuilder);
        }

        request.source(builder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        List<Document> documents = responseToDocument(response);
        SearchResult result = new SearchResult()
                .setHits(response.getHits().getHits().length)
                .setTotalHits(response.getHits().getTotalHits())
                .setDocuments(documents);
        return result;

    }

    /**
     *
     * @param query
     * @return
     * @throws IOException
     */
    public SearchResult search(Query query) throws IOException {
        SearchRequest request = new SearchRequest(query.getIndex())
                .types(query.getType())
                .preference("_replica_first");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        //if (!StringUtils.isEmpty(query.getIndex()))
        //if (!StringUtils.isEmpty(query.getType()))
        if (null != query.getFrom()) {
            builder.from(query.getFrom());
        }
        if (null != query.getSize()){
            builder.size(query.getSize());
        }
        if (null != query.getQueryBuilder()){
            builder.query(query.getQueryBuilder());
        }
        if (null != query.getHighlightBuilder()){
            builder.highlighter(query.getHighlightBuilder());
        }
        request.source(builder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        List<Document> documents = responseToDocument(response);

        SearchResult result = new SearchResult()
                .setHits(response.getHits().getHits().length)
                .setTotalHits(response.getHits().getTotalHits())
                .setDocuments(documents);
        return result;

    }


    /**
     *
     * @param response
     * @return
     */
    private List<Document> responseToDocument(SearchResponse response){
        List<Document> documents = new ArrayList<>();
        for (SearchHit hit : response.getHits()){
            Document document = new Document()
                    .setId(hit.getId())
                    .setIndex(hit.getIndex())
                    .setType(hit.getType())
                    .setExists(true)
                    .setVersion(hit.getVersion())
                    .setSource(hit.getSourceAsMap())
                    .setScore(hit.getScore())
                    .setHighlightFields(hit.getHighlightFields());
            documents.add(document);
        }
        return documents;
    }


    @PostConstruct
    public void prepareIndex(){
        table = new Node[capacity];
        for (int i = 0; i < table.length; i++){
            table[i] = new Node();
        }
    }

    @PreDestroy
    public void close(){
        if (table != null){
            for (Node nTable : table){
                nTable.stop();
            }
            while (true){
                boolean terminated = true;
                for (Node nTable : table){
                    if (!nTable.isTerminated()){
                        terminated = false;
                    }
                }
                if (terminated){break;}
            }
        }

        try {
            client.close();
        } catch (IOException e) {
            log.error("elasticsearch client close occurred IOException: " + e, e);
        }
    }

    private int hash(Object key){
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Only retain valid bits in the range of size relative to the table,
     * used to do array subscripts
     * @param h
     * @param length
     * @return
     */
    private int indexFor(int h, int length){
        return h & (length - 1);
    }

    public void refresh(String index) throws IOException {
        RefreshRequest refreshRequest = new RefreshRequest(index);
        client.indices().refresh(refreshRequest, RequestOptions.DEFAULT);
    }

    /**
     * Compute node capacity by specify cap
     * @param cap
     * @return
     */
    private int tableSizeFor(int cap){
        int n = cap -1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n < 0 ? 0 : ( n < MAXIMUM_CAPACITY ? n + 1 : MAXIMUM_CAPACITY);
    }


    class Node{
        private ExecutorService pool;

        public Node(){
            pool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), (Runnable r, ThreadPoolExecutor executor) -> {
                        if (!executor.isShutdown()){
                            try {
                                executor.getQueue().put(r);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }

        public void addIndex(Document document){
            Worker worker = new Worker(document);
            pool.execute(worker);
        }

        public void stop(){
            pool.shutdownNow();
        }

        public boolean isTerminated(){
            return pool.isTerminated();
        }

        class Worker extends Thread{
            private Document document;
            public Worker(Document document){
                this.document = document;
            }

            @Override
            public void run() {
                //super.run();
                if (document != null){
                    log.info(Thread.currentThread().getName() + " id: " + document.getId() + " type: " + document.getType());
                    try {
                        if (document.getAction() == Document.INDEX){
                            IndexRequest request = new IndexRequest(document.getIndex())
                                    .id(document.getId())
                                    .type(document.getType())
                                    .source(document.getSource())
                                    .timeout(new TimeValue(5, TimeUnit.SECONDS));
                            client.index(request, RequestOptions.DEFAULT);
                        }else if (document.getAction() == Document.UPDATE){
                            UpdateRequest request = new UpdateRequest(document.getIndex(), document.getType(), document.getId())
                                    .doc(document.getSource()).upsert(document.getSource())
                                    .timeout(new TimeValue(5, TimeUnit.SECONDS));
                            client.update(request, RequestOptions.DEFAULT);
                        }
                    } catch (IOException e) {
                        log.error("insert elasticsearch exception: " + e, e);
                    }
                }
            }
        }


    }

}
