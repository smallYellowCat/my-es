package com.doudou.es.service.search_v1.service;

import com.doudou.es.service.search_v1.AbstractSearchService;
import com.doudou.es.service.search_v1.common.EntityForSearch;
import com.doudou.es.service.search_v1.elasticsearch.Document;
import com.doudou.es.service.search_v1.entity.Book;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService extends AbstractSearchService {

    public SearchService() {
        super("book", "book");
    }

    /**
     * @param id
     * @param size
     * @return
     */
    @Override
    public List<? extends EntityForSearch> selectAll(Integer id, Integer size) {
        return null;
    }

    public String putData(Book book){
       insert(book);
       return "insert ok!";
    }


}
