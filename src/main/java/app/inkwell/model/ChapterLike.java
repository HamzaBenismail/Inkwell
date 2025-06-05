package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "chapter_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

