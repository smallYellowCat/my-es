package com.doudou.es.controller;

import com.doudou.es.service.search_v1.elasticsearch.Elasticsearch;
import com.doudou.es.service.search_v1.entity.Book;
import com.doudou.es.service.search_v1.service.SearchService;
import com.doudou.es.service.search_v1.sync.MappingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/web/config/search")
@Api
public class SearchConfig {

    @Autowired
    private MappingService mappingService;
    @Autowired
    private Elasticsearch elasticsearch;
    @Autowired
    private SearchService searchService;


    @GetMapping("/mapping")
    @ApiOperation(value = "创建索引", httpMethod = "GET")
    public String mappinng() {
        mappingService.createIndexMapping();
        return "mapping success";
    }

    @PostMapping("/insert")
    @ApiOperation(value = "插入数据", httpMethod = "POST")
    public String insert(Book book){
        return searchService.putData(book);
    }


}
