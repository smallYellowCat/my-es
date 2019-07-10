package com.doudou.es.opration.add;

/**
 * @author 豆豆
 * @date 2019/1/17 14:46
*/
public class ResponseParam {

    /**
     * 索引名称
     */
    private String index;

    /**
     * 索引类型
     */
    private String type;

    /**
     * 索引id
     */
    private String id;

    /**
     * 索引的版本号
     */
    private Integer version;

    /**
     * 执行的结果
     */
    private String result;

    /**
     * 副本的信息
     */
    private Shard shards;

    private Integer seqNo;

    private Integer prinaryTerm;

}

class Shard{
    /**
     *总数
     */
    private Integer total;

    /**
     * 成功的数量
     */
    private Integer successful;

    /**
     * 失败的数量
     */
    private Integer failed;
}
