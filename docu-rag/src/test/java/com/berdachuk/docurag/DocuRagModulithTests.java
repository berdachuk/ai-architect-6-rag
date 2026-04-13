package com.berdachuk.docurag;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class DocuRagModulithTests {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(DocuRagApplication.class).verify();
    }
}
