package com.sweetbalance.crawling_component.service;

import com.sweetbalance.crawling_component.crawler.StarbucksCrawler;
import com.sweetbalance.crawling_component.entity.Beverage;
import com.sweetbalance.crawling_component.entity.BeverageSize;
import com.sweetbalance.crawling_component.repository.BeverageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CrawlingService {

    private final StarbucksCrawler starbucksCrawler;
    private final BeverageRepository beverageRepository;

    @Autowired
    public CrawlingService(StarbucksCrawler starbucksCrawler, BeverageRepository beverageRepository) {
        this.starbucksCrawler = starbucksCrawler;
        this.beverageRepository = beverageRepository;
    }

    public void executeCrawling() {

        List<Beverage> starbucksData = starbucksCrawler.crawlBeverageList();
        for (Beverage newBeverage : starbucksData) {

            Optional<Beverage> existingBeverage =
                    beverageRepository.findByNameAndBrand(newBeverage.getName(), newBeverage.getBrand());

            if (existingBeverage.isPresent()) {
                Beverage beverage = existingBeverage.get();

                // 기존 데이터 업데이트
                updateExistingBeverage(beverage, newBeverage);
                beverageRepository.save(beverage);
            } else {

                // 새 데이터 삽입
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

