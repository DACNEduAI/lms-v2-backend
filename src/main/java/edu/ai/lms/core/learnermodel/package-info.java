@ApplicationModule(allowedDependencies = {
        "common :: api",
        "core.auth :: api",
        "core.course :: api",
        "core.quiz :: api",
        "core.attempt :: api"
})
package edu.ai.lms.core.learnermodel;

import org.springframework.modulith.ApplicationModule;
