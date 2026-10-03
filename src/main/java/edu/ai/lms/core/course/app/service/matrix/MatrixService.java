package edu.ai.lms.core.course.app.service.matrix;

import com.fasterxml.jackson.annotation.JsonFormat;
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
            SELECT m.id,m.course_id,m.title,m.description,m.mode,m.total_question_count,m.lifecycle_status,m.published_by,m.published_at,m.created_at,m.updated_at,
              (SELECT count(*) FROM edu.exam_matrix_rows r WHERE r.matrix_id=m.id),
              CASE WHEN m.mode='MANUAL' THEN coalesce((SELECT sum(c.question_count) FROM edu.exam_matrix_cells c JOIN edu.exam_matrix_rows r ON r.id=c.row_id WHERE r.matrix_id=m.id),0) ELSE m.total_question_count END
            FROM edu.exam_matrices m WHERE m.course_id=? ORDER BY m.updated_at DESC,m.created_at DESC
            """, (rs,n)->new MatrixSummary(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),MatrixMode.valueOf(rs.getString(5)),rs.getObject(6,Integer.class),LifecycleStatus.valueOf(rs.getString(7)),rs.getObject(8,UUID.class),rs.getObject(9,LocalDateTime.class),rs.getInt(12),rs.getInt(13),rs.getObject(10,LocalDateTime.class),rs.getObject(11,LocalDateTime.class)),courseId));
    }

    @Transactional(readOnly = true)
    public MatrixDetail get(UUID courseId, UUID matrixId) { access.requireCourseTeacher(courseId); return load(courseId,matrixId); }

    @Transactional public MatrixDetail create(UUID courseId, MatrixPayload payload) {
        access.requireCourseTeacher(courseId); payload=withDefaultOutcomeLearningLevels(payload); validate(courseId,payload); UUID id=UUID.randomUUID();
        UUID profileId=singleCcct(courseId);
        jdbc.update("INSERT INTO edu.exam_matrices(id,course_id,ccct_profile_id,ccct_snapshot_at,title,description,mode,total_question_count) VALUES(?,?,?,?,?,?,?,?)",id,courseId,profileId,null,payload.title().trim(),trim(payload.description()),payload.mode().name(),payload.totalQuestionCount());
        replace(id,profileId,payload); return load(courseId,id);
    }
    @Transactional public MatrixDetail update(UUID courseId,UUID id,MatrixPayload payload) {
        access.requireCourseTeacher(courseId); MatrixDetail old=editable(courseId,id);
        if(payload==null) throw badRequest("Matrix payload is required");
        payload=withDefaultOutcomeLearningLevels(payload);
        validate(courseId,payload);
        if(jdbc.update("UPDATE edu.exam_matrices SET title=?,description=?,mode=?,total_question_count=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=? AND lifecycle_status='DRAFT'",payload.title().trim(),trim(payload.description()),payload.mode().name(),payload.totalQuestionCount(),id,courseId)!=1) throw conflict("Published Matrix is read-only");
        jdbc.update("DELETE FROM edu.exam_matrix_rows WHERE matrix_id=?",id);
        jdbc.update("DELETE FROM edu.exam_matrix_outcome_learning_levels WHERE matrix_id=?",id);
        jdbc.update("DELETE FROM edu.exam_matrix_custom_learning_levels WHERE matrix_id=?",id);
        replace(id,old.ccctProfileId(),payload);
        return load(courseId,id);
    }
    @Transactional public MatrixDetail copyAsDraft(UUID courseId,UUID sourceId,CopyRequest request) {
        access.requireCourseTeacher(courseId); MatrixDetail source=load(courseId,sourceId); Map<UUID,UUID> levels=new HashMap<>();
        List<LearningLevelPayload> copiedLevels=source.customLearningLevels().stream().map(x->{UUID id=UUID.randomUUID();levels.put(x.id(),id);return new LearningLevelPayload(id,x.name(),x.assetCoverages());}).toList();
        List<RowPayload> rows=source.rows().stream().map(r->new RowPayload(UUID.randomUUID(),r.ccctItemId(),r.mappedLearningOutcomeId(),r.cells().stream().map(c->new Cell(levels.get(c.customLearningLevelId()),c.questionCount())).toList())).toList();
        rows=rows.stream().filter(r->exists("SELECT 1 FROM edu.ccct_items i JOIN edu.ccct_chapters c ON c.id=i.chapter_id WHERE i.id=? AND c.profile_id=?",r.ccctItemId(),singleCcct(courseId))).toList();
        List<OutcomeLearningLevel> assignments=source.outcomeLearningLevels().stream()
                .map(level -> new OutcomeLearningLevel(level.learningOutcomeId(),levels.get(level.customLearningLevelId())))
                .filter(level -> level.customLearningLevelId()!=null).toList();
        return create(courseId,new MatrixPayload(request==null||trim(request.title())==null?source.title()+" (Copy)":trim(request.title()),source.description(),source.mode(),source.totalQuestionCount(),rows,copiedLevels,assignments));
    }
    @Transactional public void delete(UUID courseId,UUID id) { access.requireCourseTeacher(courseId); editable(courseId,id); if(jdbc.update("DELETE FROM edu.exam_matrices WHERE id=? AND course_id=? AND lifecycle_status='DRAFT'",id,courseId)!=1)throw conflict("Only a DRAFT Matrix can be deleted"); }
    @Transactional public MatrixDetail publish(UUID courseId,UUID id) {
        access.requireCourseTeacher(courseId); MatrixDetail current=editable(courseId,id);
        LocalDateTime revisionAt=LocalDateTime.now();
        current=repairLegacyDraftLearningLevelSelections(current);
        validateConfirmable(current);
        UUID actor=signedInUserPort.findAuthenticatedUser().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication is required")).getId();
        snapshot(id,current.ccctProfileId(),revisionAt);
        if(jdbc.update("UPDATE edu.exam_matrices SET lifecycle_status='PUBLISHED',ccct_snapshot_at=?,published_by=?,published_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=? AND lifecycle_status='DRAFT'",revisionAt,actor,id,courseId)!=1)throw conflict("Published Matrix is read-only"); return load(courseId,id);
    }
    public void validateConfirmable(MatrixDetail matrix) {
        List<String> errors=new ArrayList<>(); if(matrix.rows().isEmpty())errors.add("Matrix needs at least one row"); if(matrix.customLearningLevels().isEmpty())errors.add("Matrix needs at least one custom learning level");
        Set<UUID> mappedOutcomes=matrix.outcomeLearningLevels().stream().map(OutcomeLearningLevel::learningOutcomeId).collect(java.util.stream.Collectors.toSet());
        List<Cell> configured=matrix.rows().stream().flatMap(r->r.cells().stream()).toList();
        List<Integer> allocated=matrix.mode()==MatrixMode.AUTOMATIC?allocate(configured.size(),matrix.totalQuestionCount()):configured.stream().map(Cell::questionCount).toList();
        for(int i=0;i<matrix.rows().size();i++) { Row r=matrix.rows().get(i); if(r.chapterId()==null||r.contentDetail()==null)errors.add("Row "+(i+1)+" references CCCT content that no longer exists"); if(r.mappedLearningOutcomeId()==null)errors.add("Row "+(i+1)+" is missing mapped learning outcome"); else if(!mappedOutcomes.contains(r.mappedLearningOutcomeId()))errors.add("Row "+(i+1)+" is missing its column CLL"); int offset=0; for(int j=0;j<i;j++)offset+=matrix.rows().get(j).cells().size(); boolean contributes=false; for(int j=0;j<r.cells().size();j++)if(allocated.get(offset+j)>0)contributes=true; if(!contributes)errors.add("Row "+(i+1)+" must receive at least one question"); }
        if(!errors.isEmpty())throw badRequest(String.join("; ",errors));
    }
    public static List<Integer> allocate(int selectedCellCount,Integer total) { if(selectedCellCount<=0)throw badRequest("Automatic Matrix needs at least one selected cell"); if(total==null||total<=0)throw badRequest("Automatic Matrix total question count must be greater than zero"); int base=total/selectedCellCount,rem=total%selectedCellCount; List<Integer> result=new ArrayList<>();for(int i=0;i<selectedCellCount;i++)result.add(base+(i<rem?1:0));return result; }
    public AllocationPreview previewAllocation(AllocationPreviewRequest request) { return new AllocationPreview(allocate(request.selectedCellCount(),request.totalQuestionCount())); }
    private MatrixDetail load(UUID courseId,UUID id) {
        MatrixMetadata m=jdbc.query("SELECT id,course_id,ccct_profile_id,ccct_snapshot_at,title,description,mode,total_question_count,lifecycle_status,published_by,published_at,created_at,updated_at FROM edu.exam_matrices WHERE id=? AND course_id=?",rs->rs.next()?new MatrixMetadata(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getObject(4,LocalDateTime.class),rs.getString(5),rs.getString(6),MatrixMode.valueOf(rs.getString(7)),rs.getObject(8,Integer.class),LifecycleStatus.valueOf(rs.getString(9)),rs.getObject(10,UUID.class),rs.getObject(11,LocalDateTime.class),rs.getObject(12,LocalDateTime.class),rs.getObject(13,LocalDateTime.class)):null,id,courseId); if(m==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Matrix not found: "+id);
        Map<UUID,List<AssetCoverage>> coveragesByLevel=new HashMap<>(); assetCoverages.findByMatrixId(id).forEach(coverage -> coveragesByLevel.computeIfAbsent(coverage.customLearningLevelId(), ignored -> new ArrayList<>()).add(new AssetCoverage(AssetType.valueOf(coverage.assetType()), coverage.percentage())));
        List<LearningLevel> levels=jdbc.query("SELECT id,name,sort_order FROM edu.exam_matrix_custom_learning_levels WHERE matrix_id=? ORDER BY sort_order,id",(rs,n)->new LearningLevel(rs.getObject(1,UUID.class),rs.getString(2),rs.getInt(3),coverageFor(coveragesByLevel,rs.getObject(1,UUID.class))),id); Map<UUID,List<Cell>> cells=new HashMap<>(); jdbc.query("SELECT r.id,c.custom_learning_level_id,c.question_count FROM edu.exam_matrix_rows r JOIN edu.exam_matrix_cells c ON c.row_id=r.id WHERE r.matrix_id=? ORDER BY r.sort_order,r.id",(RowCallbackHandler) rs->cells.computeIfAbsent(rs.getObject(1,UUID.class),x->new ArrayList<>()).add(new Cell(rs.getObject(2,UUID.class),rs.getObject(3,Integer.class))),id);
        List<Row> rows=jdbc.query("""
                SELECT r.id,r.chapter_id,coalesce(rc.title,cc.title,c.title),r.ccct_item_id,
                  coalesce(rc.chapter_code,cc.chapter_code),coalesce(ri.content,i.content),coalesce(ri.content_code,i.content_code),
                  r.mapped_learning_outcome_id,CASE WHEN lo.prefix_number IS NULL THEN lo.description ELSE 'LO'||lo.prefix_number||' - '||lo.description END,r.sort_order
                FROM edu.exam_matrix_rows r JOIN edu.exam_matrices m ON m.id=r.matrix_id
                LEFT JOIN edu.ccct_items i ON m.ccct_snapshot_at IS NULL AND i.id=r.ccct_item_id
                LEFT JOIN edu.ccct_chapters cc ON cc.id=i.chapter_id
                LEFT JOIN edu.matrix_ccct_snapshot_items ri ON m.ccct_snapshot_at IS NOT NULL AND ri.matrix_id=m.id AND ri.id=r.ccct_item_id
                LEFT JOIN edu.matrix_ccct_snapshot_chapters rc ON rc.matrix_id=ri.matrix_id AND rc.id=ri.chapter_id
                LEFT JOIN edu.chapters c ON c.chapter_id=r.chapter_id LEFT JOIN edu.learning_outcomes lo ON lo.id=r.mapped_learning_outcome_id
                WHERE r.matrix_id=? ORDER BY r.sort_order,r.id
                """,(rs,n)->{UUID rowId=rs.getObject(1,UUID.class); List<Cell> rowCells=List.copyOf(cells.getOrDefault(rowId,List.of())); return new Row(rowId,rs.getObject(2,UUID.class),rs.getString(3),rs.getObject(4,UUID.class),rs.getString(5),rs.getString(6),rs.getString(7),rs.getObject(8,UUID.class),rs.getString(9),rs.getInt(10),rowCells);},id);
        List<OutcomeLearningLevel> outcomeLearningLevels=jdbc.query("SELECT learning_outcome_id,custom_learning_level_id FROM edu.exam_matrix_outcome_learning_levels WHERE matrix_id=? ORDER BY learning_outcome_id",(rs,n)->new OutcomeLearningLevel(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),id);
        return new MatrixDetail(m.id,m.courseId,m.ccctProfileId,m.ccctSnapshotAt,m.title,m.description,m.mode,m.total,m.status,m.publishedBy,m.publishedAt,rows,levels,outcomeLearningLevels,m.createdAt,m.updatedAt);
    }
    private void replace(UUID matrixId,UUID profileId,MatrixPayload p) {
        for(int i=0;i<safe(p.customLearningLevels()).size();i++) {
            var level=safe(p.customLearningLevels()).get(i);
            jdbc.update("INSERT INTO edu.exam_matrix_custom_learning_levels(id,matrix_id,name,sort_order) VALUES(?,?,?,?)",level.id(),matrixId,level.name().trim(),i);
            for(var coverage:normalizedCoverages(level.assetCoverages())) assetCoverages.insert(new MatrixAssetCoverageRow(level.id(),coverage.assetType().name(),coverage.percentage()));
        }
        for(var level:safe(p.outcomeLearningLevels())) jdbc.update("INSERT INTO edu.exam_matrix_outcome_learning_levels(matrix_id,learning_outcome_id,custom_learning_level_id) VALUES(?,?,?)",matrixId,level.learningOutcomeId(),level.customLearningLevelId());
        for(int i=0;i<safe(p.rows()).size();i++) {
            var row=safe(p.rows()).get(i);
            UUID chapterId=chapterForItem(profileId,row.ccctItemId());
            jdbc.update("INSERT INTO edu.exam_matrix_rows(id,matrix_id,chapter_id,ccct_item_id,mapped_learning_outcome_id,sort_order) VALUES(?,?,?,?,?,?)",row.id(),matrixId,chapterId,row.ccctItemId(),row.mappedLearningOutcomeId(),i);
            for(var cell:safe(row.cells())) jdbc.update("INSERT INTO edu.exam_matrix_cells(row_id,custom_learning_level_id,question_count) VALUES(?,?,?)",row.id(),cell.customLearningLevelId(),p.mode()==MatrixMode.AUTOMATIC?null:cell.questionCount());
        }
    }
    void validate(UUID courseId,MatrixPayload p) {
        if(p==null||trim(p.title())==null||p.mode()==null) throw badRequest("Matrix title and mode are required");
        if(safe(p.rows()).isEmpty()) throw badRequest("Matrix needs at least one CCCT content row");
        UUID profileId=singleCcct(courseId);
        if(p.mode()==MatrixMode.AUTOMATIC&&(p.totalQuestionCount()==null||p.totalQuestionCount()<=0)) throw badRequest("Automatic Matrix total question count must be greater than zero");
        if(p.mode()==MatrixMode.MANUAL&&p.totalQuestionCount()!=null) throw badRequest("Manual Matrix cannot have a total question count");
        Set<UUID> levels=new HashSet<>();
        Set<String> levelNames=new HashSet<>();
        for(var level:safe(p.customLearningLevels())) {
            if(level==null) throw badRequest("Each learning level must be valid");
            String name=trim(level.name());
            if(level.id()==null||name==null||!levels.add(level.id())||!levelNames.add(name.toLowerCase(Locale.ROOT))) throw badRequest("Each learning level needs a unique id and name");
            validateCoverage(level.assetCoverages());
        }
        Set<UUID> outcomeIds=new HashSet<>();
        Map<UUID,UUID> levelByOutcome=new HashMap<>();
        for(var level:safe(p.outcomeLearningLevels())) {
            if(level==null||level.learningOutcomeId()==null||level.customLearningLevelId()==null||!outcomeIds.add(level.learningOutcomeId())||!exists("SELECT 1 FROM edu.learning_outcomes WHERE id=? AND course_id=?",level.learningOutcomeId(),courseId)) throw badRequest("Each LO CLL selection must reference a unique outcome in this course");
            if(!levels.contains(level.customLearningLevelId())) throw badRequest("Each LO CLL selection must reference a CLL in this Matrix");
            levelByOutcome.put(level.learningOutcomeId(),level.customLearningLevelId());
        }
        Set<UUID> rows=new HashSet<>();
        for(var row:safe(p.rows())) {
            if(row==null) throw badRequest("Each Matrix row must be valid");
            if(row.id()==null||!rows.add(row.id())||row.ccctItemId()==null) throw badRequest("Each Matrix row needs a unique id and CCCT content item");
            if(!exists("SELECT 1 FROM edu.ccct_items i JOIN edu.ccct_chapters c ON c.id=i.chapter_id WHERE i.id=? AND c.profile_id=?",row.ccctItemId(),profileId)) throw badRequest("Matrix content item does not belong to this course CCCT");
            if(row.mappedLearningOutcomeId()==null||!exists("SELECT 1 FROM edu.learning_outcomes WHERE id=? AND course_id=?",row.mappedLearningOutcomeId(),courseId)) throw badRequest("Mapped learning outcome does not belong to this course");
            if(!outcomeIds.contains(row.mappedLearningOutcomeId())) throw badRequest("Each selected LO column needs a CLL");
            List<Cell> rowCells=safe(row.cells());
            if(rowCells.size()!=1) throw badRequest("Each Matrix row must select exactly one learning-outcome cell");
            Cell cell=rowCells.get(0);
            if(cell==null||cell.customLearningLevelId()==null||!levels.contains(cell.customLearningLevelId())) throw badRequest("A Matrix row has an invalid cell");
            if(!Objects.equals(levelByOutcome.get(row.mappedLearningOutcomeId()),cell.customLearningLevelId())) throw badRequest("A Matrix row must use the CLL selected for its LO column");
            if(p.mode()==MatrixMode.MANUAL&&(cell.questionCount()==null||cell.questionCount()<=0)) throw badRequest("Manual Matrix cells must have a positive count");
            if(p.mode()==MatrixMode.AUTOMATIC&&cell.questionCount()!=null) throw badRequest("Automatic Matrix cells use selection, not a question count");
        }
    }
    /** Derive only unambiguous legacy LO-to-CLL selections at publication time. */
    private MatrixDetail repairLegacyDraftLearningLevelSelections(MatrixDetail matrix) {
        Set<UUID> mappedOutcomes=matrix.rows().stream()
                .map(Row::mappedLearningOutcomeId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> assignedOutcomes=matrix.outcomeLearningLevels().stream()
                .map(OutcomeLearningLevel::learningOutcomeId)
                .collect(java.util.stream.Collectors.toSet());
        if(mappedOutcomes.isEmpty()||assignedOutcomes.containsAll(mappedOutcomes)) return matrix;

        MatrixPayload repaired=withDefaultOutcomeLearningLevels(new MatrixPayload(
                matrix.title(),matrix.description(),matrix.mode(),matrix.totalQuestionCount(),
                matrix.rows().stream().map(row->new RowPayload(row.id(),row.ccctItemId(),row.mappedLearningOutcomeId(),row.cells())).toList(),
                matrix.customLearningLevels().stream().map(level->new LearningLevelPayload(level.id(),level.name(),level.assetCoverages())).toList(),
                matrix.outcomeLearningLevels()));
        jdbc.update("DELETE FROM edu.exam_matrix_outcome_learning_levels WHERE matrix_id=?",matrix.id());
        jdbc.update("DELETE FROM edu.exam_matrix_rows WHERE matrix_id=?",matrix.id());
        jdbc.update("DELETE FROM edu.exam_matrix_custom_learning_levels WHERE matrix_id=?",matrix.id());
        replace(matrix.id(),matrix.ccctProfileId(),repaired);
        return load(matrix.courseId(),matrix.id());
    }

    static MatrixPayload withDefaultOutcomeLearningLevels(MatrixPayload payload) {
        if(payload==null) return null;
        Map<UUID,UUID> assignments=new LinkedHashMap<>();
        safe(payload.outcomeLearningLevels()).stream().filter(Objects::nonNull)
                .filter(level -> level.learningOutcomeId()!=null && level.customLearningLevelId()!=null)
                .forEach(level -> assignments.putIfAbsent(level.learningOutcomeId(),level.customLearningLevelId()));
        for(UUID outcomeId:safe(payload.rows()).stream().filter(Objects::nonNull).map(RowPayload::mappedLearningOutcomeId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet())) {
            if(assignments.containsKey(outcomeId)) continue;
            Set<UUID> levelIds=safe(payload.rows()).stream().filter(Objects::nonNull).filter(row -> outcomeId.equals(row.mappedLearningOutcomeId()))
                    .flatMap(row -> safe(row.cells()).stream()).filter(Objects::nonNull).map(Cell::customLearningLevelId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            if(levelIds.size()==1) assignments.put(outcomeId,levelIds.iterator().next());
        }
        List<OutcomeLearningLevel> normalized=assignments.entrySet().stream().map(entry -> new OutcomeLearningLevel(entry.getKey(),entry.getValue())).toList();
        return new MatrixPayload(payload.title(),payload.description(),payload.mode(),payload.totalQuestionCount(),safe(payload.rows()),safe(payload.customLearningLevels()),normalized);
    }
    private static final List<AssetType> ASSET_TYPES=List.of(AssetType.TEXT,AssetType.IMAGE,AssetType.CODE_MCQ,AssetType.CODE_RUNNER);
    private static List<AssetCoverage> defaults(){return List.of(new AssetCoverage(AssetType.TEXT,100),new AssetCoverage(AssetType.IMAGE,0),new AssetCoverage(AssetType.CODE_MCQ,0),new AssetCoverage(AssetType.CODE_RUNNER,0));}
    private static List<AssetCoverage> normalizedCoverages(List<AssetCoverage> value){return value==null||value.isEmpty()?defaults():value;}
    private static List<AssetCoverage> coverageFor(Map<UUID,List<AssetCoverage>> byLevel,UUID levelId){Map<AssetType,Integer> values=new EnumMap<>(AssetType.class);for(var coverage:byLevel.getOrDefault(levelId,List.of()))values.put(coverage.assetType(),coverage.percentage());return ASSET_TYPES.stream().map(type->new AssetCoverage(type,values.getOrDefault(type,type==AssetType.TEXT?100:0))).toList();}
    private static void validateCoverage(List<AssetCoverage> value){if(value==null||value.isEmpty())return;Set<AssetType> types=EnumSet.noneOf(AssetType.class);int total=0;for(var coverage:value){if(coverage==null||coverage.assetType()==null||!types.add(coverage.assetType()))throw badRequest("Each learning level needs one coverage for every asset type");if(coverage.percentage()<0||coverage.percentage()>100)throw badRequest("Asset coverage percentages must be between 0 and 100");total+=coverage.percentage();}if(types.size()!=ASSET_TYPES.size()||total!=100)throw badRequest("Asset coverage percentages must include every asset type and total 100");}
    private MatrixDetail editable(UUID courseId,UUID id){MatrixDetail m=load(courseId,id);if(m.lifecycleStatus()!=LifecycleStatus.DRAFT)throw conflict("Published Matrix is read-only");return m;}
    private boolean exists(String sql,UUID id,UUID course){return Boolean.TRUE.equals(jdbc.query(sql,(ResultSetExtractor<Boolean>) rs->rs.next(),id,course));}
    private UUID chapterForItem(UUID profileId,UUID itemId){return jdbc.query("SELECT c.course_chapter_id FROM edu.ccct_items i JOIN edu.ccct_chapters c ON c.id=i.chapter_id WHERE i.id=? AND c.profile_id=?",rs->rs.next()?rs.getObject(1,UUID.class):null,itemId,profileId);}
    private UUID singleCcct(UUID courseId){
        UUID profileId = jdbc.query("SELECT id FROM edu.ccct_profiles WHERE course_id=?",rs->rs.next()?rs.getObject(1,UUID.class):null,courseId);
        if (profileId == null) throw badRequest("Create the course CCCT before creating a Matrix");
        return profileId;
    }
    private static <T> List<T> safe(List<T> v){return v==null?List.of():v;}
    private static String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private static ResponseStatusException badRequest(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    private static ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
    private void snapshot(UUID matrixId, UUID profileId, LocalDateTime snapshotAt) {
        jdbc.update("INSERT INTO edu.matrix_ccct_snapshots(matrix_id,profile_id,snapshot_at,name,template_title,markdown_content) SELECT ?,id,?,name,template_title,markdown_content FROM edu.ccct_profiles WHERE id=?",matrixId,snapshotAt,profileId);
        jdbc.update("INSERT INTO edu.matrix_ccct_snapshot_chapters(matrix_id,id,course_chapter_id,chapter_code,title,order_index) SELECT ?,id,course_chapter_id,chapter_code,title,order_index FROM edu.ccct_chapters WHERE profile_id=?",matrixId,profileId);
        jdbc.update("INSERT INTO edu.matrix_ccct_snapshot_items(matrix_id,id,chapter_id,content_code,content,order_index) SELECT ?,i.id,i.chapter_id,i.content_code,i.content,i.order_index FROM edu.ccct_items i JOIN edu.ccct_chapters c ON c.id=i.chapter_id WHERE c.profile_id=?",matrixId,profileId);
    }
    private record MatrixMetadata(UUID id,UUID courseId,UUID ccctProfileId,LocalDateTime ccctSnapshotAt,String title,String description,MatrixMode mode,Integer total,LifecycleStatus status,UUID publishedBy,LocalDateTime publishedAt,LocalDateTime createdAt,LocalDateTime updatedAt){}
    public enum MatrixMode { MANUAL, AUTOMATIC } public enum LifecycleStatus { DRAFT,PUBLISHED } public enum AssetType { TEXT,IMAGE,CODE_MCQ,CODE_RUNNER }
    public record MatrixList(UUID courseId,List<MatrixSummary> matrices){}
    public record MatrixSummary(UUID id,UUID courseId,String title,String description,MatrixMode mode,Integer totalQuestionCount,LifecycleStatus lifecycleStatus,UUID publishedBy,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime publishedAt,int rowCount,int allocatedQuestionCount,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt){}
    public record MatrixDetail(UUID id,UUID courseId,UUID ccctProfileId,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime ccctSnapshotAt,String title,String description,MatrixMode mode,Integer totalQuestionCount,LifecycleStatus lifecycleStatus,UUID publishedBy,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime publishedAt,List<Row> rows,List<LearningLevel> customLearningLevels,List<OutcomeLearningLevel> outcomeLearningLevels,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt) {}
    public record Row(UUID id,UUID chapterId,String chapterName,UUID ccctItemId,String chapterCode,String contentDetail,String contentCode,UUID mappedLearningOutcomeId,String mappedLearningOutcomeName,int sortOrder,List<Cell> cells){}
    public record Cell(UUID customLearningLevelId,Integer questionCount){}
    public record AssetCoverage(AssetType assetType,int percentage){}
    public record LearningLevel(UUID id,String name,int sortOrder,List<AssetCoverage> assetCoverages){}
    public record OutcomeLearningLevel(UUID learningOutcomeId,UUID customLearningLevelId){}
    public record MatrixPayload(String title,String description,MatrixMode mode,Integer totalQuestionCount,List<RowPayload> rows,List<LearningLevelPayload> customLearningLevels,List<OutcomeLearningLevel> outcomeLearningLevels) {
        public MatrixPayload(String title,String description,MatrixMode mode,Integer totalQuestionCount,List<RowPayload> rows,List<LearningLevelPayload> customLearningLevels) {
            this(title,description,mode,totalQuestionCount,rows,customLearningLevels,List.of());
        }
    }
    public record RowPayload(UUID id,UUID ccctItemId,UUID mappedLearningOutcomeId,List<Cell> cells){}
    public record LearningLevelPayload(UUID id,String name,List<AssetCoverage> assetCoverages){ public LearningLevelPayload(UUID id,String name){this(id,name,null);} }
    public record CopyRequest(String title){}
    public record AllocationPreviewRequest(int selectedCellCount,int totalQuestionCount){}
    public record AllocationPreview(List<Integer> questionCounts){}
}
