package tests;


import browser.Browser;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import pages.KlekiDraw;

public class Test1 {

    static ExtentReports extent;
    static ExtentTest test;


    @BeforeTest
    public static void initialize(){
        extent = ExtentManager.getExtent();
        Browser.launch();
        Browser.navigate("https://kleki.com/");
    }

    @Test
    public static void Page1() throws Exception {

        test = extent.createTest("Draw on Kleki");

        boolean result = KlekiDraw.draw();
        test.info("Attempting to draw on canvas");

        Assert.assertTrue(result);
        test.pass("Drawing completed successfully");
    }


    @Test
    public static void captureSS(){

        test = extent.createTest("Capture Screenshot");

        boolean result = KlekiDraw.captureScreenshot();
        test.info("Capturing screenshot");

        Assert.assertTrue(result);
        test.pass("Screenshot captured successfully");
    }


    @AfterTest
    public static void endTest(){
        Browser.close();
        extent.flush();
    }
}
