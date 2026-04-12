package com.berdachuk.docurag.visualization.internal;

import com.berdachuk.docurag.extraction.api.AnalysisRequest;
import com.berdachuk.docurag.extraction.api.AnalysisResponse;
import com.berdachuk.docurag.extraction.api.DocumentAnalysisApi;
import com.berdachuk.docurag.visualization.api.GraphVisualizationResponse;
import com.berdachuk.docurag.visualization.api.PieChartResponse;
import com.berdachuk.docurag.visualization.api.VisualizationDataApi;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VisualizationDataServiceImpl implements VisualizationDataApi {

    private final NamedParameterJdbcTemplate jdbc;
    private final DocumentAnalysisApi documentAnalysisApi;

    public VisualizationDataServiceImpl(NamedParameterJdbcTemplate jdbc, DocumentAnalysisApi documentAnalysisApi) {
        this.jdbc = jdbc;
        this.documentAnalysisApi = documentAnalysisApi;
    }

    @Override
    public PieChartResponse categoriesPie() {
        record CatRow(String category, long cnt) {
        }
        List<CatRow> rows = jdbc.query("""
                        SELECT category, COUNT(*) AS cnt FROM source_document
                        WHERE category IS NOT NULL AND category <> ''
                        GROUP BY category ORDER BY cnt DESC
                        """, Map.of(), (rs, rowNum) -> new CatRow(rs.getString("category"), rs.getLong("cnt")));
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (CatRow row : rows) {
            labels.add(row.category());
            values.add(row.cnt());
        }
        if (labels.isEmpty()) {
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM source_document", Map.of(), Long.class);
            labels.add("all");
            values.add(total == null ? 0L : total);
        }
        return new PieChartResponse(labels, values);
    }

    @Override
    public GraphVisualizationResponse entitiesGraph() {
        AnalysisResponse a = documentAnalysisApi.analyze(new AnalysisRequest(10));
        List<GraphVisualizationResponse.GraphNodeDto> nodes = a.graphNodes().stream()
                .map(n -> new GraphVisualizationResponse.GraphNodeDto(n.id(), n.label(), n.type()))
                .toList();
        List<GraphVisualizationResponse.GraphEdgeDto> edges = a.graphEdges().stream()
                .map(e -> new GraphVisualizationResponse.GraphEdgeDto(e.source(), e.target(), e.relation()))
                .toList();
        return new GraphVisualizationResponse(nodes, edges);
    }
}
