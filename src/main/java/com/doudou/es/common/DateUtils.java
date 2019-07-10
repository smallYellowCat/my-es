package com.doudou.es.common;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 
 * Title : DateUtils.java<br>
 * Description: 日期工具类<br>
 * 
 * @author gsj
 *
 */
public class DateUtils {

    public static String getCurrentDateString8() {
        return getDateString(new Date(), "yyyyMMdd");
    }

    public static String getCurrentDateString(String format) {
        return getDateString(new Date(), format);
    }

    public static Date getDate(Long date) {
    	if(date == null)
    		return null;
    	return new Date(date);
    }
    
    /**
     * @Title: getDateString
     * @Description: 格式化日期
     * @param date
     *            日期
     * @param format
     *            yyyyMMdd hh:mm:ss.SSS 模式
     * @return String 返回类型
     */
    public static String getDateString(Date date, String format) {
        if (date != null) {
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            String dateString = formatter.format(date);
            return dateString;
        }
        return null;
    }

    /**
     * 格式不合法时，返回null
     * 
     * @param datetime
     * @param formart
     * @return
     */
    public static Date getDateTime(String datetime, String formart) {
        SimpleDateFormat sdf = new SimpleDateFormat(formart);
        Date date = null;
        try {
            date = sdf.parse(datetime);
        } catch (ParseException e) {
            return null;
        }
        return date;
    }

    public static long beforeNTimestamp(int day) {
        long today = System.currentTimeMillis();
        return today - day * 24 * 60 * 60 * 1000L;
    }

    public static String curYear() {
        DateFormat df = new SimpleDateFormat("yyyy");
        return df.format(new Date());
    }

    public static String curMonth() {
        DateFormat df = new SimpleDateFormat("MM");
        return df.format(new Date());
    }

    public static String curDay() {
        DateFormat df = new SimpleDateFormat("dd");
        return df.format(new Date());
    }

    /*
     * public static void main(String[] args) { System.out.println(curYear()); }
     */

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static Timestamp toTimeStamp(Long time) {
        return new Timestamp(time);
    }

    public static String getYestodayString8() {
        long time = beforeNTimestamp(1);
        Date date = new Date();
        date.setTime(time);
        return getDateString(date, "yyyyMMdd");
    }

    public static long getTimeStamp(String dateStr, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date;
        long relt = 0;
        try {
            date = sdf.parse(dateStr);
            relt = date.getTime();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return relt;
    }

    public static Date getCurrentDate() {
        return new Date();
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    public static Date dateStart(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static Date dateEnd(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }

}
