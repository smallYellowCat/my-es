package com.doudou.es.common;

import org.joda.time.DateTime;

import java.util.Date;

public class WebTimeUtil {

    private WebTimeUtil() {
    }

    public  static <T extends SearchDate> void convertSelectTime(T obj, Long startTime, Long endTime, Integer selectDay) {
        if(obj == null)
            throw new NullPointerException();
        if(selectDay != null) {
            selectDay--;//需求：查询条件为最近一天，代表查询今天
            DateTime start = new DateTime();
            DateTime newStart = new DateTime(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth(), 0, 0 ,0);
            obj.setStartTime(newStart.plusDays(-selectDay).toDate());
            obj.setEndTime(new Date());
            return;
        }
        if(startTime != null) {
            obj.setStartTime(new Date(startTime));
            obj.setEndTime(endTime == null ? (new DateTime(startTime).plusDays(1).toDate()) : new Date(endTime));
            return;
        }
        if(endTime != null) {
            obj.setEndTime(new Date(endTime));
            obj.setStartTime(startTime == null ? null : new Date(startTime));
        }
    }
}
