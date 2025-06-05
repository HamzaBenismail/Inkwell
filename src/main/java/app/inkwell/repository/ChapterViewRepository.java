package app.inkwell.repository;

import app.inkwell.model.Chapter;
import app.inkwell.model.ChapterView;
import app.inkwell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterViewRepository extends JpaRepository<ChapterView, Long> {
    
    boolean existsByChapterAndUser(Chapter chapter, User user);
    
    boolean existsByChapterAndIpAddress(Chapter chapter, String ipAddress);
    
    long countByChapter(Chapter chapter);
}