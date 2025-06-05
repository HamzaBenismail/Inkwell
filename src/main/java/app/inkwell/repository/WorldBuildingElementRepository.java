package app.inkwell.repository;

import app.inkwell.model.Story;
import app.inkwell.model.WorldBuildingElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorldBuildingElementRepository extends JpaRepository<WorldBuildingElement, Long> {
    
    List<WorldBuildingElement> findByStory(Story story);
}

