package edu.ai.lms.core.material.adapter.out.persistence.mybatis;

import edu.ai.lms.core.material.app.domain.Material;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps the {@code materials.type} column exclusively to {@link Material.MaterialType}. */
public class MaterialTypeTypeHandler extends BaseTypeHandler<Material.MaterialType> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Material.MaterialType value,
            JdbcType jdbcType) throws SQLException {
        statement.setString(index, value.name());
    }

    @Override
    public Material.MaterialType getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : Material.MaterialType.valueOf(value);
    }

    @Override
    public Material.MaterialType getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        String value = resultSet.getString(columnIndex);
        return value == null ? null : Material.MaterialType.valueOf(value);
    }

    @Override
    public Material.MaterialType getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        String value = statement.getString(columnIndex);
        return value == null ? null : Material.MaterialType.valueOf(value);
    }
}
