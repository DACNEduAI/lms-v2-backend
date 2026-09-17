package edu.ai.lms.core.learnermodel.adapter.out.persistence.mybatis;

import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcomeStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Keeps learner-state outcome statuses isolated from unrelated enum columns. */
public class LearnerStateOutcomeStatusTypeHandler extends BaseTypeHandler<LearnerStateOutcomeStatus> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, LearnerStateOutcomeStatus value,
            JdbcType jdbcType) throws SQLException {
        statement.setString(index, value.name());
    }

    @Override
    public LearnerStateOutcomeStatus getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : LearnerStateOutcomeStatus.valueOf(value);
    }

    @Override
    public LearnerStateOutcomeStatus getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        String value = resultSet.getString(columnIndex);
        return value == null ? null : LearnerStateOutcomeStatus.valueOf(value);
    }

    @Override
    public LearnerStateOutcomeStatus getNullableResult(CallableStatement statement, int columnIndex)
            throws SQLException {
        String value = statement.getString(columnIndex);
        return value == null ? null : LearnerStateOutcomeStatus.valueOf(value);
    }
}
