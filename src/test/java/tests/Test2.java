package tests;

import browser.Browser;
import org.testng.annotations.Test;

public class Test2 extends Browser {
    @Test
    void demo(){
        launch();
        navigate("https://mail.google.com/");
    }
}
