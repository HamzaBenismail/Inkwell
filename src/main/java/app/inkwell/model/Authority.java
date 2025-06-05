package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "authorities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Authority implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "authorities")
    private Set<User> users = new HashSet<>();

    public Authority(String name) {
        this.name = name;
    }

    @Override
    public String getAuthority() {
        return this.name;
    }

    // Custom equals and hashCode methods to prevent infinite recursion
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Authority authority = (Authority) o;
        return id != null && Objects.equals(id, authority.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    // @ManyToMany(fetch = FetchType.EAGER)
    // @JoinTable(
    //     name = "user_authorities",
    //     joinColumns = @JoinColumn(name = "user_id"),
    //     inverseJoinColumns = @JoinColumn(name = "authority_id")
    // )
    // private Set<Authority> authorities = new HashSet<>();

}

