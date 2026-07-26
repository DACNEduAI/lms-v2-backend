@ApplicationModule(allowedDependencies = {
        "common :: api",
        "common :: events",
        "core.auth :: api",
        "core.material :: api",
        "core.user :: api"
})
package edu.ai.lms.core.course;

import org.springframework.modulith.ApplicationModule;
