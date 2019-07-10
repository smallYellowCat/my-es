package com.doudou.es.config;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author 豆豆
 * @date 2019/1/15 19:24
*/
@Component
public class EsFactory {

    private static TransportClient client;

    @PostConstruct
    public static void init(){
        // on startup
        initEs();

    }

    /**
     * 初始化客户端
     */
    static void initEs(){
        try {
            //启用嗅探
            /**
             * The Transport client comes with a cluster sniffing feature which allows it to dynamically add
             * new hosts and remove old ones. When sniffing is enabled, the transport client will connect to
             * the nodes in its internal node list, which is built via calls to addTransportAddress. After this,
             * the client will call the internal cluster state API on those nodes to discover available data nodes.
             * The internal node list of the client will be replaced with those data nodes only. This list is
             * refreshed every five seconds by default. Note that the IP addresses the sniffer connects to are the
             * ones declared as the publish address in those node’s Elasticsearch config.
             *
             * Keep in mind that the list might possibly not include the original node it connected to if that node
             * is not a data node. If, for instance, you initially connect to a master node, after sniffing, no further
             * requests will go to that master node, but rather to any data nodes instead. The reason the transport client
             * excludes non-data nodes is to avoid sending search traffic to master only nodes.
             *
             * In order to enable sniffing, set client.transport.sniff to true:
             */
            Settings settings = Settings.builder()
                    .put("client.transport.sniff", true).build();
            client = new PreBuiltTransportClient(settings);
            client.addTransportAddress(new TransportAddress(InetAddress.getByName("server.doudou.com"), 9300));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取客户端实例
     * @return
     */
    public static TransportClient getClient(){
        if (null == client){
            initEs();
        }
        return client;
    }

}
