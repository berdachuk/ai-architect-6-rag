package com.berdachuk.docurag.visualization.api;

import java.util.List;

public record PieChartResponse(
        List<String> labels,
        List<Long> values
) {
}
