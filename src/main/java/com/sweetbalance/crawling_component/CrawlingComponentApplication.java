package com.sweetbalance.crawling_component;

import com.sweetbalance.crawling_component.service.CrawlingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrawlingComponentApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrawlingComponentApplication.class, args);
	}

	// 애플리케이션 시작 시에도 크롤링을 실행
	@Bean
	public CommandLineRunner schedulingRunner(CrawlingService crawlingService) {
		return args -> {
			crawlingService.executeCrawling();
		};
	}
}
