@ApplicationModule(allowedDependencies = {
        "common :: api",
        "common :: events",
        "core.auth :: api",
        "core.course :: api",
        "core.material :: api"
})
package edu.ai.lms.core.quiz3;

import org.springframework.modulith.ApplicationModule;
