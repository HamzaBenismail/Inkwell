package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Version {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "track_url")
    private String soundCloudTrackUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "version_number")
    private Integer versionNumber;
    
    // Define what content type this version belongs to
    @Column(name = "content_type")
    private String contentType; // 'chapter', 'character', 'worldbuilding'
    
    // ID of the content this version belongs to
    @Column(name = "content_id")
    private Long contentId;
}