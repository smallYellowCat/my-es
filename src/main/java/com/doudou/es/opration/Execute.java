package com.doudou.es.opration;
/**
 * @author 豆豆
 * @date 2019/1/15 20:05
*/
public interface Execute<R,T> {

    /**
     * 根据不同的操作类型返回不同的结果
     * @param t 入参
     * @return
     */
    R execute(T t);
}
