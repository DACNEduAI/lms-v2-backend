package edu.ai.lms.core.material.adapter.out.persistence.mybatis;

import edu.ai.lms.core.material.app.domain.Material;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps the {@code materials.ingestion_status} column exclusively to its material enum. */
public class MaterialIngestionStatusTypeHandler extends BaseTypeHandler<Material.IngestionStatus> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Material.IngestionStatus value,
            JdbcType jdbcType) throws SQLException {
        statement.setString(index, value.name());
    }

    @Override
    public Material.IngestionStatus getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : Material.IngestionStatus.valueOf(value);
    }

    @Override
    public Material.IngestionStatus getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        String value = resultSet.getString(columnIndex);
        return value == null ? null : Material.IngestionStatus.valueOf(value);
    }

    @Override
    public Material.IngestionStatus getNullableResult(CallableStatement statement, int columnIndex)
            throws SQLException {
        String value = statement.getString(columnIndex);
        return value == null ? null : Material.IngestionStatus.valueOf(value);
    }
}
