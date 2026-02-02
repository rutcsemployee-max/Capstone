package locators;

import browser.Browser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class KlekiPage extends Browser {
    public static WebElement toolbar, canvas;
    public static void locate(){
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
        toolbar = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@class='kl-toolspace kl-toolspace--right']/div/div[5]/div[3]/div[1]/div/div")));
        canvas =  wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//canvas")));

    }
}
