@ApplicationModule(allowedDependencies = {
        "common :: api",
        "core.auth :: api",
        "core.course :: api",
        "core.quiz :: api",
        "core.coderunner :: api"
})
package edu.ai.lms.core.attempt;

import org.springframework.modulith.ApplicationModule;
