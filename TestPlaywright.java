import com.microsoft.playwright.*;
public class TestPlaywright {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
            Browser browser = playwright.chromium().launch(options);
            System.out.println("Success!");
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
