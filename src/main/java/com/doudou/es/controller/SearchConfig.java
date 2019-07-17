package com.doudou.es.controller;

import com.doudou.es.service.search_v1.sync.MappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/web/config/search")
public class SearchConfig {

    @Autowired
    private MappingService mappingService;


    @GetMapping("/mapping")
    public String mappinng() {
        mappingService.createIndexMapping();
        return "mapping success";
    }
}
