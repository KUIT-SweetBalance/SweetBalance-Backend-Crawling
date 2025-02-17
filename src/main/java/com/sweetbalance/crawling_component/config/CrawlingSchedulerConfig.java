package com.sweetbalance.crawling_component.config;

import com.sweetbalance.crawling_component.service.CrawlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class CrawlingSchedulerConfig {

    private final CrawlingService crawlingService;

    @Autowired
    public CrawlingSchedulerConfig(CrawlingService crawlingService) {
        this.crawlingService = crawlingService;
    }

    // 매달 1일과 15일 새벽 4시에 주기적 크롤링 진행
    @Scheduled(cron = "0 0 4 1,15 * ?")
    public void scheduleCrawling() {
        crawlingService.executeCrawling();
    }
}