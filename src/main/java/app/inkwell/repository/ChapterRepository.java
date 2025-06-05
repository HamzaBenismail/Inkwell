package app.inkwell.repository;

import app.inkwell.model.Chapter;
import app.inkwell.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByStoryOrderByChapterNumberAsc(Story story);
    Optional<Chapter> findByStoryAndChapterNumber(Story story, Integer chapterNumber);
    Integer countByStory(Story story);

    List<Chapter> findByStoryAndPublishedTrueOrderByChapterNumberAsc(Story story);
}

