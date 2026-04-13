package com.berdachuk.docurag.e2e.infra;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Optional JUnit 5 extension that delegates to {@link E2eInfraLifecycle}. Prefer {@link com.berdachuk.docurag.e2e.steps.E2eInfraHooks}
 * for Cucumber suites (suite-level extensions do not run before the Cucumber engine).
 */
public class E2eInfraExtension implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        E2eInfraLifecycle.ensureStarted();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        E2eInfraLifecycle.stopQuietly();
    }
}
