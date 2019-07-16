package com.doudou.es.service.search_v1.elasticsearch;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.UnknownHostException;
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
    private IndicesAdminClient indicesAdminClient;

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


        client.deleteByQuery()
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
