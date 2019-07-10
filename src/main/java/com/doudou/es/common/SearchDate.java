package com.doudou.es.common;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Created by wangqi on 2017/3/23.
 */
@Getter
@Setter
public class SearchDate {
    private Integer selectDay;
    private Date startTime;
    private Date endTime;
}
