package com.doudou.es.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.io.Resources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class Mapping {
    private String readContent(Path path) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        reader.lines().forEach(sb::append);
        return sb.toString();
    }

    private Map<String, Object> parse(String str) {
        Map<String, Object> mapping = JSON.parseObject(str, new TypeReference<Map<String, Object>>(){});
        return mapping;
    }

    private Map<String, Object> addPropertiesPrefix(Map<String, Object> properties, String prefix) {
        Map<String, Object> newProperties = new HashMap<>();
        properties.forEach((k, v) -> newProperties.put(prefix + "_" + k, v));
        return newProperties;
    }

    private void addTypePrefix(Map<String, Object> map) {
        String typeName = map.keySet().iterator().next();
        Map<String, Object> newProperties = addPropertiesPrefix((Map<String, Object>) ((Map) map.get(typeName)).get("properties"), typeName);
        ((Map) map.get(typeName)).put("properties", newProperties);

    }


    public String readMapping() throws IOException, URISyntaxException {
        Resource res = new ClassPathResource("mapping/mapping.txt");
        String str = Resources.toString(res.getURL(), Charset.defaultCharset());
        Map<String, Object> mapping = parse(str);

        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        Resource[] mappingLocations = patternResolver.getResources("mapping/*.json");
        for (Resource resource : mappingLocations) {
            String s = Resources.toString(resource.getURL(), Charset.defaultCharset());
            Map<String, Object> m = parse(s);
            addTypePrefix(m);
            ((Map<String, Object>)mapping.get("mappings")).putAll(m);
        }
        return JSON.toJSONString(mapping);
    }

    public String readMapping(String type) throws URISyntaxException, IOException {
        Resource res = new ClassPathResource("classpath:mapping/" + type + ".json");
        String str = Resources.toString(res.getURL(), Charset.defaultCharset());
        Map<String, Object> mapping = parse(str);

        addTypePrefix(mapping);
        return JSON.toJSONString(mapping);
    }
}
