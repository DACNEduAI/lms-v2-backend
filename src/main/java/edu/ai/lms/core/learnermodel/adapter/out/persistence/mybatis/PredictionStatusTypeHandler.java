package edu.ai.lms.core.learnermodel.adapter.out.persistence.mybatis;

import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Keeps model-specific prediction statuses isolated from aggregate learner-state statuses. */
public class PredictionStatusTypeHandler extends BaseTypeHandler<PredictionStatus> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, PredictionStatus value,
            JdbcType jdbcType) throws SQLException {
        statement.setString(index, value.name());
    }

    @Override
    public PredictionStatus getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : PredictionStatus.valueOf(value);
    }

    @Override
    public PredictionStatus getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        String value = resultSet.getString(columnIndex);
        return value == null ? null : PredictionStatus.valueOf(value);
    }

    @Override
    public PredictionStatus getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        String value = statement.getString(columnIndex);
        return value == null ? null : PredictionStatus.valueOf(value);
    }
}
