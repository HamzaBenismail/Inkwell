package app.inkwell.repository;

import app.inkwell.model.Story;
import app.inkwell.model.StoryLike;
import app.inkwell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryLikeRepository extends JpaRepository<StoryLike, Long> {
    
    Optional<StoryLike> findByUserAndStory(User user, Story story);
    
    int countByStory(Story story);

    List<StoryLike> findByUser(User user);
}

