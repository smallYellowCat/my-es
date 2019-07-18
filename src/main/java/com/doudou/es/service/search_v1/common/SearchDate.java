package com.doudou.es.service.search_v1.common;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author 豆豆
 * @date 2019/7/18 18:32
 * @flag 以万物智能，化百千万亿身
 */
@Getter
@Setter
public class SearchDate {
    private Integer selectDay;
    private Date startTime;
    private Date endTime;
}
