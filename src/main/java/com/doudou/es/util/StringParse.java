package com.doudou.es.util;

import java.io.UnsupportedEncodingException;

/**
 * @author 豆豆
 * @date 2019/7/17 17:06
 * @flag 以万物智能，化百千万亿身
 */
public final class StringParse {

    private StringParse(){}

    /**
     * get方式提交的参数编码，默认编码iso8859-1编码，
     * <p>需要自行编码成utf-8
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String convertGetParam(String str) throws UnsupportedEncodingException {
        if(str == null)
            return null;
        return new String(str.getBytes("iso8859-1"), "utf-8");
    }

    public static String convertParam(String str) {
        try {
            if(str.equals(new String(str.getBytes("iso8859-1"), "iso8859-1")))
                return convertGetParam(str);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }
}
