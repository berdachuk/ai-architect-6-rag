package com.berdachuk.docurag.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;

/**
 * Opens the dashboard ({@code /}) in the default browser once the app is ready (local dev only).
 */
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "docurag.local", name = "open-browser-on-start", havingValue = "true", matchIfMissing = true)
public class LocalHomeBrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(LocalHomeBrowserLauncher.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
        String homeUrl = "http://localhost:" + port + "/";

        if (GraphicsEnvironment.isHeadless()) {
            log.debug("Skipping browser launch (headless JVM). Home: {}", homeUrl);
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(homeUrl));
                log.info("Opened home page in browser: {}", homeUrl);
                return;
            }
        } catch (Exception e) {
            log.debug("Desktop.browse failed, trying OS fallback: {}", e.toString());
        }

        if (openWithOsFallback(homeUrl)) {
            log.info("Opened home page in browser: {}", homeUrl);
        } else {
            log.info("Open the home page manually: {}", homeUrl);
        }
    }

    private static boolean openWithOsFallback(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
                return true;
            }
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                return true;
            }
            new ProcessBuilder("xdg-open", url).start();
            return true;
        } catch (IOException e) {
            log.warn("Could not launch browser: {}", e.getMessage());
            return false;
        }
    }
}
