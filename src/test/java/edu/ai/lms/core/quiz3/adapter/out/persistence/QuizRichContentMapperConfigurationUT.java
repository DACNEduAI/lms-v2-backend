package edu.ai.lms.core.quiz3.adapter.out.persistence;

import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.EnumTypeHandler;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizRichContentMapperConfigurationUT {

    @Test
    void questionReadsRichContentAndFormatColumns() throws Exception {
        Configuration configuration = parseQuestionMapper();
        ResultMap resultMap = configuration.getResultMap(
                "edu.ai.lms.core.quiz3.adapter.out.persistence.QuestionRepository.QuestionResultMap");

        assertRichContentFormat(resultMap, "contentFormat", "content_format");
        assertMapped(resultMap, "explanation", "explanation");
        assertRichContentFormat(resultMap, "explanationFormat", "explanation_format");
        assertSelectsColumns(configuration, "edu.ai.lms.core.quiz3.adapter.out.persistence.QuestionRepository.getById",
                "content_format", "explanation", "explanation_format");
        assertSelectsColumns(configuration, "edu.ai.lms.core.quiz3.adapter.out.persistence.QuestionRepository.getByQuizId",
                "content_format", "explanation", "explanation_format");
    }

    @Test
    void answerReadsRichContentAndFormatColumns() throws Exception {
        Configuration configuration = parseAnswerMapper();
        ResultMap resultMap = configuration.getResultMap(
                "edu.ai.lms.core.quiz3.adapter.out.persistence.AnswerRepository.AnswerResultMap");

        assertRichContentFormat(resultMap, "contentFormat", "content_format");
        assertMapped(resultMap, "explanation", "explanation");
        assertRichContentFormat(resultMap, "explanationFormat", "explanation_format");
        assertSelectsColumns(configuration, "edu.ai.lms.core.quiz3.adapter.out.persistence.AnswerRepository.getByQuestionId",
                "content_format", "explanation_format");
        assertSelectsColumns(configuration,
                "edu.ai.lms.core.quiz3.adapter.out.persistence.AnswerRepository.getByAnswerNumberAndQuestionId",
                "content_format", "explanation_format");
    }

    private Configuration parseQuestionMapper() throws Exception {
        return parse("edu/ai/lms/core/quiz3/adapter/out/persistence/mapper/QuestionRepositoryMapper.xml");
    }

    private Configuration parseAnswerMapper() throws Exception {
        return parse("edu/ai/lms/core/quiz3/adapter/out/persistence/mapper/AnswerRepositoryMapper.xml");
    }

    private Configuration parse(String resource) throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private void assertRichContentFormat(ResultMap resultMap, String property, String column) {
        ResultMapping mapping = mappingFor(resultMap, property);
        assertEquals(column, mapping.getColumn());
        assertEquals(RichContentFormat.class, mapping.getJavaType());
        assertEquals(EnumTypeHandler.class, mapping.getTypeHandler().getClass());
    }

    private void assertMapped(ResultMap resultMap, String property, String column) {
        assertEquals(column, mappingFor(resultMap, property).getColumn());
    }

    private ResultMapping mappingFor(ResultMap resultMap, String property) {
        return resultMap.getResultMappings().stream()
                .filter(mapping -> property.equals(mapping.getProperty()))
                .findFirst()
                .orElseThrow();
    }

    private void assertSelectsColumns(Configuration configuration, String statementId, String... columns) {
        MappedStatement statement = configuration.getMappedStatement(statementId);
        String sql = statement.getBoundSql(null).getSql();
        for (String column : columns) {
            assertTrue(sql.contains(column), () -> statementId + " must select " + column);
        }
    }
}
