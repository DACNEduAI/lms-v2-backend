@ApplicationModule(allowedDependencies = {
        "common :: events",
        "core.auth :: api",
        "core.course :: api",
        "core.material :: events",
        "core.quiz :: events"
})
package edu.ai.lms.core.notification;

import org.springframework.modulith.ApplicationModule;
