package app.inkwell.service;

import app.inkwell.model.Chapter;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.model.ReadingProgress;
import app.inkwell.model.ReadingStatus;
import app.inkwell.repository.ChapterRepository;
import app.inkwell.repository.ReadingProgressRepository;
import app.inkwell.repository.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChapterService {
    private static final Logger logger = LoggerFactory.getLogger(ChapterService.class);

    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private ReadingProgressRepository readingProgressRepository;

    /**
     * Get a chapter by its ID
     */
    public Optional<Chapter> getChapterById(Long id) {
        return chapterRepository.findById(id);
    }

    /**
     * Get all chapters for a story
     */
    public List<Chapter> getChaptersByStory(Story story) {
        return chapterRepository.findByStoryOrderByChapterNumberAsc(story);
    }

    /**
     * Get a specific chapter by story and chapter number
     */
    public Optional<Chapter> getChapterByStoryAndNumber(Story story, int chapterNumber) {
        return chapterRepository.findByStoryAndChapterNumber(story, chapterNumber);
    }

    /**
     * Create a new chapter
     */
    @Transactional
    public Chapter createChapter(Story story, String title, String content, int chapterNumber) {
        Chapter chapter = new Chapter();
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setStory(story);
        chapter.setChapterNumber(chapterNumber);
        chapter.setCreatedAt(LocalDateTime.now());
        chapter.setLastUpdated(LocalDateTime.now());
        chapter.setWordCount(countWords(content));
        chapter.setPublished(false);
        
        // Update story last updated time
        story.setLastUpdated(LocalDateTime.now());
        storyRepository.save(story);
        
        return chapterRepository.save(chapter);
    }

    /**
     * Save an existing chapter
     */
    @Transactional
    public Chapter saveChapter(Chapter chapter) {
        // Update last updated time
        chapter.setLastUpdated(LocalDateTime.now());
        
        // Update word count if content has changed
        chapter.setWordCount(countWords(chapter.getContent()));
        
        // Update story last updated time
        Story story = chapter.getStory();
        story.setLastUpdated(LocalDateTime.now());
        storyRepository.save(story);
        
        return chapterRepository.save(chapter);
    }

    /**
     * Publish a chapter
     */
    @Transactional
    public Chapter publishChapter(Chapter chapter) {
        chapter.setPublished(true);
        chapter.setLastUpdated(LocalDateTime.now());
        
        // If this is the first published chapter, also publish the story
        Story story = chapter.getStory();
        if (!story.getPublished()) {
            story.setPublished(true);
            story.setLastUpdated(LocalDateTime.now());
            storyRepository.save(story);
        }
        
        return chapterRepository.save(chapter);
    }

    /**
     * Unpublish a chapter
     */
    @Transactional
    public Chapter unpublishChapter(Chapter chapter) {
        chapter.setPublished(false);
        chapter.setLastUpdated(LocalDateTime.now());
        return chapterRepository.save(chapter);
    }

    /**
     * Delete a chapter
     */
    @Transactional
    public void deleteChapter(Chapter chapter) {
        chapterRepository.delete(chapter);
        
        // Update story last updated time
        Story story = chapter.getStory();
        story.setLastUpdated(LocalDateTime.now());
        storyRepository.save(story);
    }

    /**
     * Count words in content
     */
    private int countWords(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        String[] words = content.split("\\s+");
        return words.length;
    }

    /**
     * Record that a user has read a chapter
     * Only counts once per user per chapter
     */
    @Transactional
    public void recordChapterRead(Chapter chapter, User user) {
        if (user == null || chapter == null) {
            logger.warn("Cannot record chapter read: user or chapter is null");
            return;
        }
        
        logger.debug("Recording chapter read for user: {} and chapter: {}", user.getUsername(), chapter.getId());
        
        Story story = chapter.getStory();
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
            progress.setLastReadAt(LocalDateTime.now());
            if (progress.getStatus() == null) {
                progress.setStatus(ReadingStatus.READING);
            }
        } else {
            progress = new ReadingProgress();
            progress.setUser(user);
            progress.setStory(story);
            progress.setCurrentChapter(chapter);
            progress.setProgressPercentage(0);
            progress.setStatus(ReadingStatus.READING);
            progress.setLastReadAt(LocalDateTime.now());
            isNewChapter = true;
        }
        
        readingProgressRepository.save(progress);
        
        // Only increment the story's read count for new readers
        if (isNewReader) {
            story.setReadCount(story.getReadCount() + 1);
            storyRepository.save(story);
            logger.debug("Incremented story read count for story: {}", story.getId());
        }
        
        // Only increment the chapter's read count for new chapter reads
        if (isNewChapter) {
            chapter.setReadCount(chapter.getReadCount() + 1);
            chapterRepository.save(chapter);
            logger.debug("Incremented chapter read count for chapter: {}", chapter.getId());
        }
    }
}