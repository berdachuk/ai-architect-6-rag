package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.visualization.api.GraphVisualizationResponse;
import com.berdachuk.docurag.visualization.api.PieChartResponse;
import com.berdachuk.docurag.visualization.api.VisualizationDataApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visualizations")
@RequiredArgsConstructor
public class VisualizationRestController {

    private final VisualizationDataApi visualizationDataApi;

    @GetMapping("/categories/pie")
    public PieChartResponse pie() {
        return visualizationDataApi.categoriesPie();
    }

    @GetMapping("/entities/graph")
    public GraphVisualizationResponse graph() {
        return visualizationDataApi.entitiesGraph();
    }
}
