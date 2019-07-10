package com.doudou.es.sync;

import com.doudou.es.service.AbstractSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 豆豆
 * @date 2019/7/10 14:58
 * @flag 以万物智能，化百千万亿身
 */
@Service
public class SearchSyncService {
    @Autowired
    public List<AbstractSearchService> searchServiceList;

    public void sync() {
        List<Thread> threadList = new ArrayList<>();

        class SyncThread implements Runnable {

            AbstractSearchService searchService;

            public SyncThread(AbstractSearchService searchService) {
                this.searchService = searchService;
            }

            @Override
            public void run() {
                searchService.sync();
            }
        }

        for (AbstractSearchService searchService : searchServiceList) {
            Thread t = new Thread(new SyncThread(searchService));
            threadList.add(t);
        }

        for (Thread thread : threadList) {
            thread.start();
        }

//        for (Thread thread : threadList) {
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }
}
