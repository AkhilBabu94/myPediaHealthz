package MyRunner;

import Driver.TestBase;
import Utils.TestUtils;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.detectlanguage.Result;
import com.detectlanguage.errors.APIError;
import cucumber.api.CucumberOptions;
import cucumber.api.testng.TestNGCucumberRunner;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.openqa.selenium.*;
import com.detectlanguage.DetectLanguage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

@CucumberOptions(
        features = "src/test/resources/Features",
        glue = {"stepDefinitions"},
        monochrome = true,
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber-pretty",
                "json:target/cucumber-reports/CucumberTestReport.json",
                "rerun:target/cucumber-reports/rerun.txt"
        }
)

public class TestRunner extends TestBase {
    private TestNGCucumberRunner testNGCucumberRunner;
    ExtentTest test;
    ExtentReports extent;

    @BeforeSuite(alwaysRun = true)
    public void setUpClass() throws Exception {
        testNGCucumberRunner = new TestNGCucumberRunner(this.getClass());
        TestBase.initialization();
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(System.getProperty("user.dir") +"/test-output/extent.html");
        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        test = extent.createTest("Automation UI/API", "API and UI automation");
    }

    @Test(dataProvider = "dataset")
    public void myPediaFunction(String healthurl,String eulaurl, String tenant) throws APIError, AWTException, InterruptedException {

        Response healthResponse = given()
                            .when()
                                .get(healthurl);
        Assert.assertEquals(healthResponse.getStatusCode(),200,"The status code is not as expected");
        test.log(Status.INFO,"The status code is "+healthResponse.getStatusCode());

        Response eulaResponse = given().queryParam("tenant",tenant)
                            .when()
                                .get(eulaurl);

        if(healthResponse.getStatusCode()==200){
            System.out.println("The EULA for the tenant "+ tenant+ " is " +eulaResponse.getBody().jsonPath().get("eula_b64"));
            test.log(Status.INFO,"The EULA for the tenant "+ tenant+ " is " +eulaResponse.getBody().jsonPath().get("eula_b64"));
        }
        Assert.assertNotNull(eulaResponse.getBody().asString(),"The body is NULL");


        driver.get("https://www.mypedia.pearson.com/login");
        driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_ENTER);


        WebDriverWait wait = new WebDriverWait(driver,20);

//        click on language dropdown
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='main']/div/div[1]/div/div/div[1]/div[2]")));
        WebElement languageDropdown = driver.findElement(By.xpath("//*[@id='main']/div/div[1]/div/div/div[1]/div[2]"));
        languageDropdown.click();

        List<WebElement> dropdownLanguages = driver.findElement(By.xpath("//div[contains(@role,'menu')]")).findElements(By.xpath("div"));
        System.out.println("the number of dropdown languages are "+dropdownLanguages.size());
        Assert.assertTrue(dropdownLanguages.size()>=3,"There are less than 3 languages available");
        test.log(Status.PASS,"There are atleast 3 languages available");

//        validate language selected and language of continue present in UI screen
        validateSelectedLanguageIsEqualToLanguageOfContinue(1);
        languageDropdown.click();
        validateSelectedLanguageIsEqualToLanguageOfContinue(2);
        languageDropdown.click();
        validateSelectedLanguageIsEqualToLanguageOfContinue(0);

        driver.findElement(By.xpath("//a[@href='#'][contains(.,'Set up parent support')]")).click();
        waitTillElementIsAvailable(By.xpath("(//div[contains(.,'CREATE A NEW ACCOUNT')])[11]"));
        driver.findElement(By.xpath("(//div[contains(.,'CREATE A NEW ACCOUNT')])[11]")).click();

    }

    @Test(dataProvider = "datasetForCreatingAcc",dependsOnMethods = "myPediaFunction")
    public void addvaluesForNewRegistrationAndValidateCreateButtonIsDisabled(String firstNameVal,String lastNameVal,String emailIdVal,String parentUserNameVal,String parentPasswordVal,String parentPasswordConfirmVal) throws InterruptedException {

        waitTillElementIsClickable(By.xpath("(//input[contains(@type,'text')])[1]"));
        WebElement firstName = driver.findElement(By.xpath("(//input[contains(@type,'text')])[1]"));
        WebElement lastName = driver.findElement(By.xpath("(//input[contains(@type,'text')])[2]"));
        WebElement emailId = driver.findElement(By.xpath("(//input[contains(@type,'text')])[3]"));
        WebElement parentUserName = driver.findElement(By.xpath("(//input[contains(@type,'text')])[4]"));
        WebElement parentPassword = driver.findElement(By.xpath("(//input[contains(@type,'password')])[1]"));
        WebElement parentConfirmPassword = driver.findElement(By.xpath("(//input[contains(@type,'password')])[2]"));

        //        Input values for new registration
        firstName.sendKeys(firstNameVal);
        lastName.sendKeys(lastNameVal);
        emailId.sendKeys(emailIdVal);
        parentUserName.sendKeys(parentUserNameVal);
        parentPassword.sendKeys(parentPasswordVal);
        parentConfirmPassword.sendKeys(parentPasswordConfirmVal);

//        validate create button is disabled
        WebElement createAccButton = driver.findElement(By.xpath("//span[contains(.,'CREATE ACCOUNT')]"));
        Assert.assertNotEquals(createAccButton.getCssValue("opacity"), "1", "The create button is enabled");

        test.log(Status.PASS,"The create button is disabled");

    }

    public void validateSelectedLanguageIsEqualToLanguageOfContinue(int i) throws APIError, InterruptedException {
        List<WebElement> dropdownLanguages = driver.findElement(By.xpath("//div[contains(@role,'menu')]")).findElements(By.xpath("div"));
        WebElement Language = dropdownLanguages.get(i).findElement(By.xpath("span/div/div/div"));
        String LangText = Language.getText();
        Language.click();
        Thread.sleep(3000);
        WebElement ContinueButton = driver.findElement(By.xpath("//*[@id='SI_0005']/div/div"));
        validateBothStringAreOfSameLanguage(ContinueButton.getText(),LangText);
    }

    public void waitTillElementIsClickable(By element){
        int i=0;
        while(i<5) {
            try {
                Thread.sleep(500);
                WebDriverWait wait = new WebDriverWait(driver, 5);
                wait.until(ExpectedConditions.elementToBeClickable(element));
            } catch (NoSuchElementException | InterruptedException | TimeoutException e) {
            }
            i++;
        }
    }

    public void waitTillElementIsAvailable(By element){
        int i=0;
        while(i<5) {
            try {
                Thread.sleep(1000);
                WebDriverWait wait = new WebDriverWait(driver, 5);
                wait.until(ExpectedConditions.visibilityOfElementLocated(element));
            } catch (NoSuchElementException | InterruptedException | TimeoutException e) {
            }
            i++;
        }
    }

    public void validateBothStringAreOfSameLanguage(String text1,String text2) throws APIError {
        DetectLanguage.apiKey = "c25114012eeeb8e8cd72c72a2a6a9733";
        List<Result> results1 = DetectLanguage.detect(text1);
        Result result1 = results1.get(0);
        String result1Lang= result1.language;
        System.out.println("First string is "+text1+" and language is " +result1Lang );
        List<Result> results2 = DetectLanguage.detect(text2);
        Result result2 = results2.get(0);
        String result2Lang= result2.language;
        System.out.println("Second string is "+text2+" and language is " + result2.language);
        Assert.assertEquals(result1Lang,result2Lang,"Both languages are not same");
    }

    public void highlightElement(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].setAttribute('style', 'background: yellow; border: 2px solid red;');", element);
    }

    @DataProvider
    public Object[][] dataset() {
        Object[][] testObjArray = TestUtils.getTestData(System.getProperty("user.dir") +"/TestData.xlsx","Sheet1");
        return (testObjArray);
    }

    @DataProvider
    public Object[][] datasetForCreatingAcc() {
        Object[][] testObjArray = TestUtils.getTestData(System.getProperty("user.dir") +"/TestData1.xlsx","Sheet1");
        return (testObjArray);
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownClass() throws Exception {
        extent.flush();
        testNGCucumberRunner.finish();
        TestBase.quitDriver();
    }
}