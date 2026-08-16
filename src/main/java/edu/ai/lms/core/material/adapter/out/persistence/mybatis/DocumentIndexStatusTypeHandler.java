package edu.ai.lms.core.material.adapter.out.persistence.mybatis;

import edu.ai.lms.core.material.app.domain.DocumentIndex;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps only registry statuses; it must never be reused for material-index jobs. */
public class DocumentIndexStatusTypeHandler extends BaseTypeHandler<DocumentIndex.Status> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, DocumentIndex.Status value,
            JdbcType jdbcType) throws SQLException {
        statement.setString(index, value.name());
    }

    @Override
    public DocumentIndex.Status getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : DocumentIndex.Status.valueOf(value);
    }

    @Override
    public DocumentIndex.Status getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        String value = resultSet.getString(columnIndex);
        return value == null ? null : DocumentIndex.Status.valueOf(value);
    }

    @Override
    public DocumentIndex.Status getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        String value = statement.getString(columnIndex);
        return value == null ? null : DocumentIndex.Status.valueOf(value);
    }
}
