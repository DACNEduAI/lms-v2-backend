@ApplicationModule(allowedDependencies = {
        "common :: api",
        "common :: events",
        "core.auth :: api"
})
package edu.ai.lms.core.material;

import org.springframework.modulith.ApplicationModule;
