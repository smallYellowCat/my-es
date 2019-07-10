package com.doudou.es.service;

import com.doudou.es.config.EsFactory;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * @author 豆豆
 * @date 2019/1/17 15:16
*/
@Service
public class EsService {

    /**
     *
     */


    private TransportClient client = EsFactory.getClient();

    public Object addIndex(String index, String id, String type, Map<String, Object> json){
        IndexResponse response = null;
        try {
            XContentBuilder builder = jsonBuilder().startObject();
            json.forEach((k,v) -> {
                try {
                    builder.field(k,v);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            builder.endObject();

            response = client.prepareIndex(index, type, id).setSource(builder).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }


    public Object getIndex(String index, String id, String type){
        GetResponse response = client.prepareGet(index, type, id).get();
        return response;
    }

    public Object deleteIndex(String index, String id, String type){
        DeleteResponse response = client.prepareDelete("twitter", "_doc", "1").get();
        return response;
    }

}
