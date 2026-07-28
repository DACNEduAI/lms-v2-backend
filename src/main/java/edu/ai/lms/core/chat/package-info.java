@ApplicationModule(allowedDependencies = {
        "common :: api",
        "core.auth :: api",
        "core.material :: api"
})
package edu.ai.lms.core.chat;

import org.springframework.modulith.ApplicationModule;
