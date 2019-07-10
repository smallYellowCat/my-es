package com.doudou.es.controller;

import com.doudou.es.service.EsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 豆豆
 * @date 2019/1/17 15:14
*/
@RestController
@RequestMapping("/es")
public class EsController {

    @Autowired
    private EsService esService;

    @GetMapping("/add.do")
    private Object add(){
        Map<String, Object> data = new HashMap<>();
        data.put("name", "豆朋伟");
        data.put("age", 18);
        return esService.addIndex("aa", "2", "_doc", data);
    }

    @GetMapping("/get.do")
    private Object get(){
        return esService.getIndex("aa","2", "_doc");
    }

}
