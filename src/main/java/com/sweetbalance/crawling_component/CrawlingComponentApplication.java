package com.sweetbalance.crawling_component;

import com.sweetbalance.crawling_component.service.CrawlingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class CrawlingComponentApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(CrawlingComponentApplication.class, args);

		CrawlingService crawlingService = context.getBean(CrawlingService.class);
		crawlingService.executeCrawling();
	}
}
