package com.sweetbalance.crawling_component.crawler;

import com.sweetbalance.crawling_component.entity.Beverage;
import com.sweetbalance.crawling_component.entity.BeverageSize;
import com.sweetbalance.crawling_component.enums.beverage.BeverageCategory;
import com.sweetbalance.crawling_component.enums.common.Status;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class StarbucksCrawler extends BaseCrawler {

    @Value("${spring.selenium.base-url.starbucks.base}")
    private String BASE_URL;

    @Value("${spring.selenium.base-url.starbucks.view}")
    private String VIEW_URL;

    @Override
    public List<Beverage> crawlBeverageList() {
        List<Beverage> results = new ArrayList<>();
        try {
            navigateTo(BASE_URL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            List<String> prodNumbers = extractProdNumbers(wait);

            for (String prodNumber : prodNumbers) {
                Beverage beverage = crawlBeverageDetail(prodNumber);
                if (beverage != null) {
                    results.add(beverage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    // BASE 페이지에서 각각의 음료에 해당하는 prod 값 얻어와서 리스트 반환
    private List<String> extractProdNumbers(WebDriverWait wait) {
        List<String> prodNumbers = new ArrayList<>();
        WebElement productList = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("/html/body/div[3]/div[7]/div[2]/div[2]/div/dl/dd[1]/div[1]")
        ));

        List<WebElement> categories = productList.findElements(By.tagName("dt"));
        List<WebElement> productSections = productList.findElements(By.tagName("dd"));

        for (int i = 0; i < categories.size(); i++) {
            List<WebElement> products = productSections.get(i).findElements(By.cssSelector("li.menuDataSet"));
            for (WebElement product : products) {
                WebElement link = product.findElement(By.cssSelector("a.goDrinkView"));
                String prodNumber = link.getAttribute("prod");
                prodNumbers.add(prodNumber);
            }
        }
        return prodNumbers;
    }

    // prod 값 각각에 해당하는 URL 이동, 해당 음료의 데이터 크롤링
    private Beverage crawlBeverageDetail(String prodNumber) {
        try {
            navigateTo(VIEW_URL + "?product_cd=" + prodNumber);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            WebElement productViewWrap = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("/html/body/div[3]/div[7]/div[2]/div[1]")
            ));

            String category = extractCategory(wait);
            String imgUrl = productViewWrap.findElement(By.cssSelector("img.zoomImg")).getAttribute("src");
            String fullName = productViewWrap.findElement(By.tagName("h4")).getText();
            String name = fullName.split("\n")[0].trim();

            String sizeInfo = productViewWrap.findElement(By.id("product_info01"))
                    .findElement(By.tagName("b")).getAttribute("textContent");

            int volume = parseVolume(sizeInfo);
            if(volume == 887) name+="(TRENTA)";

            WebElement nutritionInfo = productViewWrap.findElement(By.className("product_info_content"));
            double calories = parseNutritionValue(nutritionInfo, "kcal", volume);
            double sugar = parseNutritionValue(nutritionInfo, "sugars", volume);
            double caffeine = parseNutritionValue(nutritionInfo, "caffeine", volume);

            if (calories == -1 || sugar == -1 || caffeine == -1) {
                return null;
            }

            System.out.println("### 추출 음료: 스타벅스 || "+name);
;
            Beverage beverage = Beverage.builder()
                    .name(name)
                    .brand("스타벅스")
                    .imgUrl(imgUrl)
                    .category(determineBeverageCategory(category, name))
                    .calories(calories)
                    .sugar(sugar)
                    .caffeine(caffeine)
                    .consumeCount(0)
                    .status(Status.ACTIVE)
                    .build();

            createBeverageSizes(beverage, volume);
            return beverage;

        } catch (Exception e) {
            System.err.println("Error crawling detail for product " + prodNumber + ": " + e.getMessage());
            return null;
        }
    }

    // volume 값을 통해 BeverageSize 설정
    private void createBeverageSizes(Beverage beverage, int volume) {
        switch (volume) {
            case 22:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "SOLO", 22));
                break;
            case 190:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "JUICE_190ML", 190));
                break;
            case 207:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "TWO_SHOT", 207));
                break;
            case 355:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "TALL", 355));
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "GRANDE", 473));
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "VENTI", 591));
                break;
            case 473:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "GRANDE", 473));
                break;
            case 500:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "BOTTLE", 500));
                break;
            case 591:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "JUICE_591ML", 591));
                break;
            case 887:
                beverage.addSize(BeverageSize.fromBeverageAndVolume(beverage, "TRENTA", 887));
                break;
            default:
                break;
        }
    }

    private int parseVolume(String sizeInfo) {
        try {
            return Integer.parseInt(sizeInfo.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            System.err.println("Error parsing volume: " + e.getMessage() + ", 기본값 355ml 적용");
            return 355;
        }
    }

    private double parseNutritionValue(WebElement nutritionInfo, String nutrientClass, int volume) {
        try {
            WebElement nutrientElement = nutritionInfo.findElement(By.className(nutrientClass));
            String value = nutrientElement.findElement(By.tagName("dd")).getAttribute("textContent").trim();
            return Double.parseDouble(value) * 100 / volume;
        } catch (Exception e) {
            System.err.println("Error parsing " + nutrientClass + ": " + e.getMessage());
            return -1;
        }
    }

    private String extractCategory(WebDriverWait wait) {
        WebElement categoryElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("/html/body/div[3]/div[7]/div[1]/div/ul/li[7]/a")
        ));
        return categoryElement.getAttribute("textContent");
    }

    private BeverageCategory determineBeverageCategory(String category, String name) {
        if (category.contains("콜드 브루") || category.contains("브루드") || category.contains("에스프레소")) {
            return BeverageCategory.커피;
        } else if (category.contains("프라푸치노")|| category.contains("블렌디드") || category.contains("티") || category.contains("주스") ) {
            return BeverageCategory.음료;
        } else if (category.contains("리프레셔") || category.contains("피지오") || category.contains("제조")) {
            return BeverageCategory.시그니처;
        } else if (category.contains("프로모션")) {
            return BeverageCategory.기타;
        } else {
            return inferCategoryFromName(name);
        }
    }

    private BeverageCategory inferCategoryFromName(String name) {
        if (name.contains("콜드 브루") || name.contains("라떼")) {
            return BeverageCategory.커피;
        } else if (name.endsWith("티") || name.contains("말차")) {
            return BeverageCategory.음료;
        } else {
            return BeverageCategory.기타;
        }
    }
}
