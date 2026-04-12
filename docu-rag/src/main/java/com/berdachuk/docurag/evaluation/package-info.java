@org.springframework.modulith.ApplicationModule(
        id = "evaluation",
        displayName = "Evaluation",
        allowedDependencies = {"llm::api", "core"}
)
package com.berdachuk.docurag.evaluation;
