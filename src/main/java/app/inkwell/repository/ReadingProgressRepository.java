package app.inkwell.repository;

import app.inkwell.model.ReadingProgress;
import app.inkwell.model.ReadingStatus;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
    
    Optional<ReadingProgress> findByUserAndStory(User user, Story story);
    
    List<ReadingProgress> findByUserOrderByLastReadAtDesc(User user);
    
    List<ReadingProgress> findByUserAndStatusOrderByLastReadAtDesc(User user, ReadingStatus status);

    List<ReadingProgress> findByUserAndStatus(User user, ReadingStatus status);
}

