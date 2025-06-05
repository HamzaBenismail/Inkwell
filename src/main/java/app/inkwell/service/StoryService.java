package app.inkwell.service;

import app.inkwell.model.Chapter;
import app.inkwell.model.ReadingProgress;
import app.inkwell.model.ReadingStatus;
import app.inkwell.model.Story;
import app.inkwell.model.Tag;
import app.inkwell.model.User;
import app.inkwell.repository.ChapterRepository;
import app.inkwell.repository.ReadingProgressRepository;
import app.inkwell.repository.StoryRepository;
import app.inkwell.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StoryService {
    private static final Logger logger = LoggerFactory.getLogger(StoryService.class);

    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private ReadingProgressRepository readingProgressRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public Story createStory(String title, String description, User author, List<String> tagNames) {
        logger.info("Creating story: {} for author: {}", title, author.getUsername());
        
        Story story = new Story();
        story.setTitle(title);
        story.setDescription(description);
        story.setAuthor(author);
        story.setPublished(false);
        story.setReadCount(0);
        story.setCreatedAt(LocalDateTime.now());
        story.setLastUpdated(LocalDateTime.now());
        
        // Save the story first to get an ID
        story = storyRepository.save(story);
        
        // Add tags if provided
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String tagName : tagNames) {
                Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(tagName);
                        return tagRepository.save(newTag);
                    });
                story.getTags().add(tag);
            }
            // Save again with tags
            story = storyRepository.save(story);
        }
        
        logger.info("Story created with ID: {}", story.getId());
        return story;
    }

    public Page<Story> searchStoriesPaged(String keyword, Pageable pageable) {
        logger.debug("Searching stories with keyword: {} (paged)", keyword);
        return storyRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
    }
    
    public Page<Story> getStoriesByTagNamePaged(String tagName, Pageable pageable) {
        logger.debug("Getting stories by tag: {} (paged)", tagName);
        return storyRepository.findByTagName(tagName, pageable);
    }

    public Optional<Story> getStoryById(Long id) {
        logger.debug("Getting story by ID: {}", id);
        return storyRepository.findById(id);
    }

    public List<Story> getAllStories() {
        logger.debug("Getting all stories");
        return storyRepository.findAll();
    }

    public Story saveStory(Story story) {
        logger.debug("Saving story: {}", story.getId());
        story.setLastUpdated(LocalDateTime.now());
        return storyRepository.save(story);
    }

    public Page<Story> getPublishedStories(Pageable pageable) {
        logger.debug("Getting published stories with pageable: {}", pageable);
        return storyRepository.findByPublishedTrue(pageable);
    }
    
    public List<Story> getPublishedStories() {
        logger.debug("Getting all published stories");
        return storyRepository.findByPublishedTrue();
    }
    

    public List<Story> getUserStories(User author) {
        logger.debug("Getting stories for user: {}", author.getUsername());
        return storyRepository.findByAuthor(author);
    }

    public List<Story> searchStories(String keyword) {
        logger.debug("Searching stories with keyword: {}", keyword);
        return storyRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    public List<Story> getFeaturedStories(int limit) {
        logger.debug("Getting {} featured stories", limit);
        // Get all published stories
        List<Story> stories = storyRepository.findByPublishedTrue();
        
        // Log the number of stories found
        logger.debug("Found {} published stories", stories.size());
        
        // If there are no stories or fewer stories than requested, return all available
        if (stories.isEmpty() || stories.size() <= limit) {
            return stories;
        }

        // Shuffle the stories randomly
        Collections.shuffle(stories);

        // Return the requested number of stories
        return stories.subList(0, limit);
    }
    
    // Default limit for featured stories
    public List<Story> getFeaturedStories() {
        // Get all published stories
        Page<Story> allPublished = storyRepository.findByPublishedTrue(Pageable.unpaged());
        List<Story> stories = allPublished.getContent();
        
        // Filter out fan fiction stories
        stories = stories.stream()
            .filter(story -> story.getTags().stream().noneMatch(tag -> tag.getName().equals("Fan Fiction")))
            .collect(Collectors.toList());
        
        // If there are no stories or fewer stories than requested, return all available
        if (stories.isEmpty() || stories.size() <= 6) {
            return stories;
        }
        
        // Shuffle the stories randomly
        Collections.shuffle(stories);
        
        // Return the requested number of stories
        return stories.subList(0, Math.min(6, stories.size())); // Default to 6 featured stories
    }


    // Add this new method to get all non-fan fiction stories with sorting
    public List<Story> getAllNonFanFictionStories(Sort sort) {
        return storyRepository.findByTagsNameNotIgnoreCase("Fan Fiction", sort);
    }

    // Modify these methods to accept a limit parameter
    public List<Story> getRecentStories(int limit) {
        return storyRepository.findByTagsNameNotIgnoreCase("Fan Fiction",
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    public List<Story> getPopularStories(int limit) {
        return storyRepository.findByTagsNameNotIgnoreCase("Fan Fiction",
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "readCount"))).getContent();
    }

    public List<Story> getRecentlyUpdatedStories(int limit) {
        return storyRepository.findByTagsNameNotIgnoreCase("Fan Fiction",
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "lastUpdated"))).getContent();
    }
    
    public List<Story> getRecentStories() {
        Page<Story> recentStories = storyRepository.findStoriesWithoutTag("Fan Fiction", 
            PageRequest.of(0, 6, Sort.by("lastUpdated").descending()));
        return recentStories.getContent();
    }
    
    public List<Story> getPopularStories() {
        return getPopularStories(6); // Default to 6 stories
    }

    public List<Story> getRecentlyUpdatedStories() {
        return getRecentlyUpdatedStories(6); // Default to 6 stories
    }
    
    public List<Story> getRandomPublishedStories(int limit) {
        logger.debug("Getting {} random published stories", limit);
        List<Story> publishedStories = getPublishedStories();
        if (publishedStories.isEmpty()) {
            return publishedStories;
        }
        
        Collections.shuffle(publishedStories);
        return publishedStories.size() <= limit ? 
            publishedStories : publishedStories.subList(0, limit);
    }
    
    public String getSimplifiedUserStoriesJson(User user) {
        try {
            List<Story> userStories = getUserStories(user);
            List<Map<String, Object>> simplifiedStories = userStories.stream()
                .map(story -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", story.getId());
                    map.put("title", story.getTitle());
                    map.put("description", story.getDescription());
                    map.put("coverImage", story.getCoverImage());
                    map.put("published", story.getPublished());
                    map.put("lastUpdated", story.getLastUpdated().toString());
                    
                    // Add tags if available
                    if (story.getTags() != null) {
                        map.put("tags", story.getTags().stream()
                            .map(tag -> {
                                Map<String, Object> tagMap = new HashMap<>();
                                tagMap.put("id", tag.getId());
                                tagMap.put("name", tag.getName());
                                return tagMap;
                            })
                            .collect(Collectors.toList()));
                    }
                    
                    return map;
                })
                .collect(Collectors.toList());
            
            return objectMapper.writeValueAsString(simplifiedStories);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing stories to JSON", e);
            return "[]";
        }
    }
    
    public List<Chapter> getStoryChapters(Story story) {
        logger.debug("Getting chapters for story: {}", story.getId());
        return chapterRepository.findByStoryOrderByChapterNumberAsc(story);
    }
    
    // Fix the getPublishedChapters method to ensure it returns all published chapters
    public List<Chapter> getPublishedChapters(Story story) {
        logger.debug("Getting published chapters for story: {}", story.getId());
        List<Chapter> publishedChapters = chapterRepository.findByStoryAndPublishedTrueOrderByChapterNumberAsc(story);
        logger.debug("Found {} published chapters", publishedChapters.size());
        return publishedChapters;
    }
    
    public Optional<Chapter> getChapterById(Long id) {
        logger.debug("Getting chapter by ID: {}", id);
        return chapterRepository.findById(id);
    }
    
    @Transactional
    public Chapter saveChapter(Chapter chapter) {
        logger.debug("Saving chapter: {}", chapter.getId());
        // Update the story's last updated timestamp when saving a chapter
        Story story = chapter.getStory();
        story.setLastUpdated(LocalDateTime.now());
        storyRepository.save(story);
        
        return chapterRepository.save(chapter);
    }
    
    public Chapter addChapter(Story story, String title, String content, int chapterNumber) {
        logger.debug("Adding chapter {} to story: {}", chapterNumber, story.getId());
        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setChapterNumber(chapterNumber);
        chapter.setPublished(false);
        chapter.setWordCount(countWords(content));
        chapter.setReadCount(0);
        
        // Update the story's last updated timestamp
        story.setLastUpdated(LocalDateTime.now());
        storyRepository.save(story);
        
        return chapterRepository.save(chapter);
    }
    
    public int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String[] words = text.split("\\s+");
        return words.length;
    }
    
    public Optional<ReadingProgress> getUserReadingProgressForStory(User user, Story story) {
        logger.debug("Getting reading progress for user: {} and story: {}", user.getId(), story.getId());
        return readingProgressRepository.findByUserAndStory(user, story);
    }
    
    public void recordReadProgress(User user, Story story, Chapter chapter, int progressPercentage) {
        logger.debug("Recording read progress for user: {} and story: {}", user.getId(), story.getId());
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findByUserAndStory(user, story);
        ReadingProgress progress;
        
        boolean isNewReader = !progressOpt.isPresent();
        boolean isNewChapter = false;
        
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            // Check if this is a new chapter for this user
            isNewChapter = progress.getCurrentChapter() == null || 
                !progress.getCurrentChapter().getId().equals(chapter.getId());
            
            progress.setCurrentChapter(chapter);
            progress.setProgressPercentage(progressPercentage);
            progress.setLastReadAt(LocalDateTime.now());
        } else {
            progress = new ReadingProgress();
            progress.setUser(user);
            progress.setStory(story);
            progress.setCurrentChapter(chapter);
            progress.setProgressPercentage(progressPercentage);
            progress.setStatus(ReadingStatus.READING);
            progress.setLastReadAt(LocalDateTime.now());
            isNewChapter = true;
        }
        
        readingProgressRepository.save(progress);
        
        // Only increment the story's read count for new readers
        if (isNewReader) {
            story.setReadCount(story.getReadCount() + 1);
            storyRepository.save(story);
        }
        
        // Only increment the chapter's read count for new chapter reads
        if (isNewChapter) {
            chapter.setReadCount(chapter.getReadCount() + 1);
            chapterRepository.save(chapter);
        }
    }
    
    public void recordStoryView(User user, Story story) {
        logger.debug("Recording story view for user: {} and story: {}", user.getId(), story.getId());
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findByUserAndStory(user, story);
        
        if (!progressOpt.isPresent()) {
            ReadingProgress progress = new ReadingProgress();
            progress.setUser(user);
            progress.setStory(story);
            progress.setProgressPercentage(0);
            progress.setStatus(ReadingStatus.READING);
            progress.setLastReadAt(LocalDateTime.now());
            readingProgressRepository.save(progress);
            
            // Only increment the story's read count for new readers
            story.setReadCount(story.getReadCount() + 1);
            storyRepository.save(story);
        } else {
            // Just update last read time for existing readers
            ReadingProgress progress = progressOpt.get();
            progress.setLastReadAt(LocalDateTime.now());
            readingProgressRepository.save(progress);
        }
    }
    
    public void updateReadingStatus(User user, Story story, ReadingStatus status) {
        logger.debug("Updating reading status to {} for user: {} and story: {}", status, user.getId(), story.getId());
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findByUserAndStory(user, story);
        ReadingProgress progress;
        
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            progress.setStatus(status);
            progress.setLastReadAt(LocalDateTime.now());
        } else {
            progress = new ReadingProgress();
            progress.setUser(user);
            progress.setStory(story);
            progress.setProgressPercentage(0);
            progress.setStatus(status);
            progress.setLastReadAt(LocalDateTime.now());
        }
        
        readingProgressRepository.save(progress);
    }
    
    public List<ReadingProgress> getUserReadingProgressByStatus(User user, ReadingStatus status) {
        logger.debug("Getting reading progress for user: {} with status: {}", user.getId(), status);
        return readingProgressRepository.findByUserAndStatus(user, status);
    }

    public Story updateStory(Long id, Story story) {
        logger.debug("Updating story with id: {}, story: {}", id, story);
        if (storyRepository.existsById(id)) {
            story.setId(id);
            return storyRepository.save(story);
        }
        return null;
    }

    // Get fan fiction stories
    public Page<Story> getFanFictionStories(Pageable pageable) {
        System.out.println("Fetching fan fiction stories");
        return storyRepository.findByTagName("Fan Fiction", pageable);
    }
    
    // Get non-fan fiction stories
    public Page<Story> getNonFanFictionStories(Pageable pageable) {
        System.out.println("Fetching non-fan fiction stories");
        return storyRepository.findStoriesWithoutTag("Fan Fiction", pageable);
    }
    
    // Get featured fan fiction stories
    public List<Story> getFeaturedFanFictionStories() {
        Page<Story> fanFictionStories = getFanFictionStories(Pageable.unpaged());
        List<Story> stories = fanFictionStories.getContent();
        
        // If there are no stories or fewer stories than requested, return all available
        if (stories.isEmpty() || stories.size() <= 6) {
            return stories;
        }
        
        // Shuffle the stories randomly
        List<Story> mutableList = new ArrayList<>(stories);
        Collections.shuffle(mutableList);
        
        // Return up to 6 stories
        return mutableList.subList(0, Math.min(6, mutableList.size()));
    }

    public Page<Story> getStoriesByGenre(String genre, Pageable pageable) {
        return storyRepository.findByGenreAndPublishedTrue(genre, pageable);
    }
    
    /**
     * Gets all available genres
     */
    public List<String> getAllGenres() {
        return storyRepository.findDistinctGenres();
    }
    
    public List<Story> getHighestRatedStories(int limit) {
    // Get stories sorted by average rating in descending order
    return storyRepository.findByPublishedTrueOrderByAverageRatingDesc(
        PageRequest.of(0, limit)).getContent();
}

public List<Story> getLowestRatedStories(int limit) {
    // Get stories sorted by average rating in ascending order
    return storyRepository.findByPublishedTrueOrderByAverageRatingAsc(
        PageRequest.of(0, limit)).getContent();
}

// Default versions with no limit parameter
public List<Story> getHighestRatedStories() {
    return getHighestRatedStories(6); // Default to 6 stories
}

public List<Story> getLowestRatedStories() {
    return getLowestRatedStories(6); // Default to 6 stories
}

// Update methods in StoryService to use the new repository methods
public Page<Story> getStoriesByRatingHighToLow(Pageable pageable) {
    return storyRepository.findAllByPublishedTrueOrderByAverageRatingDescNullsLast(pageable);
}

public Page<Story> getStoriesByRatingLowToHigh(Pageable pageable) {
    return storyRepository.findAllByPublishedTrueOrderByAverageRatingAscNullsLast(pageable);
}

public Page<Story> searchFanFictionStoriesPaged(String searchTerm, Pageable pageable) {
    // First get all stories matching the search term
    Page<Story> allSearchResults = searchStoriesPaged(searchTerm, pageable);
    
    // Filter to only fan fiction stories
    List<Story> fanFictionStories = allSearchResults.getContent().stream()
        .filter(story -> story.getTags().stream()
            .anyMatch(tag -> "Fan Fiction".equalsIgnoreCase(tag.getName())))
        .collect(Collectors.toList());
    
    // Create a new page with the filtered stories
    return new org.springframework.data.domain.PageImpl<>(
        fanFictionStories, 
        allSearchResults.getPageable(), 
        fanFictionStories.size()
    );
}

// Get fan fiction stories by tag name with pagination
public Page<Story> getFanFictionStoriesByTagNamePaged(String tagName, Pageable pageable) {
    // Since "Fan Fiction" itself is a tag, we handle it specially
    if ("Fan Fiction".equalsIgnoreCase(tagName)) {
        return getFanFictionStories(pageable);
    }
    
    // For other tags, get stories with both the Fan Fiction tag and the specified tag
    Page<Story> taggedStories = getStoriesByTagNamePaged(tagName, pageable);
    
    // Filter to only fan fiction stories
    List<Story> fanFictionStories = taggedStories.getContent().stream()
        .filter(story -> story.getTags().stream()
            .anyMatch(tag -> "Fan Fiction".equalsIgnoreCase(tag.getName())))
        .collect(Collectors.toList());
    
    // Create a new page with the filtered stories
    return new org.springframework.data.domain.PageImpl<>(
        fanFictionStories, 
        taggedStories.getPageable(), 
        fanFictionStories.size()
    );
}

public Page<Story> getNonFanFictionStoriesByGenre(String genre, Pageable pageable) {
    // Get stories by genre, excluding fan fiction
    return storyRepository.findByTagsNameAndTagsNameNot(genre, "Fan Fiction", pageable);
}

// Get fan fiction stories by rating high to low
public Page<Story> getFanFictionStoriesByRatingHighToLow(Pageable pageable) {
    // Get all stories sorted by rating
    Page<Story> allRatedStories = getStoriesByRatingHighToLow(pageable);
    
    // Filter to only fan fiction stories
    List<Story> fanFictionStories = allRatedStories.getContent().stream()
        .filter(story -> story.getTags().stream()
            .anyMatch(tag -> "Fan Fiction".equalsIgnoreCase(tag.getName())))
        .collect(Collectors.toList());
    
    // Create a new page with the filtered stories
    return new org.springframework.data.domain.PageImpl<>(
        fanFictionStories, 
        allRatedStories.getPageable(), 
        fanFictionStories.size()
    );
}

// Get fan fiction stories by rating low to high
public Page<Story> getFanFictionStoriesByRatingLowToHigh(Pageable pageable) {
    // Get all stories sorted by rating
    Page<Story> allRatedStories = getStoriesByRatingLowToHigh(pageable);
    
    // Filter to only fan fiction stories
    List<Story> fanFictionStories = allRatedStories.getContent().stream()
        .filter(story -> story.getTags().stream()
            .anyMatch(tag -> "Fan Fiction".equalsIgnoreCase(tag.getName())))
        .collect(Collectors.toList());
    
    // Create a new page with the filtered stories
    return new org.springframework.data.domain.PageImpl<>(
        fanFictionStories, 
        allRatedStories.getPageable(), 
        fanFictionStories.size()
    );
}

// Add this method to StoryService
public Page<Story> getTrendingStories(Pageable pageable) {
    return storyRepository.findByPublishedTrueOrderByTrendingScoreDesc(pageable);
}

// getStoriesByAuthor
public List<Story> getStoriesByAuthor(User author) {
    return storyRepository.findByAuthor(author);
}

public Page<Story> getStoriesByTagNameAndExcludeTag(String tagName, String excludeTag, Pageable pageable) {
    return storyRepository.findByTagNameAndExcludeTag(tagName, excludeTag, pageable);
}

}

