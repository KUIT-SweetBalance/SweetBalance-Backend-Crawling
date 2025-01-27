package com.sweetbalance.crawling_component.service;

import com.sweetbalance.crawling_component.crawler.MegaCrawler;
import com.sweetbalance.crawling_component.crawler.PaikCrawler;
import com.sweetbalance.crawling_component.crawler.StarbucksCrawler;
import com.sweetbalance.crawling_component.entity.Beverage;
import com.sweetbalance.crawling_component.entity.BeverageSize;
import com.sweetbalance.crawling_component.repository.BeverageRepository;
import jakarta.transaction.Transactional;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CrawlingService {

    private final WebDriverService webDriverService;
    private final StarbucksCrawler starbucksCrawler;
    private final MegaCrawler megaCrawler;
    private final PaikCrawler paikCrawler;
    private final BeverageRepository beverageRepository;

    @Autowired
    public CrawlingService(WebDriverService webDriverService,
                           StarbucksCrawler starbucksCrawler,
                           MegaCrawler megaCrawler,
                           PaikCrawler paikCrawler,
                           BeverageRepository beverageRepository) {
        this.webDriverService = webDriverService;
        this.starbucksCrawler = starbucksCrawler;
        this.megaCrawler = megaCrawler;
        this.paikCrawler = paikCrawler;
        this.beverageRepository = beverageRepository;
    }

    @Transactional
    public void executeCrawling() {
        WebDriver webDriver = webDriverService.createWebDriver();
        try {
            starbucksCrawler.setWebDriver(webDriver);
            List<Beverage> starbucksData = starbucksCrawler.crawlBeverageList();
            processAndSaveBeverages(starbucksData);

            megaCrawler.setWebDriver(webDriver);
            List<Beverage> megaData = megaCrawler.crawlBeverageList();
            processAndSaveBeverages(megaData);

            paikCrawler.setWebDriver(webDriver);
            List<Beverage> paikData = paikCrawler.crawlBeverageList();
            processAndSaveBeverages(paikData);

        } finally {
            webDriverService.quitWebDriver(webDriver);
        }
    }

    private void processAndSaveBeverages(List<Beverage> beverages) {
        for (Beverage newBeverage : beverages) {
            Optional<Beverage> existingBeverage =
                    beverageRepository.findByNameAndBrand(newBeverage.getName(), newBeverage.getBrand());

            if (existingBeverage.isPresent()) {
                updateExistingBeverage(existingBeverage.get(), newBeverage);
                beverageRepository.save(existingBeverage.get());
            } else {
                beverageRepository.save(newBeverage);
            }
        }
    }

    private void updateExistingBeverage(Beverage existingBeverage, Beverage newBeverage) {
        // 이름과 브랜드는 변경되지 않는다고 가정
        // existingBeverage.setName(newBeverage.getName());
        // existingBeverage.setBrand(newBeverage.getBrand());

        existingBeverage.setCategory(newBeverage.getCategory());
        existingBeverage.setImgUrl(newBeverage.getImgUrl());

        // 기존 BeverageSize 업데이트 로직 - 새로운 사이즈 추가
        updateBeverageSizes(existingBeverage, newBeverage.getSizes());

        existingBeverage.setSugar(newBeverage.getSugar());
        existingBeverage.setCalories(newBeverage.getCalories());
        existingBeverage.setCaffeine(newBeverage.getCaffeine());

        existingBeverage.setStatus(newBeverage.getStatus());
    }

    private void updateBeverageSizes(Beverage existingBeverage, List<BeverageSize> newSizes) {
        for (BeverageSize newSize : newSizes) {
            boolean exists = existingBeverage.getSizes().stream()
                    .anyMatch(size -> size.getSizeType().equals(newSize.getSizeType()) && size.getVolume() == newSize.getVolume());
            if (!exists) {
                existingBeverage.addSize(newSize);
            }
        }
    }
}

