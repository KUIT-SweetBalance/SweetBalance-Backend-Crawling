package com.sweetbalance.crawling_component.crawler;

import com.sweetbalance.crawling_component.entity.Beverage;
import com.sweetbalance.crawling_component.entity.BeverageSize;
import com.sweetbalance.crawling_component.enums.beverage.BeverageCategory;
import com.sweetbalance.crawling_component.enums.common.Status;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class MegaCrawler extends BaseCrawler {

    @Value("${spring.selenium.base-url.mega}")
    private String BASE_URL;

    @Override
    public List<Beverage> crawlBeverageList() {
        List<Beverage> results = new ArrayList<>();
        try {
            navigateTo(BASE_URL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 전체 상품 체크박스 해제
            WebElement allProductsCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[@class='checkbox_wrap']/label/input[@name='list_checkbox_all']")));
            if (allProductsCheckbox.isSelected()) {
                js.executeScript("arguments[0].click();", allProductsCheckbox);
            }

            // 카테고리 체크박스 찾기
            List<WebElement> categoryCheckboxes = driver.findElements(By.xpath("//div[@class='checkbox_wrap list_checkbox']"));

            for (WebElement categoryCheckbox : categoryCheckboxes) {
                // 현재 카테고리 체크박스 선택
                WebElement checkboxInput = categoryCheckbox.findElement(By.tagName("input"));
                js.executeScript("arguments[0].click();", checkboxInput);

                // 카테고리 이름 가져오기
                String categoryName = categoryCheckbox.findElement(By.xpath(".//div[@class='checkbox_text']")).getText().trim();

                // 해당 카테고리의 음료 크롤링
                results.addAll(crawlCategoryBeverages(categoryName));

                // 현재 카테고리 체크박스 해제
                js.executeScript("arguments[0].click();", checkboxInput);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private void uncheckPreviousCategory() {
        List<WebElement> checkedBoxes = driver.findElements(By.xpath("//div[@class='checkbox_wrap list_checkbox']//input[@checked]"));
        for (WebElement checkedBox : checkedBoxes) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkedBox);
        }
    }

    private List<Beverage> crawlCategoryBeverages(String categoryName) throws InterruptedException {
        List<Beverage> categoryResults = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        boolean hasNextPage = true;
        while (hasNextPage) {
            // 음료 항목 대기 및 추출
            List<WebElement> beverageItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.xpath("/html/body/div[3]/div[3]/div/div[3]/div[2]/div[4]/div[1]/ul/div[1]/ul/li")
            ));

            for (WebElement item : beverageItems) {
                Beverage beverage = extractBeverageInfo(item, categoryName);
                if (beverage != null) {
                    categoryResults.add(beverage);
                }
            }

            // 다음 페이지 버튼 찾기
            WebElement nextPageLink = null;
            try {
                nextPageLink = driver.findElement(By.xpath("//a[@class='board_page_next board_page_link']"));
            } catch (org.openqa.selenium.NoSuchElementException e) {
                // 다음 페이지 버튼이 없으면 크롤링 종료
                hasNextPage = false;
                continue;
            }

            // 다음 페이지로 이동
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPageLink);
            Thread.sleep(500); // 페이지 로딩 대기
        }

        return categoryResults;
    }

    private Beverage extractBeverageInfo(WebElement item, String categoryName) {
        try {
            String name = item.findElement(By.xpath(".//div[@class='cont_text_inner cont_text_title']/b")).getAttribute("textContent");

            // HOT 또는 ICE 라벨 파싱
            String temperatureLabel = "";
            try {
                temperatureLabel = item.findElement(By.xpath(".//div[contains(@class, 'cont_gallery_list_label')]")).getText();
                name += " [" + temperatureLabel + "]";
            } catch (org.openqa.selenium.NoSuchElementException e) {
                // 라벨이 없는 경우 무시
            }

            String imgSrc = item.findElement(By.xpath(".//img")).getAttribute("src");
            String sizeType = item.findElement(By.xpath(".//div[@class='cont_text']/div[@class='cont_text_inner'][1]")).getAttribute("textContent").trim();

            int volume = parseVolume(sizeType);

            String caloriesText = item.findElement(By.xpath(".//div[@class='cont_text']/div[@class='cont_text_inner'][2]")).getAttribute("textContent")
                    .replaceAll("1회 제공량", "");
            double calories = parseNutritionValuePer100ml(caloriesText, volume);

            String sugarText = item.findElement(By.xpath(".//ul/li[contains(text(), '당류')]")).getAttribute("textContent");
            double sugar = parseNutritionValuePer100ml(sugarText, volume);

            String caffeineText = item.findElement(By.xpath(".//ul/li[contains(text(), '카페인')]")).getAttribute("textContent");
            double caffeine = parseNutritionValuePer100ml(caffeineText, volume);

            System.out.println("### 추출 음료: 메가커피 || "+name);

            Beverage beverage = Beverage.builder()
                    .name(name)
                    .brand("메가커피")
                    .imgUrl(imgSrc)
                    .category(determineBeverageCategory(categoryName, name))
                    .calories(calories)
                    .sugar(sugar)
                    .caffeine(caffeine)
                    .consumeCount(0)
                    .status(Status.ACTIVE)
                    .build();

            BeverageSize size = BeverageSize.fromBeverageAndVolume(beverage, sizeType, volume);
            beverage.addSize(size);

            return beverage;
        } catch (Exception e) {
            System.err.println("Error extracting beverage info: " + e.getMessage());
            return null;
        }
    }

    private int parseVolume(String sizeType) {
        try {
            int oz = Integer.parseInt(sizeType.replaceAll("[^0-9]", ""));
            return (int) Math.round(oz * 29.57353); // 온스(oz) -> ML 변환
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

    private BeverageCategory determineBeverageCategory(String categoryName, String name) {

        if (categoryName.contains("커피") || categoryName.contains("디카페인")) {
            return BeverageCategory.커피;
        } else if (categoryName.contains("티") || categoryName.contains("에이드&주스")
                || categoryName.contains("스무디&프라페") || categoryName.contains("음료")) {
            return BeverageCategory.음료;
        } else if (categoryName.contains("신상품")) {
            return BeverageCategory.기타;
        }  else {
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
