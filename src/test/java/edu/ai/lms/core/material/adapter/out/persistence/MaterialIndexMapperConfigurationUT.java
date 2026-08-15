package edu.ai.lms.core.material.adapter.out.persistence;

import edu.ai.lms.config.mybatis.DocumentIndexStatusTypeHandler;
import edu.ai.lms.config.mybatis.MaterialIndexJobOperationTypeHandler;
import edu.ai.lms.config.mybatis.MaterialIndexJobStatusTypeHandler;
import edu.ai.lms.config.mybatis.UuidTypeHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaterialIndexMapperConfigurationUT {
    @Test
    void materialJobsAndDocumentRegistryUseTheirOwnEnumHandlers() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeHandlerRegistry().register(UuidTypeHandler.class);
        parse(configuration, "edu/ai/lms/core/material/adapter/out/persistence/mapper/MaterialIndexJobRepositoryMapper.xml");
        parse(configuration, "edu/ai/lms/core/material/adapter/out/persistence/mapper/DocumentIndexRepositoryMapper.xml");

        ResultMap jobMap = configuration.getResultMap(
                "edu.ai.lms.core.material.adapter.out.persistence.MaterialIndexJobRepository.JobMap");
        ResultMap documentMap = configuration.getResultMap(
                "edu.ai.lms.core.material.adapter.out.persistence.DocumentIndexRepository.DocumentIndexMap");

        assertEquals(MaterialIndexJobOperationTypeHandler.class,
                handlerFor(jobMap, "operation").getTypeHandler().getClass());
        assertEquals(MaterialIndexJobStatusTypeHandler.class,
                handlerFor(jobMap, "status").getTypeHandler().getClass());
        assertEquals(DocumentIndexStatusTypeHandler.class,
                handlerFor(documentMap, "status").getTypeHandler().getClass());
    }

    private void parse(Configuration configuration, String resource) throws Exception {
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private ResultMapping handlerFor(ResultMap resultMap, String property) {
        return resultMap.getResultMappings().stream()
                .filter(mapping -> property.equals(mapping.getProperty()))
                .findFirst()
                .orElseThrow();
    }
}
