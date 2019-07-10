package com.doudou.es.sync;

import com.doudou.es.elasticsearch.ElasticSearch;
import com.doudou.es.elasticsearch.Mapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;

@Service
@Slf4j
public class MappingService {
    @Autowired
    private ElasticSearch elasticSearch;
    @Autowired
    private Mapping mapping;
    @Value("#{configProperties['elasticsearch.index']}")
    private String alias;

    /**
     * 创建index以及mapping
     */
    public void createIndexMapping() {
        try {
            String m = mapping.readMapping();
            log.info(m);
            elasticSearch.createIndexMapping(alias, m);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void putMapping(String type) {
        try {
            String m = mapping.readMapping(type);
            System.out.println(m);
            elasticSearch.putMapping(alias, type, m);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
