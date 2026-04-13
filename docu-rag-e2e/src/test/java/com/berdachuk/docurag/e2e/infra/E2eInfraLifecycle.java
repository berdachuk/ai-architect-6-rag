package com.berdachuk.docurag.e2e.infra;

import com.berdachuk.docurag.e2e.support.DocuRagClientFactory;
import com.berdachuk.docurag.e2e.world.E2eWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * One-shot startup for Cucumber E2E: Compose, fat JAR, health wait. Tear down via {@link #stopQuietly()} (also
 * registered as a JVM shutdown hook).
 */
public final class E2eInfraLifecycle {

    private static final Logger log = LoggerFactory.getLogger(E2eInfraLifecycle.class);

    private static final Object LOCK = new Object();

    private static final String LOOPBACK = "127.0.0.1";

    private static volatile Process appProcess;
    private static volatile boolean composeStarted;
    private static volatile Path composeDirectory;
    private static volatile boolean shutdownHookRegistered;
    /** Cached: whether {@code docker compose} (Compose V2 plugin) responds successfully. */
    private static volatile Boolean dockerComposePluginOk;

    private E2eInfraLifecycle() {
    }

    private static int composeUpTimeoutMinutes() {
        String p = System.getProperty("e2e.compose.up.timeoutMinutes");
        if (p == null || p.isBlank()) {
            return 20;
        }
        return Integer.parseInt(p.trim());
    }

    private static int intProperty(String key, int defaultValue) {
        String p = System.getProperty(key);
        if (p == null || p.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(p.trim());
    }

    /**
     * Fail fast if the Docker daemon is not reachable (instead of hanging until compose up times out).
     */
    private static void requireDockerDaemon() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("docker", "info");
        pb.redirectErrorStream(true);
        File sink = Path.of("target", "docurag-e2e-docker-info.log").toAbsolutePath().toFile();
        Files.createDirectories(sink.getParentFile().toPath());
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(sink));
        Process p = pb.start();
        if (!p.waitFor(45, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException(
                    "docker info timed out - is Docker running? See " + sink.toPath().toAbsolutePath());
        }
        if (p.exitValue() != 0) {
            throw new IllegalStateException(
                    "Docker daemon not usable (docker info exit " + p.exitValue()
                            + "). Start Docker Desktop / dockerd, then retry. Log: " + sink.toPath().toAbsolutePath());
        }
    }

    private static void redirectComposeLogs(ProcessBuilder pb) throws IOException {
        Path log = Path.of("target", "docurag-e2e-compose.log").toAbsolutePath();
        Files.createDirectories(log.getParent());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
    }

    /**
     * Ensures Compose Postgres is up before spawning a second JVM (eval-cli). The main app JVM does not share DB
     * health with child processes; a prior compose blip or race can otherwise yield connection refused on 5433.
     */
    public static void ensurePostgresForChildJvmCli() throws Exception {
        Path dir = Path.of(System.getProperty("e2e.compose.dir", defaultComposeDir())).toAbsolutePath().normalize();
        composeDirectory = dir;
        int pg = intProperty("e2e.pg.port", 5433);
        requireDockerDaemon();
        runDockerComposeUp();
        composeStarted = true;
        waitPortOpen(LOOPBACK, pg, Duration.ofMinutes(2));
        waitPostgresReady(dir, Duration.ofMinutes(3));
    }

    public static void ensureStarted() throws Exception {
        if (appProcess != null && appProcess.isAlive()) {
            return;
        }
        synchronized (LOCK) {
            if (appProcess != null && appProcess.isAlive()) {
                return;
            }
            doStart();
        }
    }

    private static void doStart() throws Exception {
        composeDirectory = Path.of(System.getProperty("e2e.compose.dir", defaultComposeDir())).toAbsolutePath().normalize();
        Path jar = Path.of(System.getProperty("e2e.app.jar", defaultJarPath())).toAbsolutePath().normalize();
        int port = intProperty("e2e.app.port", 18080);
        int pg = intProperty("e2e.pg.port", 5433);

        if (!Files.exists(jar)) {
            throw new IllegalStateException(
                    "DocuRAG JAR not found: " + jar + ". Build the app first: mvn -f ../docu-rag-parent verify (or package docu-rag)");
        }

        E2eWorld.setAppPort(port);
        E2eWorld.setPgPort(pg);
        E2eWorld.setAppJarPath(jar);
        E2eWorld.setJavaBinary(resolveJavaBinary());
        E2eWorld.setUiBaseUrl("http://" + LOOPBACK + ":" + port);

        Path fixturesDir = Path.of("target", "e2e-fixtures").toAbsolutePath();
        Files.createDirectories(fixturesDir);
        Path jsonlSrc = composeDirectory.resolve("src/test/resources/fixtures/sample.jsonl");
        if (!Files.exists(jsonlSrc)) {
            throw new IllegalStateException("Missing fixture: " + jsonlSrc);
        }
        Path jsonlDest = fixturesDir.resolve("sample.jsonl");
        Files.copy(jsonlSrc, jsonlDest, StandardCopyOption.REPLACE_EXISTING);
        E2eWorld.setFixtureJsonlPath(jsonlDest.toAbsolutePath().toString());

        try (InputStream in = E2eInfraLifecycle.class.getResourceAsStream("/fixtures/tiny.pdf")) {
            if (in == null) {
                throw new IllegalStateException("Classpath resource /fixtures/tiny.pdf missing");
            }
            Path pdfDest = fixturesDir.resolve("tiny.pdf");
            Files.copy(in, pdfDest, StandardCopyOption.REPLACE_EXISTING);
            E2eWorld.setFixturePdfPath(pdfDest.toAbsolutePath().toString());
        }

        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(E2eInfraLifecycle::stopQuietly, "docurag-e2e-shutdown"));
            shutdownHookRegistered = true;
        }

        requireDockerDaemon();
        runDockerComposeUp();
        composeStarted = true;
        waitPortOpen(LOOPBACK, pg, Duration.ofMinutes(2));
        waitPostgresReady(composeDirectory, Duration.ofMinutes(3));

        Path logFile = Path.of("target", "docurag-e2e-app.log").toAbsolutePath();
        Files.createDirectories(logFile.getParent());

        List<String> cmd = new ArrayList<>();
        cmd.add(E2eWorld.javaBinary());
        cmd.add("-jar");
        cmd.add(jar.toString());
        cmd.add("--spring.profiles.active=e2e");
        cmd.add("--server.port=" + port);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("E2E_PG_HOST", LOOPBACK);
        pb.environment().put("E2E_PG_PORT", String.valueOf(pg));
        pb.environment().put("E2E_APP_PORT", String.valueOf(port));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        log.info("Starting DocuRAG: {}", String.join(" ", cmd));
        appProcess = pb.start();

        System.setProperty(DocuRagClientFactory.BASE_URL_PROPERTY, E2eWorld.apiBaseUrl());
        System.setProperty("e2e.ui.base.url", E2eWorld.uiBaseUrl());

        E2eWorld.setClientFactory(new DocuRagClientFactory(E2eWorld.apiBaseUrl()));
        waitHealthUp(Duration.ofMinutes(2));
        log.info("DocuRAG E2E infra ready at {}", E2eWorld.apiBaseUrl());
    }

    private static String defaultComposeDir() {
        return Path.of(System.getProperty("user.dir", ".")).resolve("../docu-rag").normalize().toString();
    }

    private static String defaultJarPath() {
        return Path.of(System.getProperty("user.dir", "."))
                .resolve("../docu-rag/target/docu-rag-0.1.0-SNAPSHOT.jar")
                .normalize()
                .toString();
    }

    private static String resolveJavaBinary() {
        String home = System.getenv("JAVA_HOME");
        if (home != null && !home.isBlank()) {
            Path bin = Path.of(home, "bin", isWindows() ? "java.exe" : "java");
            if (Files.isExecutable(bin)) {
                return bin.toAbsolutePath().toString();
            }
        }
        return "java";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Whether {@code docker compose} (V2 plugin) works. When false, use standalone {@code docker-compose} so Windows
     * installs without the plugin do not run a failing {@code docker} invocation first.
     */
    private static boolean dockerComposePluginAvailable() {
        if (dockerComposePluginOk != null) {
            return dockerComposePluginOk;
        }
        synchronized (LOCK) {
            if (dockerComposePluginOk != null) {
                return dockerComposePluginOk;
            }
            try {
                Path probeLog = Path.of("target", "docurag-e2e-compose-plugin-probe.log").toAbsolutePath();
                Files.createDirectories(probeLog.getParent());
                ProcessBuilder pb = new ProcessBuilder("docker", "compose", "version");
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(probeLog.toFile()));
                Process p = pb.start();
                boolean finished = p.waitFor(30, TimeUnit.SECONDS);
                boolean ok = finished && p.exitValue() == 0;
                dockerComposePluginOk = ok;
                if (!ok) {
                    log.info(
                            "docker compose plugin not available; using docker-compose for E2E (probe log: {})",
                            probeLog);
                }
                return ok;
            } catch (Exception e) {
                log.warn("docker compose plugin probe failed: {}", e.toString());
                dockerComposePluginOk = false;
                return false;
            }
        }
    }

    private static void runDockerComposeUp() throws IOException, InterruptedException {
        int composeUpMinutes = composeUpTimeoutMinutes();
        Path composeLog = Path.of("target", "docurag-e2e-compose.log").toAbsolutePath();

        if (dockerComposePluginAvailable()) {
            ProcessBuilder pb = newDockerCompose(composeDirectory, "up", "-d");
            redirectComposeLogs(pb);
            log.info("Running docker compose up -d (log: {})", composeLog);
            Process p = pb.start();
            if (!p.waitFor(composeUpMinutes, TimeUnit.MINUTES)) {
                p.destroyForcibly();
                throw new IllegalStateException(
                        "docker compose up timed out after " + composeUpMinutes + "m (first image pull can be slow). Log: "
                                + composeLog);
            }
            if (p.exitValue() != 0) {
                throw new IllegalStateException(
                        "docker compose up failed with exit " + p.exitValue() + ". See " + composeLog);
            }
            return;
        }

        ProcessBuilder legacy = new ProcessBuilder("docker-compose", "up", "-d");
        legacy.directory(composeDirectory.toFile());
        redirectComposeLogs(legacy);
        log.info("Running docker-compose up -d (log: {})", composeLog);
        Process p2 = legacy.start();
        if (!p2.waitFor(composeUpMinutes, TimeUnit.MINUTES)) {
            p2.destroyForcibly();
            throw new IllegalStateException("docker-compose up timed out after " + composeUpMinutes + "m. Log: " + composeLog);
        }
        if (p2.exitValue() != 0) {
            throw new IllegalStateException(
                    "docker-compose up failed with exit " + p2.exitValue() + ". See " + composeLog
                            + " and docurag-e2e-docker-info.log");
        }
    }

    private static ProcessBuilder newDockerCompose(Path dir, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("compose");
        for (String a : args) {
            cmd.add(a);
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        return pb;
    }

    private static boolean composeExec(Path composeDir, String... execArgs) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        if (dockerComposePluginAvailable()) {
            cmd.add("docker");
            cmd.add("compose");
        } else {
            cmd.add("docker-compose");
        }
        cmd.add("exec");
        cmd.add("-T");
        for (String a : execArgs) {
            cmd.add(a);
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(composeDir.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        return p.waitFor(45, TimeUnit.SECONDS) && p.exitValue() == 0;
    }

    /** TCP open is not enough; wait until Postgres accepts connections (avoids Flyway/Hikari racing a not-ready server). */
    private static void waitPostgresReady(Path composeDir, Duration timeout) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (composeExec(composeDir, "postgres", "pg_isready", "-U", "docurag", "-d", "docurag")) {
                log.info("Postgres is accepting connections (pg_isready)");
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException(
                "Timeout waiting for Postgres (pg_isready). See target/docurag-e2e-compose.log and compose service logs.");
    }

    private static void waitPortOpen(String host, int port, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 500);
                log.info("Port {}:{} is open", host, port);
                return;
            } catch (IOException e) {
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("Timeout waiting for " + host + ":" + port);
    }

    private static void waitHealthUp(Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        DocuRagClientFactory f = E2eWorld.clients();
        while (Instant.now().isBefore(deadline)) {
            if (appProcess != null && !appProcess.isAlive()) {
                throw new IllegalStateException(
                        "DocuRAG process exited with code " + appProcess.exitValue()
                                + " before health UP; see target/docurag-e2e-app.log");
            }
            try {
                Map<String, Object> h = f.actuator().getHealth();
                if (isUp(h)) {
                    return;
                }
                log.debug("Health not UP yet: {}", h);
            } catch (Exception e) {
                log.debug("Health poll: {}", e.toString());
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Timeout waiting for actuator health UP");
    }

    /**
     * Use the aggregate {@code status} only. A previous implementation could return true when the root was {@code DOWN}
     * but nested maps looked {@code UP}, which let E2E proceed before the database was actually usable.
     */
    private static boolean isUp(Map<String, Object> health) {
        return health != null && "UP".equals(health.get("status"));
    }

    public static void stopQuietly() {
        if (appProcess != null) {
            log.info("Stopping DocuRAG process");
            appProcess.destroy();
            try {
                if (!appProcess.waitFor(30, TimeUnit.SECONDS)) {
                    appProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                appProcess.destroyForcibly();
            }
            appProcess = null;
        }
        if (composeStarted && composeDirectory != null) {
            boolean rmVol = "true".equalsIgnoreCase(System.getProperty("e2e.compose.down.removeVolumes", "false"));
            if (dockerComposePluginAvailable()) {
                try {
                    ProcessBuilder pb = rmVol
                            ? newDockerCompose(composeDirectory, "down", "-v", "--remove-orphans")
                            : newDockerCompose(composeDirectory, "down", "--remove-orphans");
                    redirectComposeLogs(pb);
                    Process cp = pb.start();
                    if (!cp.waitFor(60, TimeUnit.SECONDS)) {
                        cp.destroyForcibly();
                    }
                } catch (Exception e) {
                    log.warn("docker compose down: {}", e.toString());
                }
            } else {
                try {
                    List<String> leg = new ArrayList<>();
                    leg.add("docker-compose");
                    leg.add("down");
                    leg.add("--remove-orphans");
                    if (rmVol) {
                        leg.add("-v");
                    }
                    ProcessBuilder legacy = new ProcessBuilder(leg);
                    legacy.directory(composeDirectory.toFile());
                    redirectComposeLogs(legacy);
                    Process p2 = legacy.start();
                    p2.waitFor(60, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("docker-compose down: {}", e.toString());
                }
            }
            if (dockerComposePluginAvailable()) {
                try {
                    List<String> leg = new ArrayList<>();
                    leg.add("docker-compose");
                    leg.add("down");
                    leg.add("--remove-orphans");
                    if (rmVol) {
                        leg.add("-v");
                    }
                    ProcessBuilder legacy = new ProcessBuilder(leg);
                    legacy.directory(composeDirectory.toFile());
                    redirectComposeLogs(legacy);
                    Process p2 = legacy.start();
                    p2.waitFor(30, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // best effort when both V2 plugin and standalone binary exist
                }
            }
            composeStarted = false;
        }
    }
}
