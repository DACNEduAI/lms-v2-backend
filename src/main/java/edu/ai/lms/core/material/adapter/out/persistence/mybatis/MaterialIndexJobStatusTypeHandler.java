package edu.ai.lms.core.material.adapter.out.persistence.mybatis;

import edu.ai.lms.core.material.app.domain.MaterialIndexJob;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Keeps the material-index job status enum isolated from other MyBatis enum mappings. */
public class MaterialIndexJobStatusTypeHandler extends BaseTypeHandler<MaterialIndexJob.Status> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, MaterialIndexJob.Status value,
            JdbcType jdbcType) throws SQLException {
        statement.setString(index, value.name());
    }

    @Override
    public MaterialIndexJob.Status getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : MaterialIndexJob.Status.valueOf(value);
    }

    @Override
    public MaterialIndexJob.Status getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        String value = resultSet.getString(columnIndex);
        return value == null ? null : MaterialIndexJob.Status.valueOf(value);
    }

    @Override
    public MaterialIndexJob.Status getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        String value = statement.getString(columnIndex);
        return value == null ? null : MaterialIndexJob.Status.valueOf(value);
    }
}
