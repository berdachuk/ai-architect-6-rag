package com.berdachuk.docurag.e2e.support;

import com.berdachuk.docurag.e2e.client.ApiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures generated OpenAPI APIs compile and bind to a shared {@link ApiClient}.
 */
class DocuRagClientFactoryTest {

    @Test
    void factoryExposesAllGeneratedApis() {
        DocuRagClientFactory f = new DocuRagClientFactory("http://localhost:19999");
        assertThat(f.apiClient().getBasePath()).isEqualTo("http://localhost:19999");
        assertThat(f.documents()).isNotNull();
        assertThat(f.index()).isNotNull();
        assertThat(f.rag()).isNotNull();
        assertThat(f.evaluation()).isNotNull();
        assertThat(f.visualizations()).isNotNull();
        assertThat(f.actuator()).isNotNull();
    }
}
