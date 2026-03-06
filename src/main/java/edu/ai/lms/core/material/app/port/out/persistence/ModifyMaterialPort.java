package edu.ai.lms.core.material.app.port.out.persistence;

import edu.ai.lms.core.material.app.domain.Material;

public interface ModifyMaterialPort {

    Material update(Material material);
}
