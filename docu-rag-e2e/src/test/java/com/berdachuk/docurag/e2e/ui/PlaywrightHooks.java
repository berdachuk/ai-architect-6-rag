package com.berdachuk.docurag.e2e.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;
import com.microsoft.playwright.options.WaitUntilState;
import com.berdachuk.docurag.e2e.world.E2eWorld;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class PlaywrightHooks {

    private final PlaywrightContext ctx;

    public PlaywrightHooks(PlaywrightContext ctx) {
        this.ctx = ctx;
    }

    private static boolean headless() {
        return !"false".equalsIgnoreCase(System.getProperty("e2e.playwright.headless", "true"));
    }

    @Before(value = "@ui", order = 10)
    public void beforeUi(Scenario scenario) {
        Playwright pw = Playwright.create();
        Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless()));
        var bctx = browser.newContext(
                new Browser.NewContextOptions().setViewportSize(new ViewportSize(1280, 720)));
        Page page = bctx.newPage();
        page.setDefaultNavigationTimeout(90_000);
        page.setDefaultTimeout(90_000);
        ctx.setPlaywright(pw);
        ctx.setBrowser(browser);
        ctx.setBrowserContext(bctx);
        ctx.setPage(page);
    }

    @After(value = "@ui", order = 10)
    public void afterUi(Scenario scenario) {
        if (scenario.isFailed() && "true".equalsIgnoreCase(System.getProperty("e2e.playwright.screenshot-on-failure", "true"))) {
            Page page = ctx.getPage();
            if (page != null) {
                try {
                    byte[] shot = page.screenshot();
                    scenario.attach(shot, "image/png", "failure");
                } catch (Exception ignored) {
                }
            }
        }
        ctx.close();
    }

    public static String uiBase() {
        return System.getProperty("e2e.ui.base.url", E2eWorld.uiBaseUrl());
    }

    /** Avoid hanging on `load` when auxiliary requests (fonts, etc.) never settle. */
    public static WaitUntilState navigationWaitUntil() {
        return WaitUntilState.DOMCONTENTLOADED;
    }
}
