package app.inkwell.repository;

import app.inkwell.model.Chapter;
import app.inkwell.model.ChapterLike;
import app.inkwell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapterLikeRepository extends JpaRepository<ChapterLike, Long> {

    Optional<ChapterLike> findByUserAndChapter(User user, Chapter chapter);

    int countByChapter(Chapter chapter);
}

