package com.doudou.es.elasticsearch;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.ToString;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 豆豆
 * @date 2019/1/22 10:55
*/
@ToString
public class Document {

    private String index;

    private String type;

    private String id;

    private long version;

    private boolean exists;

    private Map<String, Object> source;

    private double score;

    private Map<String, HighlightField> highlightFields;

    @JSONField(serialize = false, deserialize = false)
    private int action = INDEX;
    @JSONField(serialize = false, deserialize = false)
    public static final int INDEX = 0;
    @JSONField(serialize = false, deserialize = false)
    public static final int UPDATE = 1;



    Document() {}

    public Document(String index, String type, String id) {
        this(index, type, id, new HashMap<>());
    }

    public Document(String index, String type, String id, Map<String, Object> source) {
        this.index = index;
        this.type = type;
        this.id = id;
        this.source = source;
    }

    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public boolean isExists() {
        return exists;
    }

    public double getScore() {
        return score;
    }

    void setIndex(String index) {
        this.index = index;
    }

    void setType(String type) {
        this.type = type;
    }

    void setId(String id) {
        this.id = id;
    }

    void setVersion(long version) {
        this.version = version;
    }

    void setExists(boolean exists) {
        this.exists = exists;
    }

    void setSource(Map<String, Object> source) {
        this.source = source;
    }

    void setScore(double score) {
        this.score = score;
    }

    public void addData(String key, Object value) {
        source.put(key, value);
    }

    public Map<String, Object> getSourceAsMap() {
        return source;
    }

    public String getSource() {
        return JSON.toJSONString(source, true);
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public Map<String, HighlightField> getHighlightFields() {
        return highlightFields;
    }

    public void setHighlightFields(Map<String, HighlightField> highlightFields) {
        this.highlightFields = highlightFields;
    }

    public void removePrefix() {
        if (source != null) {
            Map<String, Object> m = new HashMap<>();
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                String key = entry.getKey();
                if (type == null || type.isEmpty()) {
                    key = key.substring(key.indexOf("_") + 1);
                } else {
                    key = key.substring(type.length() + 1);
                }
                m.put(key, entry.getValue());
            }
            source = m;
        }

        if (highlightFields != null) {
            Map<String, HighlightField> h = new HashMap<>();
            for (Map.Entry<String, HighlightField> entry : highlightFields.entrySet()) {
                String key = entry.getKey();
                if (type == null || type.isEmpty()) {
                    key = key.substring(key.indexOf("_") + 1);
                } else {
                    key = key.substring(type.length() + 1);
                }
                h.put(key, entry.getValue());
            }
            highlightFields = h;
        }
    }

//    public void removePrefix() {
//        Map<String, Object> m = new HashMap<>();
//        for (Map.Entry<String, Object> entry : source.entrySet()) {
//            String key = entry.getKey();
//            String[] s = key.split("_");
//            key = s[s.length - 1];
//            m.put(key, entry.getValue());
//        }
//        source.clear();
//        source.putAll(m);
//    }

}
