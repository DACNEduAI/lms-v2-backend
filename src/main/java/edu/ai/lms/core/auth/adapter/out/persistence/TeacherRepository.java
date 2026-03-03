package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.TeacherJdbcTable;

/**
 * Package-private repository interface for teachers.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface TeacherRepository extends CrudRepository<TeacherJdbcTable, UUID> {

    @Modifying
    @Query("INSERT INTO teachers (user_id, employee_id, phone_number, department, specialization) VALUES (:userId, :employeeId, :phoneNumber, :department, :specialization)")
    void insertTeacher(@Param("userId") UUID userId, @Param("employeeId") String employeeId, @Param("phoneNumber") String phoneNumber, @Param("department") String department, @Param("specialization") String specialization);
}
