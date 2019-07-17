package com.doudou.es.service.search_v1.sync;

import com.alibaba.fastjson.JSON;
import com.doudou.es.service.search_v1.elasticsearch.Elasticsearch;
import com.doudou.es.service.search_v1.elasticsearch.Mapping;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 豆豆
 * @date 2019/7/16 14:23
 * @flag 以万物智能，化百千万亿身
 */
@Service
@Slf4j
public class MappingService {

    @Autowired
    private Elasticsearch elasticsearch;

    @Autowired
    private Mapping mapping;

    @Value("#{configProperties['elasticsearch.index']}")
    private String alias;


    public void createIndexMapping(){

        try {
            String m = mapping.readMapping();
            log.info(m);
            elasticsearch.createIndex(alias, alias + "_" + getDateString(), m);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void putMapping(String type){
        try {
            String m = mapping.readMapping(type);
            log.info(m);
            elasticsearch.putMapping(alias, type, m);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDateString() {
        LocalDateTime time = LocalDateTime.now();
        return time.format(DateTimeFormatter.ofPattern("yyyMMddHHmmss"));
    }
}
