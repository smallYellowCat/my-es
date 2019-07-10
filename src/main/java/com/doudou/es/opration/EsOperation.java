package com.doudou.es.opration;

import com.doudou.es.opration.add.RequestParam;
import com.doudou.es.opration.add.ResponseParam;

/**
 * es的操作接口
 * @author 豆豆
 * @date 2019/1/15 19:58
*/
public interface EsOperation {

    /**
     *
     * @param requestParam 添加索引得参数
     * @return
     */
    ResponseParam addIndex(RequestParam requestParam);

    void updateIndex();

    void search();

    void deleteIndex();

}
