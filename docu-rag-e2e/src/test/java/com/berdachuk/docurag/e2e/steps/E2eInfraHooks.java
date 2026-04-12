package com.berdachuk.docurag.e2e.steps;

import com.berdachuk.docurag.e2e.infra.E2eInfraLifecycle;
import io.cucumber.java.Before;

/**
 * JUnit {@code @Suite} extensions do not run before the Cucumber engine, so infrastructure is started from this hook.
 */
public class E2eInfraHooks {

    @Before(order = Integer.MIN_VALUE)
    public void ensureInfra() throws Exception {
        E2eInfraLifecycle.ensureStarted();
    }
}
