package edu.ai.lms.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/** Prevents every @Scheduled method from registering while the one-shot backfill command runs. */
@Configuration
@ConditionalOnProperty(name = "quiz3.rich-content-backfill.enabled", havingValue = "true")
public class BackfillSchedulingDisabledConfiguration {
    @Bean
    static BeanDefinitionRegistryPostProcessor removeScheduledProcessorForBackfill() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                String name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
                if (registry.containsBeanDefinition(name)) registry.removeBeanDefinition(name);
                registry.registerBeanDefinition(name, new RootBeanDefinition(DisabledScheduledProcessor.class));
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException { }
        };
    }

    static final class DisabledScheduledProcessor extends ScheduledAnnotationBeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            return bean;
        }
    }
}
