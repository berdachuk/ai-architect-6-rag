package com.berdachuk.docurag.documents.api;

import java.util.List;
import java.util.Optional;

public interface DocumentCatalogApi {

    List<SourceDocumentSummary> listDocuments(int page, int pageSize);

    long countDocuments();

    Optional<SourceDocumentDetail> getDocument(String id);

    List<String> listCategories();
}
