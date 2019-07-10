package com.doudou.es.pagehelper;

import com.github.pagehelper.Page;
import lombok.ToString;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
public class PageContent<T>{
	@SuppressWarnings("unchecked")
	public static final PageContent<?> EMPTY_PAGE = new PageContent<>(Collections.EMPTY_LIST, 0, 0, 0, 0);

	public final List<T> array;
	
	public final int pageNo;
    /**
     * 页记录数
     */
	public final int pageSize;
    
    /**
     * 总数
     */
	public final long total;
    /**
     * 总页数
     */
	public final int totalPage;
	
	public PageContent(Page<T> page) {
		array = page.getResult();
		pageNo = page.getPageNum();
		pageSize = page.getPageSize();
		total = page.getTotal();
		totalPage = page.getPages();
	}
	
	public PageContent(List<T> dataList, int currentPageNo, int currentPageSize,
                       long totalCount, int totalPageCount) {
        array = dataList;
        pageNo = currentPageNo;
        pageSize = currentPageSize;
        total = totalCount;
        totalPage = totalPageCount;
    }

    public static Map<String, Object> getContent(Long totalHits, Integer pageNo, Integer pageSize, int MAX_PAGE) {
		Map<String, Object> map = new HashMap<>();
		int totalPage = (int) Math.ceil(totalHits/(pageSize*1.0));
		totalPage = totalPage < MAX_PAGE ? totalPage : MAX_PAGE;
		int totalCount = totalHits.intValue();
		map.put("pageNo", pageNo);
		map.put("pageSize", pageSize);
		map.put("total", totalCount);
		map.put("totalPage", totalPage);
		return map;
	}

}
