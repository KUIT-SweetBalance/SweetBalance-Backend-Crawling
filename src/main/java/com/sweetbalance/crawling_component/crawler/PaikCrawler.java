package com.sweetbalance.crawling_component.crawler;

import com.sweetbalance.crawling_component.entity.Beverage;
import com.sweetbalance.crawling_component.entity.BeverageSize;
import com.sweetbalance.crawling_component.enums.beverage.BeverageCategory;
import com.sweetbalance.crawling_component.enums.common.Status;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaikCrawler extends BaseCrawler {

    @Value("${spring.selenium.base-url.paik.coffee}")
    private String COFFEE_URL;

    @Value("${spring.selenium.base-url.paik.drink}")
    private String DRINK_URL;

    @Value("${spring.selenium.base-url.paik.ccino}")
    private String CCINO_URL;

    private static final String MENU_LIST_CSS = "div.menu_list.clear > ul";
    private static final String CATEGORY_XPATH = "/html/body/div/div[2]/div[1]/div/h1";

    @Override
    public List<Beverage> crawlBeverageList() {
        List<Beverage> results = new ArrayList<>();
        List<String> urls = List.of(COFFEE_URL, DRINK_URL, CCINO_URL);

        for (String url : urls) {
            results.addAll(crawlUrl(url));
        }

        return results;
    }

    private List<Beverage> crawlUrl(String url) {
        List<Beverage> beverages = new ArrayList<>();
        try {
            navigateTo(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            String category = wait.until(driver -> driver.findElement(By.xpath(CATEGORY_XPATH)))
                    .getAttribute("textContent");

            List<WebElement> menuItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(MENU_LIST_CSS + " > li")));

            for (WebElement item : menuItems) {
                Beverage beverage = parseBeverage(item, category);
                if (beverage != null) {
                    beverages.add(beverage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return beverages;
    }

    private Beverage parseBeverage(WebElement item, String category) {
        try {
            String name = item.findElement(By.className("menu_tit")).getAttribute("textContent");
            String imgUrl = item.findElement(By.cssSelector("div.thumb img")).getAttribute("src");

            WebElement hoverElement = item.findElement(By.className("hover"));

            String sizeInfo;
            String rawSizeStr = "";;
            int volume = 0;;

            try {
                sizeInfo = hoverElement.findElement(By.className("menu_ingredient_basis")).getAttribute("textContent");
                String[] sizeParts = sizeInfo.split(":");
                if (sizeParts.length > 1) {
                    rawSizeStr = sizeParts[1].trim();
                    volume = parseVolume(rawSizeStr);
                }
            } catch (org.openqa.selenium.NoSuchElementException e) {

                if (name.endsWith("(HOT)")) {
                    volume = 473;
                } else {
                    volume = 710;
                }
            }

            double sugar = 0, calories = 0, caffeine = 0;
            List<WebElement> nutritionItems = hoverElement.findElements(By.cssSelector(".ingredient_table li"));

            if (nutritionItems.isEmpty()) return null;

            for (WebElement nutritionItem : nutritionItems) {
                String label = nutritionItem.findElement(By.xpath("./div[1]")).getAttribute("textContent");
                String value = nutritionItem.findElement(By.xpath("./div[2]")).getAttribute("textContent");

                if(label == null) continue;
                if(label.contains("카페인")) caffeine = parseNutritionValuePer100ml(value, volume);
                if(label.contains("칼로리")) calories = parseNutritionValuePer100ml(value, volume);
                if(label.contains("당류")) sugar = parseNutritionValuePer100ml(value, volume);
            }

            BeverageCategory beverageCategory = determineBeverageCategory(category, name);

            System.out.println("### 추출 음료: 빽다방 || "+name);

            Beverage beverage = Beverage.builder()
                    .name(name)
                    .brand("빽다방")
                    .imgUrl(imgUrl)
                    .category(beverageCategory)
                    .sugar(sugar)
                    .calories(calories)
                    .caffeine(caffeine)
                    .consumeCount(0)
                    .status(Status.ACTIVE)
                    .build();

            createBeverageSizes(beverage, volume);
            return beverage;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createBeverageSizes(Beverage beverage, int volume) {
        switch (volume) {
            case 60:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "one size", "60ml", 60));
                break;
            case 473:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "one size", "16oz", 473));
                break;
            case 710:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "one size", "24oz", 710));
                break;
            case 946:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "one size", "32oz", 946));
                break;
            default:
                break;
        }
    }

    private int parseVolume(String rawSizeStr) {
        try {
            if(rawSizeStr.endsWith("ml")) {
                return Integer.parseInt(rawSizeStr.replaceAll("[^0-9]", ""));
            }else{
                int oz = Integer.parseInt(rawSizeStr.replaceAll("[^0-9]", ""));
                return (int) Math.round(oz * 29.57353); // 온스(oz) -> ML 변환
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing volume: " + e.getMessage());
            return 0;
        }
    }

    private double parseNutritionValuePer100ml(String value, int volume) {
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", "")) * 100 / volume;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private BeverageCategory determineBeverageCategory(String category, String name) {
        if (category.contains("커피")) {
            return BeverageCategory.커피;
        } else if (category.contains("음료")) {
            return BeverageCategory.음료;
        } else if (category.contains("빽스치노")) {
            return BeverageCategory.시그니쳐;
        } else {
            return inferCategoryFromName(name);
        }
    }

    private BeverageCategory inferCategoryFromName(String name) {
        if (name.contains("커피")) {
            return BeverageCategory.커피;
        } else {
            return BeverageCategory.기타;
        }
    }
}
