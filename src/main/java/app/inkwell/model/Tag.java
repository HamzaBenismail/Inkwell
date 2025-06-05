package app.inkwell.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @ManyToMany(mappedBy = "tags")
    private Set<Story> stories = new HashSet<>();
    
    // Constructors
    public Tag() {}
    
    public Tag(String name) {
        this.name = name;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Story> getStories() {
        return stories;
    }

    public void setStories(Set<Story> stories) {
        this.stories = stories;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Tag tag = (Tag) o;
        
        return name.equals(tag.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

