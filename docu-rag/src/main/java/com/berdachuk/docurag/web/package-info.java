@org.springframework.modulith.ApplicationModule(
        id = "web",
        displayName = "Web",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {
                "evaluation::api",
                "visualization::api",
                "extraction::api",
                "llm::api",
                "vector::api",
                "documents::api",
                "core"
        }
)
package com.berdachuk.docurag.web;
