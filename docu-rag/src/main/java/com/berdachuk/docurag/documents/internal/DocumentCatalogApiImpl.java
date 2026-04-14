package com.berdachuk.docurag.documents.internal;

import com.berdachuk.docurag.documents.api.DocumentCatalogApi;
import com.berdachuk.docurag.documents.api.SourceDocumentDetail;
import com.berdachuk.docurag.documents.api.SourceDocumentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentCatalogApiImpl implements DocumentCatalogApi {

    private final DocumentJdbcRepository repository;

    @Override
    public List<SourceDocumentSummary> listDocuments(int page, int pageSize) {
        int p = Math.max(0, page);
        int s = Math.min(200, Math.max(1, pageSize));
        int offset = p * s;
        return repository.findPage(offset, s).stream()
                .map(e -> new SourceDocumentSummary(
                        e.id(),
                        e.externalId(),
                        e.title(),
                        e.category(),
                        e.sourceFormat()
                ))
                .toList();
    }

    @Override
    public long countDocuments() {
        return repository.count();
    }

    @Override
    public Optional<SourceDocumentDetail> getDocument(String id) {
        return repository.findById(id).map(e -> new SourceDocumentDetail(
                e.id(),
                e.externalId(),
                e.title(),
                e.category(),
                e.sourceName(),
                e.sourceUrl(),
                e.content(),
                e.sourceFormat()
        ));
    }

    @Override
    public List<String> listCategories() {
        return repository.distinctCategories();
    }
}
