package app.inkwell.repository;

import app.inkwell.model.Story;
import app.inkwell.model.StoryPlanningElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryPlanningElementRepository extends JpaRepository<StoryPlanningElement, Long> {
    
    List<StoryPlanningElement> findByStory(Story story);

    void deleteByStory(Story story);
}

