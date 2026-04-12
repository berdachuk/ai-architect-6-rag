@org.springframework.modulith.ApplicationModule(
        id = "llm",
        displayName = "LLM",
        allowedDependencies = {"retrieval::api", "core"}
)
package com.berdachuk.docurag.llm;
