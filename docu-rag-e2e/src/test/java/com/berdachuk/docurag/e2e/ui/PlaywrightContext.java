package com.berdachuk.docurag.e2e.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class PlaywrightContext {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page page;

    public Playwright getPlaywright() {
        return playwright;
    }

    public void setPlaywright(Playwright playwright) {
        this.playwright = playwright;
    }

    public Browser getBrowser() {
        return browser;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public BrowserContext getBrowserContext() {
        return browserContext;
    }

    public void setBrowserContext(BrowserContext browserContext) {
        this.browserContext = browserContext;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public void close() {
        if (page != null) {
            try {
                page.close();
            } catch (Exception ignored) {
            }
            page = null;
        }
        if (browserContext != null) {
            try {
                browserContext.close();
            } catch (Exception ignored) {
            }
            browserContext = null;
        }
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception ignored) {
            }
            browser = null;
        }
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception ignored) {
            }
            playwright = null;
        }
    }
}
