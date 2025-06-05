package app.inkwell.service;

import app.inkwell.model.*;
import app.inkwell.model.Character;
import app.inkwell.repository.CharacterRepository;
import app.inkwell.repository.StoryPlanningElementRepository;
import app.inkwell.repository.WorldBuildingElementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WritingPlanningService {
  
  @Autowired
  private CharacterRepository characterRepository;
  
  @Autowired
  private WorldBuildingElementRepository worldBuildingRepository;
  
  @Autowired
  private StoryPlanningElementRepository planningRepository;
  
  // Character methods
  public Character createCharacter(Story story, String name, String description) {
      Character character = new Character();
      character.setStory(story);
      character.setName(name);
      character.setDescription(description);
      return characterRepository.save(character);
  }
  
  public Character saveCharacter(Character character) {
      return characterRepository.save(character);
  }
  
  public List<Character> getStoryCharacters(Story story) {
      return characterRepository.findByStory(story);
  }
  
  public Optional<Character> getCharacterById(Long id) {
      return characterRepository.findById(id);
  }
  
  // World Building methods
  public WorldBuildingElement createWorldElement(Story story, String title, String content) {
      WorldBuildingElement element = new WorldBuildingElement();
      element.setStory(story);
      element.setTitle(title);
      element.setContent(content);
      return worldBuildingRepository.save(element);
  }
  
  public WorldBuildingElement saveWorldElement(WorldBuildingElement element) {
      return worldBuildingRepository.save(element);
  }
  
  public List<WorldBuildingElement> getStoryWorldElements(Story story) {
      return worldBuildingRepository.findByStory(story);
  }
  
  public Optional<WorldBuildingElement> getWorldElementById(Long id) {
      return worldBuildingRepository.findById(id);
  }
  
  // Story Planning methods
  public StoryPlanningElement createPlanningElement(Story story, String title, String content, 
                                                   String color, int posX, int posY, int width, int height) {
      StoryPlanningElement element = new StoryPlanningElement();
      element.setStory(story);
      element.setTitle(title);
      element.setContent(content);
      element.setColor(color);
      element.setPositionX(posX);
      element.setPositionY(posY);
      element.setWidth(width);
      element.setHeight(height);
      return planningRepository.save(element);
  }
  
  public StoryPlanningElement savePlanningElement(StoryPlanningElement element) {
      return planningRepository.save(element);
  }
  
  public List<StoryPlanningElement> getStoryPlanningElements(Story story) {
      return planningRepository.findByStory(story);
  }
  
  public Optional<StoryPlanningElement> getPlanningElementById(Long id) {
      return planningRepository.findById(id);
  }

  @Transactional
  public void deleteAllPlanningElements(Story story) {
    planningRepository.deleteByStory(story);
    }
}