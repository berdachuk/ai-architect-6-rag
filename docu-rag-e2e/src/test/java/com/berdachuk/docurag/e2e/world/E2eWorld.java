package com.berdachuk.docurag.e2e.world;

import com.berdachuk.docurag.e2e.support.DocuRagClientFactory;

import java.nio.file.Path;

/**
 * Shared E2E state across Cucumber scenarios (single-threaded suite).
 */
public final class E2eWorld {

    private static volatile DocuRagClientFactory clientFactory;
    private static volatile String fixtureJsonlPath;
    private static volatile String fixturePdfPath;
    private static volatile Path appJarPath;
    private static volatile String javaBinary;
    private static volatile int appPort = 18080;
    private static volatile int pgPort = 5433;
    private static volatile String uiBaseUrl;

    private E2eWorld() {
    }

    public static void resetScenarioState() {
        // reserved if we split scenarios with clean state later
    }

    public static void setClientFactory(DocuRagClientFactory factory) {
        clientFactory = factory;
    }

    public static DocuRagClientFactory clients() {
        if (clientFactory == null) {
            throw new IllegalStateException("E2E client factory not initialized (infrastructure extension did not run?)");
        }
        return clientFactory;
    }

    public static void setFixtureJsonlPath(String path) {
        fixtureJsonlPath = path;
    }

    public static String fixtureJsonlPath() {
        if (fixtureJsonlPath == null) {
            throw new IllegalStateException("fixture jsonl path not set");
        }
        return fixtureJsonlPath;
    }

    public static void setFixturePdfPath(String path) {
        fixturePdfPath = path;
    }

    public static String fixturePdfPath() {
        if (fixturePdfPath == null) {
            throw new IllegalStateException("fixture pdf path not set");
        }
        return fixturePdfPath;
    }

    public static void setAppJarPath(Path path) {
        appJarPath = path;
    }

    public static Path appJarPath() {
        if (appJarPath == null) {
            throw new IllegalStateException("app jar path not set");
        }
        return appJarPath;
    }

    public static void setJavaBinary(String java) {
        javaBinary = java;
    }

    public static String javaBinary() {
        if (javaBinary == null) {
            return "java";
        }
        return javaBinary;
    }

    public static void setAppPort(int port) {
        appPort = port;
    }

    public static int appPort() {
        return appPort;
    }

    public static void setPgPort(int port) {
        pgPort = port;
    }

    public static int pgPort() {
        return pgPort;
    }

    public static void setUiBaseUrl(String url) {
        uiBaseUrl = url;
    }

    public static String uiBaseUrl() {
        if (uiBaseUrl == null) {
            return "http://127.0.0.1:" + appPort();
        }
        return uiBaseUrl;
    }

    public static String apiBaseUrl() {
        return "http://127.0.0.1:" + appPort();
    }
}
