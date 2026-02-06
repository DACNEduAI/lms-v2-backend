package edu.ai.lms;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class PackageStructureTest {

    ApplicationModules modules = ApplicationModules.of(LmsApplication.class);

    @Test
    void verifyModulesStructure() {
        System.out.println(modules);
        modules.verify();
    }
}
