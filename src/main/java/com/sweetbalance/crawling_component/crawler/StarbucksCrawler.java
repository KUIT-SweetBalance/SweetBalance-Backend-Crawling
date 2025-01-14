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

    @Autowired
    public StarbucksCrawler(WebDriver driver) {
        super(driver);
    }

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
        } finally {
            quitDriver();
        }
        return results;
    }

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

            System.out.println("### 가져온 음료 데이터 이름: "+name);

            String sizeInfo = productViewWrap.findElement(By.id("product_info01"))
                    .findElement(By.tagName("b")).getAttribute("textContent");
            int volume = parseVolume(sizeInfo);

            if(volume == 887) name+="(TRENTA)";

            System.out.println("    가져온 음료 데이터 용량: "+volume);

            WebElement nutritionInfo = productViewWrap.findElement(By.className("product_info_content"));
            double calories = parseNutritionValue(nutritionInfo, "kcal", volume);
            double sugar = parseNutritionValue(nutritionInfo, "sugars", volume);
            double caffeine = parseNutritionValue(nutritionInfo, "caffeine", volume);

            // 영양 정보 파싱 중 예외가 발생하면 null을 반환
            if (calories == -1 || sugar == -1 || caffeine == -1) {
                return null;
            }
;
            Beverage beverage = Beverage.builder()
                    .name(name)
                    .brand("Starbucks")
                    .imgUrl(imgUrl)
                    .category(determineBeverageCategory(category, name))
                    .calories(calories)
                    .sugar(sugar)
                    .caffeine(caffeine)
                    .status(Status.ACTIVE)
                    .build();

            createBeverageSizes(beverage, volume);
            return beverage;

        } catch (Exception e) {
            System.err.println("Error crawling detail for product " + prodNumber + ": " + e.getMessage());
            return null;
        }
    }

    private void createBeverageSizes(Beverage beverage, int volume) {
        switch (volume) {
            case 22:
                beverage.addSize(new BeverageSize(beverage, "SOLO", 22));
                break;
            case 190:
                beverage.addSize(new BeverageSize(beverage, "JUICE_190ML", 190));
                break;
            case 207:
                beverage.addSize(new BeverageSize(beverage, "TWO_SHOT", 207));
                break;
            case 355:
                beverage.addSize(new BeverageSize(beverage, "TALL", 355));
                beverage.addSize(new BeverageSize(beverage, "GRANDE", 473));
                beverage.addSize(new BeverageSize(beverage, "VENTI", 591));
                break;
            case 473:
                beverage.addSize(new BeverageSize(beverage, "GRANDE", 473));
                break;
            case 500:
                beverage.addSize(new BeverageSize(beverage, "BOTTLE", 500));
                break;
            case 591:
                beverage.addSize(new BeverageSize(beverage, "JUICE_591ML", 591));
                break;
            case 887:
                beverage.addSize(new BeverageSize(beverage, "TRENTA", 887));
                break;
            default:
                break;
        }
    }

    private String extractCategory(WebDriverWait wait) {
        WebElement categoryElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("/html/body/div[3]/div[7]/div[1]/div/ul/li[7]/a")
        ));
        return categoryElement.getAttribute("textContent");
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

    private int parseVolume(String sizeInfo) {
        try {
            return Integer.parseInt(sizeInfo.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            System.err.println("Error parsing volume: " + e.getMessage() + ", 기본값 355ml 적용");
            return 355;
        }
    }

    private BeverageCategory determineBeverageCategory(String category, String name) {
        category = category.toLowerCase();
        if (category.contains("콜드 브루")) {
            return BeverageCategory.콜드브루;
        } else if (category.contains("브루드")) {
            return BeverageCategory.브루드커피;
        } else if (category.contains("아메리카노")) {
            return BeverageCategory.아메리카노;
        } else if (category.contains("에스프레소")) {
            return BeverageCategory.에스프레소;
        } else if (category.contains("티")) {
            return BeverageCategory.티;
        } else if (category.contains("주스") || category.contains("블렌디드") || category.contains("리프레셔") || category.contains("피지오")) {
            return BeverageCategory.주스;
        } else {
            return inferCategoryFromName(name);
        }
    }

    private BeverageCategory inferCategoryFromName(String name) {
        if (name.contains("콜드 브루")) {
            return BeverageCategory.콜드브루;
        } else if (name.endsWith("티") || name.contains("말차")) {
            return BeverageCategory.티;
        } else {
            return BeverageCategory.기타;
        }
    }
}
