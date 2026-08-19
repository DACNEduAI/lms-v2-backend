package edu.ai.lms.core.course.app.service.matrix3;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import lombok.RequiredArgsConstructor;

/** Matrix 3 stores a relationship grid and its own total question count. */
@Service
@RequiredArgsConstructor
public class Matrix3Service {
    private final JdbcTemplate jdbc;
    private final ExamMatrixAccess access;
    private final LoadSignedInUserPort signedInUserPort;

    @Transactional(readOnly = true)
    public Matrix3List list(UUID courseId) {
        access.requireCourseTeacher(courseId);
        return new Matrix3List(courseId, jdbc.query("""
                SELECT id,course_id,title,description,total_question_count,lifecycle_status,version,published_by,published_at,
                       created_at,updated_at,(SELECT COUNT(*) FROM edu.exam_matrix_3_rows r WHERE r.matrix_id=m.id) row_count,
                       (SELECT COUNT(*) FROM edu.exam_matrix_3_cell_relations c JOIN edu.exam_matrix_3_rows r ON r.id=c.row_id WHERE r.matrix_id=m.id) relation_count
                FROM edu.exam_matrices_3 m WHERE course_id=? ORDER BY updated_at DESC,created_at DESC
                """, (rs, row) -> new Matrix3Summary(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                rs.getString(3), rs.getString(4), rs.getInt(5), LifecycleStatus.valueOf(rs.getString(6)), rs.getLong(7),
                rs.getObject(8, UUID.class), rs.getObject(9, LocalDateTime.class), rs.getInt(12), rs.getInt(13),
                rs.getObject(10, LocalDateTime.class), rs.getObject(11, LocalDateTime.class)), courseId));
    }

    @Transactional(readOnly = true)
    public Matrix3Detail get(UUID courseId, UUID matrixId) { access.requireCourseTeacher(courseId); return load(courseId, matrixId); }

    @Transactional
    public Matrix3Detail create(UUID courseId, Matrix3Payload payload) {
        access.requireCourseTeacher(courseId); validate(courseId, payload);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.exam_matrices_3(id,course_id,title,description,total_question_count) VALUES(?,?,?,?,?)",
                id, courseId, payload.title().trim(), trim(payload.description()), payload.totalQuestionCount());
        replace(id, payload); return load(courseId, id);
    }

    @Transactional
    public Matrix3Detail copyAsDraft(UUID courseId, UUID sourceId, Matrix3CopyRequest request) {
        access.requireCourseTeacher(courseId); Matrix3Detail source = load(courseId, sourceId);
        Map<UUID, UUID> levels = new HashMap<>();
        List<Matrix3LearningLevelPayload> copiedLevels = source.customLearningLevels().stream().map(level -> {
            UUID id = UUID.randomUUID(); levels.put(level.id(), id); return new Matrix3LearningLevelPayload(id, level.name());
        }).toList();
        List<Matrix3RowPayload> copiedRows = source.rows().stream().map(row -> new Matrix3RowPayload(UUID.randomUUID(), row.chapterId(), row.mappedLearningOutcomeId(),
                row.cellRelations().stream().map(cell -> new Matrix3CellRelation(
                        levels.get(cell.customLearningLevelId()), cell.related(), cell.questionType())).toList())).toList();
        String title = request == null || trim(request.title()) == null ? source.title()+" (Copy)" : trim(request.title());
        return create(courseId, new Matrix3Payload(title, source.description(), source.totalQuestionCount(), copiedRows, copiedLevels, null));
    }

    @Transactional
    public Matrix3Detail update(UUID courseId, UUID matrixId, Matrix3Payload payload) {
        access.requireCourseTeacher(courseId); Matrix3Metadata current = editable(courseId, matrixId);
        if (payload == null || payload.expectedVersion() == null || payload.expectedVersion() != current.version()) throw conflict("Matrix 3 changed before update; reload and try again");
        validate(courseId, payload);
        int changed = jdbc.update("""
                UPDATE edu.exam_matrices_3 SET title=?,description=?,total_question_count=?,version=version+1,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND course_id=? AND lifecycle_status='DRAFT' AND version=?
                """, payload.title().trim(), trim(payload.description()), payload.totalQuestionCount(), matrixId, courseId, payload.expectedVersion());
        if (changed != 1) throw conflict("Matrix 3 changed before update; reload and try again");
        jdbc.update("DELETE FROM edu.exam_matrix_3_rows WHERE matrix_id=?", matrixId);
        jdbc.update("DELETE FROM edu.exam_matrix_3_custom_learning_levels WHERE matrix_id=?", matrixId);
        replace(matrixId, payload); return load(courseId, matrixId);
    }

    @Transactional
    public void delete(UUID courseId, UUID matrixId) {
        access.requireCourseTeacher(courseId); editable(courseId, matrixId);
        if (jdbc.update("DELETE FROM edu.exam_matrices_3 WHERE id=? AND course_id=? AND lifecycle_status='DRAFT'", matrixId, courseId) != 1) throw conflict("Only a DRAFT Matrix 3 can be deleted");
    }

    @Transactional
    public Matrix3Detail publish(UUID courseId, UUID matrixId, Matrix3PublishRequest request) {
        access.requireCourseTeacher(courseId); Matrix3Metadata current = editable(courseId, matrixId);
        if (request == null || request.expectedVersion() != current.version()) throw conflict("Matrix 3 changed before publish; reload and try again");
        Matrix3Detail detail = load(courseId, matrixId); validateConfirmable(detail);
        UUID actor = signedInUserPort.findAuthenticatedUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required")).getId();
        if (jdbc.update("""
                UPDATE edu.exam_matrices_3 SET lifecycle_status='PUBLISHED',version=version+1,published_by=?,published_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND course_id=? AND lifecycle_status='DRAFT' AND version=?
                """, actor, matrixId, courseId, request.expectedVersion()) != 1) throw conflict("Matrix 3 changed before publish; reload and try again");
        return load(courseId, matrixId);
    }

    public void validateConfirmable(Matrix3Detail matrix) {
        List<String> errors = new ArrayList<>();
        if (matrix.totalQuestionCount() <= 0) errors.add("Matrix 3 total question count must be greater than zero");
        if (matrix.rows().isEmpty()) errors.add("Matrix 3 needs at least one row");
        if (matrix.customLearningLevels().isEmpty()) errors.add("Matrix 3 needs at least one custom learning level");
        for (int i=0; i<matrix.rows().size(); i++) { Matrix3Row row=matrix.rows().get(i); if(row.chapterId()==null) errors.add("Row "+(i+1)+" is missing Chapter"); if(row.mappedLearningOutcomeId()==null) errors.add("Row "+(i+1)+" is missing mapped learning outcome"); }
        if (matrix.rows().stream().flatMap(row -> row.cellRelations().stream()).noneMatch(Matrix3CellRelation::related)) errors.add("Matrix 3 needs at least one selected relation");
        if (!errors.isEmpty()) throw badRequest(String.join("; ", errors));
    }

    private Matrix3Detail load(UUID courseId, UUID matrixId) {
        Matrix3Metadata meta = jdbc.query("""
                SELECT id,course_id,title,description,total_question_count,lifecycle_status,version,published_by,published_at,created_at,updated_at
                FROM edu.exam_matrices_3 WHERE id=? AND course_id=?
                """, rs -> rs.next() ? new Matrix3Metadata(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getInt(5),LifecycleStatus.valueOf(rs.getString(6)),rs.getLong(7),rs.getObject(8,UUID.class),rs.getObject(9,LocalDateTime.class),rs.getObject(10,LocalDateTime.class),rs.getObject(11,LocalDateTime.class)) : null, matrixId,courseId);
        if (meta == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrix 3 not found: "+matrixId);
        List<Matrix3LearningLevel> levels = jdbc.query("SELECT id,name,sort_order FROM edu.exam_matrix_3_custom_learning_levels WHERE matrix_id=? ORDER BY sort_order,id",(rs,row)->new Matrix3LearningLevel(rs.getObject(1,UUID.class),rs.getString(2),rs.getInt(3)),matrixId);
        Map<UUID,List<Matrix3CellRelation>> cells=new HashMap<>();
        jdbc.query("SELECT r.id,c.custom_learning_level_id,c.question_type FROM edu.exam_matrix_3_rows r JOIN edu.exam_matrix_3_cell_relations c ON c.row_id=r.id WHERE r.matrix_id=?",(org.springframework.jdbc.core.RowCallbackHandler)rs->cells.computeIfAbsent(rs.getObject(1,UUID.class), ignored -> new ArrayList<>()).add(new Matrix3CellRelation(rs.getObject(2,UUID.class),true,QuestionType.valueOf(rs.getString(3)))),matrixId);
        List<Matrix3Row> rows=jdbc.query("""
                SELECT r.id,r.chapter_id,c.title,r.mapped_learning_outcome_id,CASE WHEN lo.prefix_number IS NULL THEN lo.description ELSE 'LO'||lo.prefix_number||' - '||lo.description END,r.sort_order
                FROM edu.exam_matrix_3_rows r LEFT JOIN edu.chapters c ON c.chapter_id=r.chapter_id LEFT JOIN edu.learning_outcomes lo ON lo.id=r.mapped_learning_outcome_id
                WHERE r.matrix_id=? ORDER BY r.sort_order,r.id
                """,(rs,row)->new Matrix3Row(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getObject(4,UUID.class),rs.getString(5),rs.getInt(6),List.copyOf(cells.getOrDefault(rs.getObject(1,UUID.class),List.of()))),matrixId);
        return new Matrix3Detail(meta.id(),meta.courseId(),meta.title(),meta.description(),meta.totalQuestionCount(),meta.lifecycleStatus(),meta.version(),meta.publishedBy(),meta.publishedAt(),rows,levels,meta.createdAt(),meta.updatedAt());
    }

    private void replace(UUID matrixId, Matrix3Payload payload) {
        for(int i=0;i<safe(payload.customLearningLevels()).size();i++){var level=safe(payload.customLearningLevels()).get(i);jdbc.update("INSERT INTO edu.exam_matrix_3_custom_learning_levels(id,matrix_id,name,sort_order) VALUES(?,?,?,?)",level.id(),matrixId,level.name().trim(),i);}
        for(int i=0;i<safe(payload.rows()).size();i++){var row=safe(payload.rows()).get(i);jdbc.update("INSERT INTO edu.exam_matrix_3_rows(id,matrix_id,chapter_id,mapped_learning_outcome_id,sort_order) VALUES(?,?,?,?,?)",row.id(),matrixId,row.chapterId(),row.mappedLearningOutcomeId(),i);for(var cell:safe(row.cellRelations()))if(cell.related())jdbc.update("INSERT INTO edu.exam_matrix_3_cell_relations(row_id,custom_learning_level_id,question_type) VALUES(?,?,?)",row.id(),cell.customLearningLevelId(),questionType(cell.questionType()).name());}
    }

    private void validate(UUID courseId, Matrix3Payload payload) {
        if(payload==null||trim(payload.title())==null) throw badRequest("Matrix 3 title cannot be blank");
        if(payload.totalQuestionCount()==null||payload.totalQuestionCount()<=0) throw badRequest("Matrix 3 total question count must be greater than zero");
        Set<UUID> levels=new HashSet<>(); for(var level:safe(payload.customLearningLevels())){if(level.id()==null||trim(level.name())==null)throw badRequest("Each Matrix 3 learning level needs an id and a name");if(!levels.add(level.id()))throw badRequest("Duplicate Matrix 3 learning level id");}
        Set<UUID> rows=new HashSet<>(); for(var row:safe(payload.rows())){if(row.id()==null||!rows.add(row.id()))throw badRequest("Each Matrix 3 row needs a unique id");if(row.chapterId()!=null&&!exists("SELECT 1 FROM edu.chapters WHERE chapter_id=? AND course_id=?",row.chapterId(),courseId))throw badRequest("Chapter does not belong to this course");if(row.mappedLearningOutcomeId()!=null&&!exists("SELECT 1 FROM edu.learning_outcomes WHERE id=? AND course_id=?",row.mappedLearningOutcomeId(),courseId))throw badRequest("Mapped learning outcome does not belong to this course");Set<UUID> seen=new HashSet<>();for(var cell:safe(row.cellRelations())){if(cell.customLearningLevelId()==null||!levels.contains(cell.customLearningLevelId()))throw badRequest("A Matrix 3 relation references an unknown learning level");if(!seen.add(cell.customLearningLevelId()))throw badRequest("Duplicate relation in a Matrix 3 row");questionType(cell.questionType());}}
    }
    private Matrix3Metadata editable(UUID courseId,UUID id){Matrix3Detail detail=load(courseId,id);if(detail.lifecycleStatus()!=LifecycleStatus.DRAFT)throw conflict("Published Matrix 3 is read-only");return new Matrix3Metadata(detail.id(),detail.courseId(),detail.title(),detail.description(),detail.totalQuestionCount(),detail.lifecycleStatus(),detail.version(),detail.publishedBy(),detail.publishedAt(),detail.createdAt(),detail.updatedAt());}
    private boolean exists(String sql,UUID id,UUID courseId){return Boolean.TRUE.equals(jdbc.query(sql,(org.springframework.jdbc.core.ResultSetExtractor<Boolean>)rs->rs.next(),id,courseId));}
    private static <T> List<T> safe(List<T> value){return value==null?List.of():value;}
    private static QuestionType questionType(QuestionType value){return value==null?QuestionType.MULTIPLE_CHOICE:value;}
    private static String trim(String value){return value==null||value.isBlank()?null:value.trim();}
    private static ResponseStatusException badRequest(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}

    private record Matrix3Metadata(UUID id,UUID courseId,String title,String description,int totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,LocalDateTime publishedAt,LocalDateTime createdAt,LocalDateTime updatedAt) { }
    public enum LifecycleStatus { DRAFT, PUBLISHED }
    public enum QuestionType { MULTIPLE_CHOICE, CODE_RUNNER }
    public record Matrix3List(UUID courseId,List<Matrix3Summary> matrices) { }
    public record Matrix3Summary(UUID id,UUID courseId,String title,String description,int totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,LocalDateTime publishedAt,int rowCount,int relationCount,LocalDateTime createdAt,LocalDateTime updatedAt) { }
    public record Matrix3Detail(UUID id,UUID courseId,String title,String description,int totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,LocalDateTime publishedAt,List<Matrix3Row> rows,List<Matrix3LearningLevel> customLearningLevels,LocalDateTime createdAt,LocalDateTime updatedAt) { }
    public record Matrix3Row(UUID id,UUID chapterId,String chapterName,UUID mappedLearningOutcomeId,String mappedLearningOutcomeName,int sortOrder,List<Matrix3CellRelation> cellRelations) { }
    public record Matrix3LearningLevel(UUID id,String name,int sortOrder) { }
    public record Matrix3CellRelation(UUID customLearningLevelId,boolean related,QuestionType questionType) { }
    public record Matrix3Payload(String title,String description,Integer totalQuestionCount,List<Matrix3RowPayload> rows,List<Matrix3LearningLevelPayload> customLearningLevels,Long expectedVersion) { }
    public record Matrix3RowPayload(UUID id,UUID chapterId,UUID mappedLearningOutcomeId,List<Matrix3CellRelation> cellRelations) { }
    public record Matrix3LearningLevelPayload(UUID id,String name) { }
    public record Matrix3PublishRequest(long expectedVersion) { }
    public record Matrix3CopyRequest(String title) { }
}
