package app.inkwell.service;

import app.inkwell.model.Chapter;
import app.inkwell.model.ChapterView;
import app.inkwell.model.User;
import app.inkwell.repository.ChapterRepository;
import app.inkwell.repository.ChapterViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ViewTrackingService {
    private static final Logger logger = LoggerFactory.getLogger(ViewTrackingService.class);
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private ChapterViewRepository chapterViewRepository;
    
    /**
     * Track a unique view for a chapter
     * 
     * @param chapter The chapter being viewed
     * @param user The user viewing the chapter (null for anonymous users)
     * @param ipAddress IP address of the viewer (used for anonymous users)
     * @return true if this was a new unique view, false if already viewed
     */
    @Transactional
    public boolean trackChapterView(Chapter chapter, User user, String ipAddress) {
        if (chapter == null) {
            logger.warn("Cannot track view: chapter is null");
            return false;
        }
        
        boolean isNewView = false;
        
        if (user != null) {
            // Check if this user has already viewed this chapter
            if (!chapterViewRepository.existsByChapterAndUser(chapter, user)) {
                // New view by this user
                ChapterView view = new ChapterView(chapter, user);
                chapterViewRepository.save(view);
                isNewView = true;
                
                logger.debug("Recorded new view for user {} on chapter {}", user.getUsername(), chapter.getId());
            }
        } else if (ipAddress != null && !ipAddress.isEmpty()) {
            // Check if this IP address has already viewed this chapter
            if (!chapterViewRepository.existsByChapterAndIpAddress(chapter, ipAddress)) {
                // New view from this IP address
                ChapterView view = new ChapterView(chapter, ipAddress);
                chapterViewRepository.save(view);
                isNewView = true;
                
                logger.debug("Recorded new view from IP {} on chapter {}", ipAddress, chapter.getId());
            }
        }
        
        if (isNewView) {
            // Increment the chapter's view count
            Integer currentViews = chapter.getViews() != null ? chapter.getViews() : 0;
            chapter.setViews(currentViews + 1);
            chapterRepository.save(chapter);
        }
        
        return isNewView;
    }
    
    /**
     * Helper method to extract client IP address
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // Handle multiple IP addresses (proxies)
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress;
    }
}