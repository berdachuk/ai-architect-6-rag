package com.berdachuk.docurag.visualization.api;

import java.util.List;

public record GraphVisualizationResponse(
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges
) {
    public record GraphNodeDto(String id, String label, String type) {
    }

    public record GraphEdgeDto(String source, String target, String relation) {
    }
}
