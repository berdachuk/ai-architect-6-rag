package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.visualization.api.VisualizationDataApi;
import com.berdachuk.docurag.web.openapi.api.VisualizationsApi;
import com.berdachuk.docurag.web.openapi.model.GraphVisualizationResponse;
import com.berdachuk.docurag.web.openapi.model.PieChartResponse;
import com.berdachuk.docurag.web.openapi.model.VizGraphEdge;
import com.berdachuk.docurag.web.openapi.model.VizGraphNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VisualizationRestController implements VisualizationsApi {

    private final VisualizationDataApi visualizationDataApi;

    @Override
    public ResponseEntity<PieChartResponse> getCategoriesPie() {
        com.berdachuk.docurag.visualization.api.PieChartResponse pie = visualizationDataApi.categoriesPie();
        PieChartResponse payload = new PieChartResponse()
                .labels(pie.labels())
                .values(pie.values());
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<GraphVisualizationResponse> getEntitiesGraph() {
        com.berdachuk.docurag.visualization.api.GraphVisualizationResponse graph = visualizationDataApi.entitiesGraph();
        GraphVisualizationResponse payload = new GraphVisualizationResponse()
                .nodes(graph.nodes().stream()
                        .map(n -> new VizGraphNode().id(n.id()).label(n.label()).type(n.type()))
                        .toList())
                .edges(graph.edges().stream()
                        .map(e -> new VizGraphEdge().source(e.source()).target(e.target()).relation(e.relation()))
                        .toList());
        return ResponseEntity.ok(payload);
    }
}
