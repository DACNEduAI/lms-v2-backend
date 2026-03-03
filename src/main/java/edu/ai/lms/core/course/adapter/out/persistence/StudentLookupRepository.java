package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.StudentJdbcTable;

@Repository
interface StudentLookupRepository extends CrudRepository<StudentJdbcTable, UUID> {

    @Query("SELECT * FROM students WHERE email IN (:emails)")
    List<StudentJdbcTable> findByEmailIn(@Param("emails") List<String> emails);
}
