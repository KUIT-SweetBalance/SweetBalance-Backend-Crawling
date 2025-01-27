package com.sweetbalance.crawling_component.crawler;

import com.sweetbalance.crawling_component.entity.Beverage;
import org.openqa.selenium.WebDriver;

import java.util.List;

public abstract class BaseCrawler {

    protected WebDriver driver;

    public void setWebDriver(WebDriver driver) {
        this.driver = driver;
    }

    public abstract List<Beverage> crawlBeverageList();

    protected void navigateTo(String url) {
        driver.get(url);
    }
}