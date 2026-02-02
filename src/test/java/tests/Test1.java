package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.testng.annotations.Test;
import pages.KlekiDraw;

public class Test1 {
    @Test
    public static void Page1() throws Exception {
        KlekiDraw.draw();
    }
}
