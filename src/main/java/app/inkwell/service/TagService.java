package app.inkwell.service;

import app.inkwell.model.Story;
import app.inkwell.model.Tag;
import app.inkwell.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    @Autowired
    private TagRepository tagRepository;

    public List<Tag> getAllTags() {
        logger.debug("Getting all tags");
        return tagRepository.findAll();
    }
    
    public Optional<Tag> getTagByName(String name) {
        logger.debug("Getting tag by name: {}", name);
        return tagRepository.findByName(name);
    }
    
    public List<Story> getStoriesByTagName(String tagName) {
        logger.debug("Getting stories by tag name: {}", tagName);
        Optional<Tag> tagOpt = tagRepository.findByName(tagName);
        if (tagOpt.isPresent()) {
            Tag tag = tagOpt.get();
            // Filter for published stories only
            List<Story> publishedStories = new ArrayList<>();
            for (Story story : tag.getStories()) {
                if (story.getPublished()) {
                    publishedStories.add(story);
                }
            }
            logger.debug("Found {} published stories with tag: {}", publishedStories.size(), tagName);
            return publishedStories;
        }
        logger.debug("Tag not found: {}", tagName);
        return new ArrayList<>();
    }
    
    public Tag createTag(String name) {
        logger.debug("Creating tag: {}", name);
        Tag tag = new Tag();
        tag.setName(name);
        return tagRepository.save(tag);
    }
}

