package edu.ai.lms.core.course.app.service.matrix;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import edu.ai.lms.core.course.adapter.out.persistence.MatrixAssetCoverageRepository;
import edu.ai.lms.core.course.adapter.out.persistence.MatrixAssetCoverageRow;
import lombok.RequiredArgsConstructor;

/** Course-owned matrix authoring with manual quotas or automatic cell allocation. */
@Service
@RequiredArgsConstructor
public class MatrixService {
    private final JdbcTemplate jdbc;
    private final CourseTeacherAccess access;
    private final LoadSignedInUserPort signedInUserPort;
    private final MatrixAssetCoverageRepository assetCoverages;

    @Transactional(readOnly = true)
    public MatrixList list(UUID courseId) {
        access.requireCourseTeacher(courseId);
        return new MatrixList(courseId, jdbc.query("""
            SELECT m.id,m.course_id,m.title,m.description,m.mode,m.total_question_count,m.lifecycle_status,m.version,m.published_by,m.published_at,m.created_at,m.updated_at,
              (SELECT count(*) FROM edu.exam_matrix_rows r WHERE r.matrix_id=m.id),
              CASE WHEN m.mode='MANUAL' THEN coalesce((SELECT sum(c.question_count) FROM edu.exam_matrix_cells c JOIN edu.exam_matrix_rows r ON r.id=c.row_id WHERE r.matrix_id=m.id),0) ELSE m.total_question_count END
            FROM edu.exam_matrices m WHERE m.course_id=? ORDER BY m.updated_at DESC,m.created_at DESC
            """, (rs,n)->new MatrixSummary(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),MatrixMode.valueOf(rs.getString(5)),rs.getObject(6,Integer.class),LifecycleStatus.valueOf(rs.getString(7)),rs.getLong(8),rs.getObject(9,UUID.class),rs.getObject(10,LocalDateTime.class),rs.getInt(13),rs.getInt(14),rs.getObject(11,LocalDateTime.class),rs.getObject(12,LocalDateTime.class)),courseId));
    }

    @Transactional(readOnly = true)
    public MatrixDetail get(UUID courseId, UUID matrixId) { access.requireCourseTeacher(courseId); return load(courseId,matrixId); }

    @Transactional public MatrixDetail create(UUID courseId, MatrixPayload payload) {
        access.requireCourseTeacher(courseId); validate(courseId,payload); UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO edu.exam_matrices(id,course_id,title,description,mode,total_question_count) VALUES(?,?,?,?,?,?)",id,courseId,payload.title().trim(),trim(payload.description()),payload.mode().name(),payload.totalQuestionCount());
        replace(id,payload); return load(courseId,id);
    }
    @Transactional public MatrixDetail update(UUID courseId,UUID id,MatrixPayload payload) {
        access.requireCourseTeacher(courseId); MatrixDetail old=editable(courseId,id);
        if(payload==null||payload.expectedVersion()==null||payload.expectedVersion()!=old.version()) throw conflict("Matrix changed before update; reload and try again");
        validate(courseId,payload);
        if(jdbc.update("UPDATE edu.exam_matrices SET title=?,description=?,mode=?,total_question_count=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=? AND lifecycle_status='DRAFT' AND version=?",payload.title().trim(),trim(payload.description()),payload.mode().name(),payload.totalQuestionCount(),id,courseId,payload.expectedVersion())!=1) throw conflict("Matrix changed before update; reload and try again");
        jdbc.update("DELETE FROM edu.exam_matrix_rows WHERE matrix_id=?",id); jdbc.update("DELETE FROM edu.exam_matrix_custom_learning_levels WHERE matrix_id=?",id); replace(id,payload); return load(courseId,id);
    }
    @Transactional public MatrixDetail copyAsDraft(UUID courseId,UUID sourceId,CopyRequest request) {
        access.requireCourseTeacher(courseId); MatrixDetail source=load(courseId,sourceId); Map<UUID,UUID> levels=new HashMap<>();
        List<LearningLevelPayload> copiedLevels=source.customLearningLevels().stream().map(x->{UUID id=UUID.randomUUID();levels.put(x.id(),id);return new LearningLevelPayload(id,x.name(),x.assetCoverages());}).toList();
        List<RowPayload> rows=source.rows().stream().map(r->new RowPayload(UUID.randomUUID(),r.chapterId(),r.mappedLearningOutcomeId(),r.cells().stream().map(c->new Cell(levels.get(c.customLearningLevelId()),c.questionCount())).toList())).toList();
        return create(courseId,new MatrixPayload(request==null||trim(request.title())==null?source.title()+" (Copy)":trim(request.title()),source.description(),source.mode(),source.totalQuestionCount(),rows,copiedLevels,null));
    }
    @Transactional public void delete(UUID courseId,UUID id) { access.requireCourseTeacher(courseId); editable(courseId,id); if(jdbc.update("DELETE FROM edu.exam_matrices WHERE id=? AND course_id=? AND lifecycle_status='DRAFT'",id,courseId)!=1)throw conflict("Only a DRAFT Matrix can be deleted"); }
    @Transactional public MatrixDetail publish(UUID courseId,UUID id,PublishRequest request) {
        access.requireCourseTeacher(courseId); MatrixDetail current=editable(courseId,id); if(request==null||request.expectedVersion()!=current.version())throw conflict("Matrix changed before publish; reload and try again"); validateConfirmable(current);
        UUID actor=signedInUserPort.findAuthenticatedUser().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication is required")).getId();
        if(jdbc.update("UPDATE edu.exam_matrices SET lifecycle_status='PUBLISHED',version=version+1,published_by=?,published_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=? AND lifecycle_status='DRAFT' AND version=?",actor,id,courseId,request.expectedVersion())!=1)throw conflict("Matrix changed before publish; reload and try again"); return load(courseId,id);
    }
    public void validateConfirmable(MatrixDetail matrix) {
        // Temporary product decision: automatic drafts may be saved/published
        // before their row coverage and allocation configuration is complete.
        if (matrix.mode() == MatrixMode.AUTOMATIC) return;
        List<String> errors=new ArrayList<>(); if(matrix.rows().isEmpty())errors.add("Matrix needs at least one row"); if(matrix.customLearningLevels().isEmpty())errors.add("Matrix needs at least one custom learning level");
        List<Cell> configured=matrix.rows().stream().flatMap(r->r.cells().stream()).toList();
        List<Integer> allocated=matrix.mode()==MatrixMode.AUTOMATIC?allocate(configured.size(),matrix.totalQuestionCount()):configured.stream().map(Cell::questionCount).toList();
        for(int i=0;i<matrix.rows().size();i++) { Row r=matrix.rows().get(i); if(r.chapterId()==null)errors.add("Row "+(i+1)+" is missing Chapter"); if(r.mappedLearningOutcomeId()==null)errors.add("Row "+(i+1)+" is missing mapped learning outcome"); int offset=0; for(int j=0;j<i;j++)offset+=matrix.rows().get(j).cells().size(); boolean contributes=false; for(int j=0;j<r.cells().size();j++)if(allocated.get(offset+j)>0)contributes=true; if(!contributes)errors.add("Row "+(i+1)+" must receive at least one question"); }
        if(!errors.isEmpty())throw badRequest(String.join("; ",errors));
    }
    public static List<Integer> allocate(int selectedCellCount,Integer total) { if(selectedCellCount<=0)throw badRequest("Automatic Matrix needs at least one selected cell"); if(total==null||total<=0)throw badRequest("Automatic Matrix total question count must be greater than zero"); int base=total/selectedCellCount,rem=total%selectedCellCount; List<Integer> result=new ArrayList<>();for(int i=0;i<selectedCellCount;i++)result.add(base+(i<rem?1:0));return result; }
    public AllocationPreview previewAllocation(AllocationPreviewRequest request) { return new AllocationPreview(allocate(request.selectedCellCount(),request.totalQuestionCount())); }
    private MatrixDetail load(UUID courseId,UUID id) {
        MatrixMetadata m=jdbc.query("SELECT id,course_id,title,description,mode,total_question_count,lifecycle_status,version,published_by,published_at,created_at,updated_at FROM edu.exam_matrices WHERE id=? AND course_id=?",rs->rs.next()?new MatrixMetadata(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),MatrixMode.valueOf(rs.getString(5)),rs.getObject(6,Integer.class),LifecycleStatus.valueOf(rs.getString(7)),rs.getLong(8),rs.getObject(9,UUID.class),rs.getObject(10,LocalDateTime.class),rs.getObject(11,LocalDateTime.class),rs.getObject(12,LocalDateTime.class)):null,id,courseId); if(m==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Matrix not found: "+id);
        Map<UUID,List<AssetCoverage>> coveragesByLevel=new HashMap<>(); assetCoverages.findByMatrixId(id).forEach(coverage -> coveragesByLevel.computeIfAbsent(coverage.customLearningLevelId(), ignored -> new ArrayList<>()).add(new AssetCoverage(AssetType.valueOf(coverage.assetType()), coverage.percentage())));
        List<LearningLevel> levels=jdbc.query("SELECT id,name,sort_order FROM edu.exam_matrix_custom_learning_levels WHERE matrix_id=? ORDER BY sort_order,id",(rs,n)->new LearningLevel(rs.getObject(1,UUID.class),rs.getString(2),rs.getInt(3),coverageFor(coveragesByLevel,rs.getObject(1,UUID.class))),id); Map<UUID,List<Cell>> cells=new HashMap<>(); jdbc.query("SELECT r.id,c.custom_learning_level_id,c.question_count FROM edu.exam_matrix_rows r JOIN edu.exam_matrix_cells c ON c.row_id=r.id WHERE r.matrix_id=? ORDER BY r.sort_order,r.id",(RowCallbackHandler) rs->cells.computeIfAbsent(rs.getObject(1,UUID.class),x->new ArrayList<>()).add(new Cell(rs.getObject(2,UUID.class),rs.getObject(3,Integer.class))),id);
        List<Row> rows=jdbc.query("SELECT r.id,r.chapter_id,c.title,r.mapped_learning_outcome_id,CASE WHEN lo.prefix_number IS NULL THEN lo.description ELSE 'LO'||lo.prefix_number||' - '||lo.description END,r.sort_order FROM edu.exam_matrix_rows r LEFT JOIN edu.chapters c ON c.chapter_id=r.chapter_id LEFT JOIN edu.learning_outcomes lo ON lo.id=r.mapped_learning_outcome_id WHERE r.matrix_id=? ORDER BY r.sort_order,r.id",(rs,n)->new Row(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getObject(4,UUID.class),rs.getString(5),rs.getInt(6),List.copyOf(cells.getOrDefault(rs.getObject(1,UUID.class),List.of()))),id);
        return new MatrixDetail(m.id,m.courseId,m.title,m.description,m.mode,m.total,m.status,m.version,m.publishedBy,m.publishedAt,rows,levels,m.createdAt,m.updatedAt);
    }
    private void replace(UUID matrixId,MatrixPayload p) { for(int i=0;i<safe(p.customLearningLevels()).size();i++){var l=safe(p.customLearningLevels()).get(i);jdbc.update("INSERT INTO edu.exam_matrix_custom_learning_levels(id,matrix_id,name,sort_order) VALUES(?,?,?,?)",l.id(),matrixId,l.name().trim(),i);for(var coverage:normalizedCoverages(l.assetCoverages()))assetCoverages.insert(new MatrixAssetCoverageRow(l.id(),coverage.assetType().name(),coverage.percentage()));} for(int i=0;i<safe(p.rows()).size();i++){var r=safe(p.rows()).get(i);jdbc.update("INSERT INTO edu.exam_matrix_rows(id,matrix_id,chapter_id,mapped_learning_outcome_id,sort_order) VALUES(?,?,?,?,?)",r.id(),matrixId,r.chapterId(),r.mappedLearningOutcomeId(),i);for(var c:safe(r.cells()))if(p.mode()==MatrixMode.AUTOMATIC||c.questionCount()!=null&&c.questionCount()>0)jdbc.update("INSERT INTO edu.exam_matrix_cells(row_id,custom_learning_level_id,question_count) VALUES(?,?,?)",r.id(),c.customLearningLevelId(),p.mode()==MatrixMode.AUTOMATIC?null:c.questionCount());} }
    private void validate(UUID courseId,MatrixPayload p) { if(p==null||trim(p.title())==null||p.mode()==null)throw badRequest("Matrix title and mode are required"); if(p.mode()==MatrixMode.AUTOMATIC&&(p.totalQuestionCount()==null||p.totalQuestionCount()<=0))throw badRequest("Automatic Matrix total question count must be greater than zero"); if(p.mode()==MatrixMode.MANUAL&&p.totalQuestionCount()!=null)throw badRequest("Manual Matrix cannot have a total question count"); Set<UUID> levels=new HashSet<>();for(var l:safe(p.customLearningLevels())){if(l.id()==null||trim(l.name())==null||!levels.add(l.id()))throw badRequest("Each learning level needs a unique id and name");validateCoverage(l.assetCoverages());}Set<UUID> rows=new HashSet<>();for(var r:safe(p.rows())){if(r.id()==null||!rows.add(r.id()))throw badRequest("Each Matrix row needs a unique id");if(r.chapterId()!=null&&!exists("SELECT 1 FROM edu.chapters WHERE chapter_id=? AND course_id=?",r.chapterId(),courseId))throw badRequest("Chapter does not belong to this course");if(r.mappedLearningOutcomeId()!=null&&!exists("SELECT 1 FROM edu.learning_outcomes WHERE id=? AND course_id=?",r.mappedLearningOutcomeId(),courseId))throw badRequest("Mapped learning outcome does not belong to this course");Set<UUID> seen=new HashSet<>();for(var c:safe(r.cells())){if(c.customLearningLevelId()==null||!levels.contains(c.customLearningLevelId())||!seen.add(c.customLearningLevelId()))throw badRequest("A Matrix row has an invalid or duplicate cell");if(p.mode()==MatrixMode.MANUAL&&(c.questionCount()==null||c.questionCount()<0))throw badRequest("Manual Matrix cells must have non-negative counts");if(p.mode()==MatrixMode.AUTOMATIC&&c.questionCount()!=null)throw badRequest("Automatic Matrix cells use x selection, not a question count");}} }
    private static final List<AssetType> ASSET_TYPES=List.of(AssetType.TEXT,AssetType.IMAGE,AssetType.CODE_MCQ,AssetType.CODE_RUNNER);
    private static List<AssetCoverage> defaults(){return List.of(new AssetCoverage(AssetType.TEXT,100),new AssetCoverage(AssetType.IMAGE,0),new AssetCoverage(AssetType.CODE_MCQ,0),new AssetCoverage(AssetType.CODE_RUNNER,0));}
    private static List<AssetCoverage> normalizedCoverages(List<AssetCoverage> value){return value==null||value.isEmpty()?defaults():value;}
    private static List<AssetCoverage> coverageFor(Map<UUID,List<AssetCoverage>> byLevel,UUID levelId){Map<AssetType,Integer> values=new EnumMap<>(AssetType.class);for(var coverage:byLevel.getOrDefault(levelId,List.of()))values.put(coverage.assetType(),coverage.percentage());return ASSET_TYPES.stream().map(type->new AssetCoverage(type,values.getOrDefault(type,type==AssetType.TEXT?100:0))).toList();}
    private static void validateCoverage(List<AssetCoverage> value){if(value==null||value.isEmpty())return;Set<AssetType> types=EnumSet.noneOf(AssetType.class);int total=0;for(var coverage:value){if(coverage==null||coverage.assetType()==null||!types.add(coverage.assetType()))throw badRequest("Each learning level needs one coverage for every asset type");if(coverage.percentage()<0||coverage.percentage()>100)throw badRequest("Asset coverage percentages must be between 0 and 100");total+=coverage.percentage();}if(types.size()!=ASSET_TYPES.size()||total!=100)throw badRequest("Asset coverage percentages must include every asset type and total 100");}
    private MatrixDetail editable(UUID courseId,UUID id){MatrixDetail m=load(courseId,id);if(m.lifecycleStatus()!=LifecycleStatus.DRAFT)throw conflict("Published Matrix is read-only");return m;} private boolean exists(String sql,UUID id,UUID course){return Boolean.TRUE.equals(jdbc.query(sql,(ResultSetExtractor<Boolean>) rs->rs.next(),id,course));} private static <T> List<T> safe(List<T> v){return v==null?List.of():v;} private static String trim(String v){return v==null||v.isBlank()?null:v.trim();} private static ResponseStatusException badRequest(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private static ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
    private record MatrixMetadata(UUID id,UUID courseId,String title,String description,MatrixMode mode,Integer total,LifecycleStatus status,long version,UUID publishedBy,LocalDateTime publishedAt,LocalDateTime createdAt,LocalDateTime updatedAt){}
    public enum MatrixMode { MANUAL, AUTOMATIC } public enum LifecycleStatus { DRAFT,PUBLISHED } public enum AssetType { TEXT,IMAGE,CODE_MCQ,CODE_RUNNER }
    public record MatrixList(UUID courseId,List<MatrixSummary> matrices){} public record MatrixSummary(UUID id,UUID courseId,String title,String description,MatrixMode mode,Integer totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,LocalDateTime publishedAt,int rowCount,int allocatedQuestionCount,LocalDateTime createdAt,LocalDateTime updatedAt){} public record MatrixDetail(UUID id,UUID courseId,String title,String description,MatrixMode mode,Integer totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,LocalDateTime publishedAt,List<Row> rows,List<LearningLevel> customLearningLevels,LocalDateTime createdAt,LocalDateTime updatedAt){} public record Row(UUID id,UUID chapterId,String chapterName,UUID mappedLearningOutcomeId,String mappedLearningOutcomeName,int sortOrder,List<Cell> cells){} public record Cell(UUID customLearningLevelId,Integer questionCount){} public record AssetCoverage(AssetType assetType,int percentage){} public record LearningLevel(UUID id,String name,int sortOrder,List<AssetCoverage> assetCoverages){}  public record MatrixPayload(String title,String description,MatrixMode mode,Integer totalQuestionCount,List<RowPayload> rows,List<LearningLevelPayload> customLearningLevels,Long expectedVersion){} public record RowPayload(UUID id,UUID chapterId,UUID mappedLearningOutcomeId,List<Cell> cells){} public record LearningLevelPayload(UUID id,String name,List<AssetCoverage> assetCoverages){ public LearningLevelPayload(UUID id,String name){this(id,name,null);} } public record PublishRequest(long expectedVersion){} public record CopyRequest(String title){} public record AllocationPreviewRequest(int selectedCellCount,int totalQuestionCount){} public record AllocationPreview(List<Integer> questionCounts){}
}
