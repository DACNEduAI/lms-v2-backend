@ApplicationModule(allowedDependencies = {
        "common :: api",
        "core.auth :: api",
        "core.course :: api",
        "core.quiz :: api"
})
package edu.ai.lms.core.coderunner;

import org.springframework.modulith.ApplicationModule;
