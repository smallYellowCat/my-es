package com.doudou.es.common;

import java.io.UnsupportedEncodingException;
import java.net.URL;

public final class StringParse {

	private StringParse() {
	}
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

	
	private static final String URL_MATCH = "[a-zA-z]+://[^\\s]*";
	/**
	 * 检验是否为url
	 * @param url
	 * @return true 是 false 否
	 */
	public static boolean urlCheck(String url) {
		if(url == null)
			throw new NullPointerException();
		return url.matches(URL_MATCH);
	}
	
	/**
	 * 统计有多少个汉字
	 * @param url
	 * @return true 是 false 否
	 */
	public static int getLength(String str) {  
	    int valueLength = 0;    
	       String chinese = "[\u4e00-\u9fa5]";    
	       // 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1    
	       for (int i = 0; i < str.length(); i++) {    
	           // 获取一个字符    
	           String temp = str.substring(i, i + 1);    
	           // 判断是否为中文字符    
	           if (temp.matches(chinese)) {    
	               // 中文字符长度为1    
	               valueLength += 1;    
	           } 
	       }    
	       return  valueLength;    
	   }  
	
	/**
	 * 字符串划分为前几个汉字
	 * @param url
	 * @return true 是 false 否
	 */
	public static String getStr(String str,int num) {  
	    int valueLength = 0;    
	       String chinese = "[\u4e00-\u9fa5]";    
	       // 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1    
	       for (int i = 0; i < str.length(); i++) {    
	           // 获取一个字符    
	           String temp = str.substring(i, i + 1);    
	           // 判断是否为中文字符    
	           if (temp.matches(chinese)) {    
	               // 中文字符长度为1    
	               valueLength += 1;    
	           } 
	           if(valueLength>num){
	        	   return str.substring(0, i)+"...";
	           }
	       }    
	       return  str;    
	   }
	/**
	 * "file:/home/whf/cn/fh" -> "/home/whf/cn/fh"
	 * "jar:file:/home/whf/foo.jar!cn/fh" -> "/home/whf/foo.jar"
	 */
	public static String getRootPath(URL url) {
		String fileUrl = url.getFile();
		int pos = fileUrl.indexOf('!');

		if (-1 == pos) {
			return fileUrl;
		}

		return fileUrl.substring(5, pos);
	}

	/**
	 * "cn.fh.lightning" -> "cn/fh/lightning"
	 * @param name
	 * @return
	 */
	public static String dotToSplash(String name) {
		return name.replaceAll("\\.", "/");
	}

	/**
	 * "Apple.class" -> "Apple"
	 * "cn.fh.lightning.class" -> "cn.fh.lightning"
	 */
	public static String trimExtension(String name) {
		int pos = name.lastIndexOf('.');
		if (-1 != pos) {
			return name.substring(0, pos);
		}

		return name;
	}

	/**
	 * /application/home -> /home
	 * @param uri
	 * @return
	 */
	public static String trimURI(String uri) {
		String trimmed = uri.substring(1);
		int splashIndex = trimmed.indexOf('/');

		return trimmed.substring(splashIndex);
	}

}
