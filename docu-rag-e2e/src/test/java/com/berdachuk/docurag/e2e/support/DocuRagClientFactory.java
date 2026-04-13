package com.berdachuk.docurag.e2e.support;

import com.berdachuk.docurag.e2e.client.ApiClient;
import com.berdachuk.docurag.e2e.client.Configuration;
import com.berdachuk.docurag.e2e.client.api.ActuatorApi;
import com.berdachuk.docurag.e2e.client.api.DocumentsApi;
import com.berdachuk.docurag.e2e.client.api.EvaluationApi;
import com.berdachuk.docurag.e2e.client.api.IndexApi;
import com.berdachuk.docurag.e2e.client.api.RagApi;
import com.berdachuk.docurag.e2e.client.api.VisualizationsApi;

/**
 * Thin wrapper around the OpenAPI-generated {@link ApiClient} (okhttp-gson).
 * Base path defaults to {@value #DEFAULT_BASE_PATH} or system property {@code e2e.api.base.url}.
 */
public final class DocuRagClientFactory {

    public static final String DEFAULT_BASE_PATH = "http://127.0.0.1:18080";
    public static final String BASE_URL_PROPERTY = "e2e.api.base.url";

    private final ApiClient apiClient;

    public DocuRagClientFactory() {
        this(System.getProperty(BASE_URL_PROPERTY, DEFAULT_BASE_PATH));
    }

    public DocuRagClientFactory(String basePath) {
        this.apiClient = Configuration.getDefaultApiClient();
        this.apiClient.setBasePath(basePath);
    }

    public ApiClient apiClient() {
        return apiClient;
    }

    public DocumentsApi documents() {
        return new DocumentsApi(apiClient);
    }

    public IndexApi index() {
        return new IndexApi(apiClient);
    }

    public RagApi rag() {
        return new RagApi(apiClient);
    }

    public EvaluationApi evaluation() {
        return new EvaluationApi(apiClient);
    }

    public VisualizationsApi visualizations() {
        return new VisualizationsApi(apiClient);
    }

    public ActuatorApi actuator() {
        return new ActuatorApi(apiClient);
    }
}
