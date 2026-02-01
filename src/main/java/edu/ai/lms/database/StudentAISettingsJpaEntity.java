package edu.ai.lms.database;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("STUDENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StudentAISettingsJpaEntity extends AISettingsJpaEntity {

    private boolean alignToCLO;

    private boolean includeAnswerKey;

    private String mode;
}
