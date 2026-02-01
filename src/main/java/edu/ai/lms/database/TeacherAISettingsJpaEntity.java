package edu.ai.lms.database;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("TEACHER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TeacherAISettingsJpaEntity extends AISettingsJpaEntity {

    private boolean includeAnswerKey;

    private boolean alignToCLO;
}
