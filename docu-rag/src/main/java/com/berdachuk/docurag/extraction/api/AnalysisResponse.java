package com.berdachuk.docurag.extraction.api;

import java.util.List;

public record AnalysisResponse(
        List<CategoryCount> categories,
        List<GraphNode> graphNodes,
        List<GraphEdge> graphEdges,
        String extractionNotes
) {
}
