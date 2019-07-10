package com.doudou.es.opration.add;

import com.doudou.es.opration.EsOperation;
import com.doudou.es.opration.Execute;

/**
 * @author 豆豆
 * @date 2019/1/15 20:05
*/
public class Add implements Execute<ResponseParam, RequestParam> {

    private final EsOperation operation;

    public Add(EsOperation operation){
        this.operation = operation;
    }


    @Override
    public ResponseParam execute(RequestParam requestParam) {
        return operation.addIndex(requestParam);
    }
}
