package app.inkwell.service;

import app.inkwell.model.Chapter;
import app.inkwell.model.Character;
import app.inkwell.model.Version;
import app.inkwell.model.WorldBuildingElement;
import app.inkwell.repository.VersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VersionService {

    @Autowired
    private VersionRepository versionRepository;
    
    /**
     * Create a new version for a chapter
     */
    public Version createChapterVersion(Chapter chapter) {
        Integer versionNumber = getNextVersionNumber("chapter", chapter.getId());
        
        Version version = Version.builder()
                .content(chapter.getContent())
                .soundCloudTrackUrl(chapter.getSoundCloudTrackUrl())
                .createdAt(LocalDateTime.now())
                .versionNumber(versionNumber)
                .contentType("chapter")
                .contentId(chapter.getId())
                .build();
        
        return versionRepository.save(version);
    }
    
    /**
     * Create a new version for a character
     */
    public Version createCharacterVersion(Character character) {
        Integer versionNumber = getNextVersionNumber("character", character.getId());
        
        Version version = Version.builder()
                .content(character.getDescription())
                .createdAt(LocalDateTime.now())
                .versionNumber(versionNumber)
                .contentType("character")
                .contentId(character.getId())
                .build();
        
        return versionRepository.save(version);
    }
    
    /**
     * Create a new version for a worldbuilding element
     */
    public Version createWorldBuildingVersion(WorldBuildingElement element) {
        Integer versionNumber = getNextVersionNumber("worldbuilding", element.getId());
        
        Version version = Version.builder()
                .content(element.getContent())
                .createdAt(LocalDateTime.now())
                .versionNumber(versionNumber)
                .contentType("worldbuilding")
                .contentId(element.getId())
                .build();
        
        return versionRepository.save(version);
    }
    
    /**
     * Get all versions for a specific content
     */
    public List<Version> getVersions(String contentType, Long contentId) {
        return versionRepository.findByContentTypeAndContentIdOrderByVersionNumberDesc(contentType, contentId);
    }
    
    /**
     * Get a specific version by ID
     */
    public Optional<Version> getVersionById(Long versionId) {
        return versionRepository.findById(versionId);
    }
    
    /**
     * Get the latest version number for a content item
     */
    private Integer getNextVersionNumber(String contentType, Long contentId) {
        Version latestVersion = versionRepository.findTopByContentTypeAndContentIdOrderByVersionNumberDesc(contentType, contentId);
        
        if (latestVersion == null) {
            return 1;
        } else {
            return latestVersion.getVersionNumber() + 1;
        }
    }
}