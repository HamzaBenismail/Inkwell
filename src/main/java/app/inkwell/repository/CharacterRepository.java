package app.inkwell.repository;

import app.inkwell.model.Character;
import app.inkwell.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {
    
    List<Character> findByStory(Story story);
}

