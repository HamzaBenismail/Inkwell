package app.inkwell.repository;

import app.inkwell.model.Version;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionRepository extends JpaRepository<Version, Long> {
    
    List<Version> findByContentTypeAndContentIdOrderByVersionNumberDesc(String contentType, Long contentId);
    
    Version findTopByContentTypeAndContentIdOrderByVersionNumberDesc(String contentType, Long contentId);
}