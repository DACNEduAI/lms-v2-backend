package edu.ai.lms.core.quiz3.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/** Legacy Quiz3 helpers kept aligned with the chapter-owned CCCT tree. */
@Repository @RequiredArgsConstructor
public class Quiz3CcctSourceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    @Transactional public UUID cloneCcct(UUID courseId, UUID sourceId, UUID actorId) {
        Map<String,Object> source=jdbc.queryForMap("SELECT name,template_title,format_version,structured_current,last_mode,markdown_content FROM edu.ccct_profiles WHERE course_id=:courseId AND id=:sourceId",Map.of("courseId",courseId,"sourceId",sourceId));
        UUID cloneId=UUID.randomUUID();
        jdbc.update("INSERT INTO edu.ccct_profiles(id,course_id,name,template_title,status,version,format_version,structured_current,last_mode,markdown_content,created_by) VALUES(:id,:courseId,:name,:template_title,'DRAFT',0,:format_version,:structured_current,:last_mode,:markdown_content,:actorId)",new MapSqlParameterSource(source).addValue("id",cloneId).addValue("courseId",courseId).addValue("name",source.get("name")+"-Copy").addValue("actorId",actorId));
        jdbc.query("SELECT id,course_chapter_id,chapter_code,title,order_index FROM edu.ccct_chapters WHERE profile_id=:sourceId ORDER BY order_index",Map.of("sourceId",sourceId),(RowCallbackHandler) rs->{UUID oldId=rs.getObject("id",UUID.class),newId=UUID.randomUUID(); jdbc.update("INSERT INTO edu.ccct_chapters(id,profile_id,course_chapter_id,chapter_code,title,order_index) VALUES(:id,:profileId,:courseChapterId,:chapterCode,:title,:orderIndex)",new MapSqlParameterSource().addValue("id",newId).addValue("profileId",cloneId).addValue("courseChapterId",rs.getObject("course_chapter_id",UUID.class)).addValue("chapterCode",rs.getString("chapter_code")).addValue("title",rs.getString("title")).addValue("orderIndex",rs.getInt("order_index"))); jdbc.query("SELECT content_code,content,order_index FROM edu.ccct_items WHERE chapter_id=:chapterId ORDER BY order_index",Map.of("chapterId",oldId),(RowCallbackHandler) item->jdbc.update("INSERT INTO edu.ccct_items(id,chapter_id,content_code,content,order_index) VALUES(:id,:chapterId,:contentCode,:content,:orderIndex)",new MapSqlParameterSource().addValue("id",UUID.randomUUID()).addValue("chapterId",newId).addValue("contentCode",item.getString("content_code")).addValue("content",item.getString("content")).addValue("orderIndex",item.getInt("order_index"))));});
        return cloneId;
    }
    @Transactional public UUID createCcctDraft(UUID courseId,String name,UUID actorId) {
        List<Map<String,Object>> chapters=jdbc.queryForList("SELECT chapter_id,title FROM edu.chapters WHERE course_id=:courseId ORDER BY chapter_id",Map.of("courseId",courseId)); if(chapters.isEmpty()) throw new IllegalStateException("Create at least one Course Chapter before creating a CCCT Draft"); UUID profileId=UUID.randomUUID(); jdbc.update("INSERT INTO edu.ccct_profiles(id,course_id,name,template_title,status,version,format_version,structured_current,last_mode,markdown_content,created_by) VALUES(:id,:courseId,:name,'Course Content Coverage Template','DRAFT',0,2,false,'STRUCTURED','',:actorId)",new MapSqlParameterSource().addValue("id",profileId).addValue("courseId",courseId).addValue("name",name).addValue("actorId",actorId)); for(int i=0;i<chapters.size();i++){Map<String,Object> chapter=chapters.get(i);jdbc.update("INSERT INTO edu.ccct_chapters(id,profile_id,course_chapter_id,chapter_code,title,order_index) VALUES(:id,:profileId,:courseChapterId,:chapterCode,:title,:orderIndex)",new MapSqlParameterSource().addValue("id",UUID.randomUUID()).addValue("profileId",profileId).addValue("courseChapterId",chapter.get("chapter_id")).addValue("chapterCode","chapter"+(i+1)).addValue("title",chapter.get("title")).addValue("orderIndex",i));} return profileId;
    }
}
