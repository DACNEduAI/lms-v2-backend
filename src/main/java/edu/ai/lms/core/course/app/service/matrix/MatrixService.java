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
import edu.ai.lms.core.course.app.port.in.command.PublishCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.usecase.PublishCcctProfileUseCase;
import lombok.RequiredArgsConstructor;

/** Course-owned matrix authoring with manual quotas or automatic cell allocation. */
@Service
@RequiredArgsConstructor
public class MatrixService {
    private final JdbcTemplate jdbc;
    private final CourseTeacherAccess access;
    private final LoadSignedInUserPort signedInUserPort;
    private final MatrixAssetCoverageRepository assetCoverages;
    private final PublishCcctProfileUseCase publishCcct;

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
        access.requireCourseTeacher(courseId); payload=withDefaultOutcomeCognitiveLevels(payload); validate(courseId,payload); UUID id=UUID.randomUUID();
        UUID profileId=singleCcct(courseId);
        jdbc.update("INSERT INTO edu.exam_matrices(id,course_id,ccct_profile_id,ccct_published_at,title,description,mode,total_question_count) VALUES(?,?,?,?,?,?,?,?)",id,courseId,profileId,null,payload.title().trim(),trim(payload.description()),payload.mode().name(),payload.totalQuestionCount());
        replace(id,profileId,payload); return load(courseId,id);
    }
    @Transactional public MatrixDetail update(UUID courseId,UUID id,MatrixPayload payload) {
        access.requireCourseTeacher(courseId); MatrixDetail old=editable(courseId,id);
        if(payload==null||payload.expectedVersion()==null||payload.expectedVersion()!=old.version()) throw conflict("Matrix changed before update; reload and try again");
        payload=withDefaultOutcomeCognitiveLevels(payload);
        validate(courseId,payload);
        if(jdbc.update("UPDATE edu.exam_matrices SET title=?,description=?,mode=?,total_question_count=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=? AND lifecycle_status='DRAFT' AND version=?",payload.title().trim(),trim(payload.description()),payload.mode().name(),payload.totalQuestionCount(),id,courseId,payload.expectedVersion())!=1) throw conflict("Matrix changed before update; reload and try again");
        jdbc.update("DELETE FROM edu.exam_matrix_rows WHERE matrix_id=?",id);
        jdbc.update("DELETE FROM edu.exam_matrix_outcome_cognitive_levels WHERE matrix_id=?",id);
        jdbc.update("DELETE FROM edu.exam_matrix_custom_learning_levels WHERE matrix_id=?",id);
        replace(id,old.ccctProfileId(),payload);
        return load(courseId,id);
    }
    @Transactional public MatrixDetail copyAsDraft(UUID courseId,UUID sourceId,CopyRequest request) {
        access.requireCourseTeacher(courseId); MatrixDetail source=load(courseId,sourceId); Map<UUID,UUID> levels=new HashMap<>();
        List<LearningLevelPayload> copiedLevels=source.customLearningLevels().stream().map(x->{UUID id=UUID.randomUUID();levels.put(x.id(),id);return new LearningLevelPayload(id,x.name(),x.assetCoverages());}).toList();
        List<RowPayload> rows=source.rows().stream().map(r->new RowPayload(UUID.randomUUID(),r.ccctItemId(),r.mappedLearningOutcomeId(),r.cells().stream().map(c->new Cell(levels.get(c.customLearningLevelId()),c.questionCount())).toList())).toList();
        return create(courseId,new MatrixPayload(request==null||trim(request.title())==null?source.title()+" (Copy)":trim(request.title()),source.description(),source.mode(),source.totalQuestionCount(),rows,copiedLevels,source.outcomeCognitiveLevels(),null));
    }
    @Transactional public void delete(UUID courseId,UUID id) { access.requireCourseTeacher(courseId); editable(courseId,id); if(jdbc.update("DELETE FROM edu.exam_matrices WHERE id=? AND course_id=? AND lifecycle_status='DRAFT'",id,courseId)!=1)throw conflict("Only a DRAFT Matrix can be deleted"); }
    @Transactional public MatrixDetail publish(UUID courseId,UUID id,PublishRequest request) {
        access.requireCourseTeacher(courseId); MatrixDetail current=editable(courseId,id); if(request==null||request.expectedVersion()!=current.version())throw conflict("Matrix changed before publish; reload and try again");
        Object[] source=jdbc.query("SELECT status,version,published_at FROM edu.ccct_profiles WHERE id=?",rs->rs.next()?new Object[]{rs.getString(1),rs.getLong(2),rs.getObject(3,LocalDateTime.class)}:null,current.ccctProfileId());
        if(source==null) throw badRequest("Course CCCT is required");
        if("DRAFT".equals(source[0])) publishCcct.publish(new PublishCcctProfileCommand(courseId,current.ccctProfileId(),(Long)source[1]));
        LocalDateTime revisionAt=jdbc.queryForObject("SELECT published_at FROM edu.ccct_profiles WHERE id=?",LocalDateTime.class,current.ccctProfileId());
        current=load(courseId,id); validateConfirmable(current);
        UUID actor=signedInUserPort.findAuthenticatedUser().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication is required")).getId();
        if(jdbc.update("UPDATE edu.exam_matrices SET lifecycle_status='PUBLISHED',ccct_published_at=?,version=version+1,published_by=?,published_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=? AND lifecycle_status='DRAFT' AND version=?",revisionAt,actor,id,courseId,request.expectedVersion())!=1)throw conflict("Matrix changed before publish; reload and try again"); return load(courseId,id);
    }
    public void validateConfirmable(MatrixDetail matrix) {
        Integer sourceState=jdbc.query("SELECT CASE WHEN status='PUBLISHED' THEN 1 ELSE 0 END FROM edu.ccct_profiles WHERE id=?",rs->rs.next()?rs.getInt(1):0,matrix.ccctProfileId());
        if(sourceState==null||sourceState!=1) throw badRequest("Matrix source CCCT must be Published before publishing this Matrix");
        List<String> errors=new ArrayList<>(); if(matrix.rows().isEmpty())errors.add("Matrix needs at least one row"); if(matrix.customLearningLevels().isEmpty())errors.add("Matrix needs at least one custom learning level");
        Set<UUID> mappedOutcomes=matrix.outcomeCognitiveLevels().stream().map(OutcomeCognitiveLevel::learningOutcomeId).collect(java.util.stream.Collectors.toSet());
        List<Cell> configured=matrix.rows().stream().flatMap(r->r.cells().stream()).toList();
        List<Integer> allocated=matrix.mode()==MatrixMode.AUTOMATIC?allocate(configured.size(),matrix.totalQuestionCount()):configured.stream().map(Cell::questionCount).toList();
        for(int i=0;i<matrix.rows().size();i++) { Row r=matrix.rows().get(i); if(r.chapterId()==null||r.contentDetail()==null)errors.add("Row "+(i+1)+" references CCCT content that no longer exists"); if(r.mappedLearningOutcomeId()==null)errors.add("Row "+(i+1)+" is missing mapped learning outcome"); else if(!mappedOutcomes.contains(r.mappedLearningOutcomeId()))errors.add("Row "+(i+1)+" is missing its column Bloom level"); int offset=0; for(int j=0;j<i;j++)offset+=matrix.rows().get(j).cells().size(); boolean contributes=false; for(int j=0;j<r.cells().size();j++)if(allocated.get(offset+j)>0)contributes=true; if(!contributes)errors.add("Row "+(i+1)+" must receive at least one question"); }
        if(!errors.isEmpty())throw badRequest(String.join("; ",errors));
    }
    public static List<Integer> allocate(int selectedCellCount,Integer total) { if(selectedCellCount<=0)throw badRequest("Automatic Matrix needs at least one selected cell"); if(total==null||total<=0)throw badRequest("Automatic Matrix total question count must be greater than zero"); int base=total/selectedCellCount,rem=total%selectedCellCount; List<Integer> result=new ArrayList<>();for(int i=0;i<selectedCellCount;i++)result.add(base+(i<rem?1:0));return result; }
    public AllocationPreview previewAllocation(AllocationPreviewRequest request) { return new AllocationPreview(allocate(request.selectedCellCount(),request.totalQuestionCount())); }
    private MatrixDetail load(UUID courseId,UUID id) {
        MatrixMetadata m=jdbc.query("SELECT id,course_id,ccct_profile_id,ccct_published_at,title,description,mode,total_question_count,lifecycle_status,version,published_by,published_at,created_at,updated_at FROM edu.exam_matrices WHERE id=? AND course_id=?",rs->rs.next()?new MatrixMetadata(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getObject(4,LocalDateTime.class),rs.getString(5),rs.getString(6),MatrixMode.valueOf(rs.getString(7)),rs.getObject(8,Integer.class),LifecycleStatus.valueOf(rs.getString(9)),rs.getLong(10),rs.getObject(11,UUID.class),rs.getObject(12,LocalDateTime.class),rs.getObject(13,LocalDateTime.class),rs.getObject(14,LocalDateTime.class)):null,id,courseId); if(m==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Matrix not found: "+id);
        Map<UUID,List<AssetCoverage>> coveragesByLevel=new HashMap<>(); assetCoverages.findByMatrixId(id).forEach(coverage -> coveragesByLevel.computeIfAbsent(coverage.customLearningLevelId(), ignored -> new ArrayList<>()).add(new AssetCoverage(AssetType.valueOf(coverage.assetType()), coverage.percentage())));
        List<LearningLevel> levels=jdbc.query("SELECT id,name,sort_order FROM edu.exam_matrix_custom_learning_levels WHERE matrix_id=? ORDER BY sort_order,id",(rs,n)->new LearningLevel(rs.getObject(1,UUID.class),rs.getString(2),rs.getInt(3),coverageFor(coveragesByLevel,rs.getObject(1,UUID.class))),id); Map<UUID,List<Cell>> cells=new HashMap<>(); jdbc.query("SELECT r.id,c.custom_learning_level_id,c.question_count FROM edu.exam_matrix_rows r JOIN edu.exam_matrix_cells c ON c.row_id=r.id WHERE r.matrix_id=? ORDER BY r.sort_order,r.id",(RowCallbackHandler) rs->cells.computeIfAbsent(rs.getObject(1,UUID.class),x->new ArrayList<>()).add(new Cell(rs.getObject(2,UUID.class),rs.getObject(3,Integer.class))),id);
        List<Row> rows=jdbc.query("""
                SELECT r.id,r.chapter_id,coalesce(rc.title,cc.title,c.title),r.ccct_item_id,
                  coalesce(rc.chapter_code,cc.chapter_code),coalesce(ri.content,i.content),coalesce(ri.content_code,i.content_code),
                  r.mapped_learning_outcome_id,CASE WHEN lo.prefix_number IS NULL THEN lo.description ELSE 'LO'||lo.prefix_number||' - '||lo.description END,r.sort_order
                FROM edu.exam_matrix_rows r JOIN edu.exam_matrices m ON m.id=r.matrix_id
                LEFT JOIN edu.ccct_items i ON m.ccct_published_at IS NULL AND i.id=r.ccct_item_id
                LEFT JOIN edu.ccct_chapters cc ON cc.id=i.chapter_id
                LEFT JOIN edu.ccct_revision_items ri ON m.ccct_published_at IS NOT NULL AND ri.profile_id=m.ccct_profile_id AND ri.published_at=m.ccct_published_at AND ri.id=r.ccct_item_id
                LEFT JOIN edu.ccct_revision_chapters rc ON rc.profile_id=ri.profile_id AND rc.published_at=ri.published_at AND rc.id=ri.chapter_id
                LEFT JOIN edu.chapters c ON c.chapter_id=r.chapter_id LEFT JOIN edu.learning_outcomes lo ON lo.id=r.mapped_learning_outcome_id
                WHERE r.matrix_id=? ORDER BY r.sort_order,r.id
                """,(rs,n)->{UUID rowId=rs.getObject(1,UUID.class); List<Cell> rowCells=List.copyOf(cells.getOrDefault(rowId,List.of())); return new Row(rowId,rs.getObject(2,UUID.class),rs.getString(3),rs.getObject(4,UUID.class),rs.getString(5),rs.getString(6),rs.getString(7),rs.getObject(8,UUID.class),rs.getString(9),rs.getInt(10),rowCells);},id);
        List<OutcomeCognitiveLevel> outcomeCognitiveLevels=jdbc.query("SELECT learning_outcome_id,cognitive_level_code FROM edu.exam_matrix_outcome_cognitive_levels WHERE matrix_id=? ORDER BY learning_outcome_id",(rs,n)->new OutcomeCognitiveLevel(rs.getObject(1,UUID.class),rs.getString(2)),id);
        return new MatrixDetail(m.id,m.courseId,m.ccctProfileId,m.ccctPublishedAt,m.title,m.description,m.mode,m.total,m.status,m.version,m.publishedBy,m.publishedAt,rows,levels,outcomeCognitiveLevels,m.createdAt,m.updatedAt);
    }
    private void replace(UUID matrixId,UUID profileId,MatrixPayload p) {
        for(int i=0;i<safe(p.customLearningLevels()).size();i++) {
            var level=safe(p.customLearningLevels()).get(i);
            jdbc.update("INSERT INTO edu.exam_matrix_custom_learning_levels(id,matrix_id,name,sort_order) VALUES(?,?,?,?)",level.id(),matrixId,level.name().trim(),i);
            for(var coverage:normalizedCoverages(level.assetCoverages())) assetCoverages.insert(new MatrixAssetCoverageRow(level.id(),coverage.assetType().name(),coverage.percentage()));
        }
        for(var level:safe(p.outcomeCognitiveLevels())) jdbc.update("INSERT INTO edu.exam_matrix_outcome_cognitive_levels(matrix_id,learning_outcome_id,cognitive_level_code) VALUES(?,?,?)",matrixId,level.learningOutcomeId(),level.cognitiveLevelCode());
        for(int i=0;i<safe(p.rows()).size();i++) {
            var row=safe(p.rows()).get(i);
            UUID chapterId=chapterForItem(profileId,row.ccctItemId());
            jdbc.update("INSERT INTO edu.exam_matrix_rows(id,matrix_id,chapter_id,ccct_item_id,mapped_learning_outcome_id,sort_order) VALUES(?,?,?,?,?,?)",row.id(),matrixId,chapterId,row.ccctItemId(),row.mappedLearningOutcomeId(),i);
            for(var cell:safe(row.cells())) jdbc.update("INSERT INTO edu.exam_matrix_cells(row_id,custom_learning_level_id,question_count) VALUES(?,?,?)",row.id(),cell.customLearningLevelId(),p.mode()==MatrixMode.AUTOMATIC?null:cell.questionCount());
        }
    }
    private void validate(UUID courseId,MatrixPayload p) {
        if(p==null||trim(p.title())==null||p.mode()==null) throw badRequest("Matrix title and mode are required");
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
        Map<UUID,String> cognitiveLevelByOutcome=new HashMap<>();
        Set<String> cognitiveLevelCodes=Set.of("REMEMBER","UNDERSTAND","APPLY","ANALYZE","EVALUATE","CREATE");
        for(var level:safe(p.outcomeCognitiveLevels())) {
            if(level==null||level.learningOutcomeId()==null||!outcomeIds.add(level.learningOutcomeId())||!exists("SELECT 1 FROM edu.learning_outcomes WHERE id=? AND course_id=?",level.learningOutcomeId(),courseId)) throw badRequest("Each LO Bloom selection must reference a unique outcome in this course");
            if(level.cognitiveLevelCode()==null||!cognitiveLevelCodes.contains(level.cognitiveLevelCode())) throw badRequest("Each LO Bloom selection must use one of the six Bloom levels");
            cognitiveLevelByOutcome.put(level.learningOutcomeId(),level.cognitiveLevelCode());
        }
        Map<UUID,String> cognitiveLevelByCll=new HashMap<>();
        for(var level:safe(p.customLearningLevels())) if(level!=null&&level.id()!=null) cognitiveLevelByCll.put(level.id(),cognitiveLevelCodeForName(level.name()));
        Set<UUID> rows=new HashSet<>();
        for(var row:safe(p.rows())) {
            if(row==null) throw badRequest("Each Matrix row must be valid");
            if(row.id()==null||!rows.add(row.id())||row.ccctItemId()==null) throw badRequest("Each Matrix row needs a unique id and CCCT content item");
            if(!exists("SELECT 1 FROM edu.ccct_items i JOIN edu.ccct_chapters c ON c.id=i.chapter_id WHERE i.id=? AND c.profile_id=?",row.ccctItemId(),profileId)) throw badRequest("Matrix content item does not belong to this course CCCT");
            if(row.mappedLearningOutcomeId()==null||!exists("SELECT 1 FROM edu.learning_outcomes WHERE id=? AND course_id=?",row.mappedLearningOutcomeId(),courseId)) throw badRequest("Mapped learning outcome does not belong to this course");
            if(!outcomeIds.contains(row.mappedLearningOutcomeId())) throw badRequest("Each selected LO column needs a Bloom level");
            List<Cell> rowCells=safe(row.cells());
            if(rowCells.size()!=1) throw badRequest("Each Matrix row must select exactly one learning-outcome cell");
            Cell cell=rowCells.get(0);
            if(cell==null||cell.customLearningLevelId()==null||!levels.contains(cell.customLearningLevelId())) throw badRequest("A Matrix row has an invalid cell");
            if(!Objects.equals(cognitiveLevelByOutcome.get(row.mappedLearningOutcomeId()),cognitiveLevelByCll.get(cell.customLearningLevelId()))) throw badRequest("A Matrix row must use the Bloom CLL assigned to its LO column");
            if(p.mode()==MatrixMode.MANUAL&&(cell.questionCount()==null||cell.questionCount()<=0)) throw badRequest("Manual Matrix cells must have a positive count");
            if(p.mode()==MatrixMode.AUTOMATIC&&cell.questionCount()!=null) throw badRequest("Automatic Matrix cells use selection, not a question count");
        }
    }
    private MatrixPayload withDefaultOutcomeCognitiveLevels(MatrixPayload payload) {
        if(payload==null) return null;
        List<OutcomeCognitiveLevel> assignments=new ArrayList<>(safe(payload.outcomeCognitiveLevels()));
        Map<UUID,String> assigned=new LinkedHashMap<>();
        assignments.stream().filter(Objects::nonNull).forEach(level->{if(level.learningOutcomeId()!=null)assigned.putIfAbsent(level.learningOutcomeId(),level.cognitiveLevelCode());});
        List<LearningLevelPayload> levels=new ArrayList<>(safe(payload.customLearningLevels()));
        Map<UUID,String> namesByLevel=levels.stream().filter(level->level!=null&&level.id()!=null&&level.name()!=null).collect(java.util.stream.Collectors.toMap(LearningLevelPayload::id,LearningLevelPayload::name,(first,ignored)->first));
        Map<String,UUID> idsByCognitiveLevel=new HashMap<>();
        levels.stream().filter(Objects::nonNull).filter(level->level.id()!=null).forEach(level->{String code=cognitiveLevelCodeForName(level.name());if(code!=null)idsByCognitiveLevel.putIfAbsent(code,level.id());});
        List<RowPayload> rows=new ArrayList<>();
        for(RowPayload row:safe(payload.rows())) {
            if(row==null) { rows.add(null); continue; }
            UUID outcomeId=row.mappedLearningOutcomeId();
            String code=outcomeId==null?null:assigned.get(outcomeId);
            if(outcomeId!=null&&code==null) {
                code=safe(row.cells()).stream().filter(Objects::nonNull).map(Cell::customLearningLevelId).map(namesByLevel::get).map(MatrixService::cognitiveLevelCodeForName).filter(Objects::nonNull).findFirst().orElse("UNDERSTAND");
                assignments.add(new OutcomeCognitiveLevel(outcomeId,code));
                assigned.put(outcomeId,code);
            }
            List<Cell> cells=safe(row.cells());
            if(code!=null&&cells.size()==1&&cells.get(0)!=null) {
                UUID levelId=idsByCognitiveLevel.get(code);
                if(levelId==null) {
                    levelId=UUID.randomUUID();
                    String displayName="APPLY".equals(code)?"Implement":code.charAt(0)+code.substring(1).toLowerCase(Locale.ROOT);
                    levels.add(new LearningLevelPayload(levelId,displayName,defaults()));
                    idsByCognitiveLevel.put(code,levelId);
                }
                Cell cell=cells.get(0);
                cells=List.of(new Cell(levelId,payload.mode()==MatrixMode.AUTOMATIC?null:cell.questionCount()));
            }
            rows.add(new RowPayload(row.id(),row.ccctItemId(),row.mappedLearningOutcomeId(),cells));
        }
        return new MatrixPayload(payload.title(),payload.description(),payload.mode(),payload.totalQuestionCount(),rows,levels,assignments,payload.expectedVersion());
    }
    private static String cognitiveLevelCodeForName(String name) {
        if(name==null) return null;
        return switch(name.trim().toLowerCase(Locale.ROOT)) {
            case "remember", "recall", "recognize" -> "REMEMBER";
            case "understand", "comprehend" -> "UNDERSTAND";
            case "apply", "implement", "implementation" -> "APPLY";
            case "analyze", "analyse", "analysis" -> "ANALYZE";
            case "evaluate", "evaluation" -> "EVALUATE";
            case "create", "creating", "synthesize", "design" -> "CREATE";
            default -> null;
        };
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
    private record MatrixMetadata(UUID id,UUID courseId,UUID ccctProfileId,LocalDateTime ccctPublishedAt,String title,String description,MatrixMode mode,Integer total,LifecycleStatus status,long version,UUID publishedBy,LocalDateTime publishedAt,LocalDateTime createdAt,LocalDateTime updatedAt){}
    public enum MatrixMode { MANUAL, AUTOMATIC } public enum LifecycleStatus { DRAFT,PUBLISHED } public enum AssetType { TEXT,IMAGE,CODE_MCQ,CODE_RUNNER }
    public record MatrixList(UUID courseId,List<MatrixSummary> matrices){}
    public record MatrixSummary(UUID id,UUID courseId,String title,String description,MatrixMode mode,Integer totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime publishedAt,int rowCount,int allocatedQuestionCount,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt){}
    public record MatrixDetail(UUID id,UUID courseId,UUID ccctProfileId,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime ccctPublishedAt,String title,String description,MatrixMode mode,Integer totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime publishedAt,List<Row> rows,List<LearningLevel> customLearningLevels,List<OutcomeCognitiveLevel> outcomeCognitiveLevels,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt) {
        public MatrixDetail(UUID id,UUID courseId,UUID ccctProfileId,LocalDateTime ccctPublishedAt,String title,String description,MatrixMode mode,Integer totalQuestionCount,LifecycleStatus lifecycleStatus,long version,UUID publishedBy,LocalDateTime publishedAt,List<Row> rows,List<LearningLevel> customLearningLevels,LocalDateTime createdAt,LocalDateTime updatedAt) {
            this(id,courseId,ccctProfileId,ccctPublishedAt,title,description,mode,totalQuestionCount,lifecycleStatus,version,publishedBy,publishedAt,rows,customLearningLevels,List.of(),createdAt,updatedAt);
        }
    }
    public record Row(UUID id,UUID chapterId,String chapterName,UUID ccctItemId,String chapterCode,String contentDetail,String contentCode,UUID mappedLearningOutcomeId,String mappedLearningOutcomeName,int sortOrder,List<Cell> cells){}
    public record Cell(UUID customLearningLevelId,Integer questionCount){}
    public record AssetCoverage(AssetType assetType,int percentage){}
    public record LearningLevel(UUID id,String name,int sortOrder,List<AssetCoverage> assetCoverages){}
    public record OutcomeCognitiveLevel(UUID learningOutcomeId,String cognitiveLevelCode){}
    public record MatrixPayload(String title,String description,MatrixMode mode,Integer totalQuestionCount,List<RowPayload> rows,List<LearningLevelPayload> customLearningLevels,List<OutcomeCognitiveLevel> outcomeCognitiveLevels,Long expectedVersion) {
        public MatrixPayload(String title,String description,MatrixMode mode,Integer totalQuestionCount,List<RowPayload> rows,List<LearningLevelPayload> customLearningLevels,Long expectedVersion) {
            this(title,description,mode,totalQuestionCount,rows,customLearningLevels,List.of(),expectedVersion);
        }
    }
    public record RowPayload(UUID id,UUID ccctItemId,UUID mappedLearningOutcomeId,List<Cell> cells){}
    public record LearningLevelPayload(UUID id,String name,List<AssetCoverage> assetCoverages){ public LearningLevelPayload(UUID id,String name){this(id,name,null);} }
    public record PublishRequest(long expectedVersion){}
    public record CopyRequest(String title){}
    public record AllocationPreviewRequest(int selectedCellCount,int totalQuestionCount){}
    public record AllocationPreview(List<Integer> questionCounts){}
}
