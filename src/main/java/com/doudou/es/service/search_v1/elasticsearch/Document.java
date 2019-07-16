package com.doudou.es.service.search_v1.elasticsearch;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import java.util.HashMap;
import java.util.Map;

/**
 * es document that look like line of table in database
 * @author 豆豆
 * @date 2019/7/13 17:43
 * @flag 以万物智能，化百千万亿身
 * @since 1.0
 */
@NoArgsConstructor
@Setter
@Getter
@Accessors(chain = true)
public class Document {
    private String index;
    private String type;
    private String id;
    private long version;
    private boolean exists;
    private Map<String, Object> source;
    private double score;

    private Map<String, HighlightField> highlightFields;


    private transient int action = INDEX;

    public transient static final int INDEX = 0;

    public transient static final int UPDATE = 1;

    public Document(String index, String type, String id){
        this(index, type, id, new HashMap<>());
    }

    public Document(String index, String type, String id, Map<String, Object> source){
        this.index = index;
        this.type = type;
        this.id = id;
        this.source = source;
    }

    public void removePrefix(){
        if (source != null) {

            Map<String, Object> m = new HashMap<>();
            source.forEach((k,v) -> {
                if (type == null || type .isEmpty()){
                    k = k.substring(k.indexOf("_") + 1);
                }else {
                    k = k.substring(type.length() + 1);
                }

                m.put(k, v);
            });

            source = m;
        }

        if (highlightFields != null){
            Map<String, HighlightField> h = new HashMap<>();
            highlightFields.forEach((k, v) -> {
                if (type == null || type.isEmpty()){
                    k = k.substring(k.indexOf("_") + 1);
                }else {
                    k = k.substring(type.length() + 1);
                }
                h.put(k, v);
            });

            highlightFields = h;
        }
    }


}
