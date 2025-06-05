package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails, OAuth2User {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(unique = true, nullable = false)
  private String username;
  
  @Column(unique = true, nullable = false)
  private String email;
  
  private String password;
  
  @Column(name = "profile_image")
  private String profileImage;
  
  private String bio;
  
  @Column(name = "created_at")
  private LocalDateTime createdAt;
  
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  @Column(name = "last_login")
  private LocalDateTime lastLogin;
  
  @Column(columnDefinition = "smallint")
  private Integer provider;
  
  @Column(name = "oauth2provider", length = 50)
  private String oauth2Provider;
  
  @Column(name = "oauth2id", length = 255)
  private String oauth2Id;
  
  private boolean enabled = true;
  
  @Column(name = "account_non_expired")
  private boolean accountNonExpired = true;
  
  @Column(name = "account_non_locked")
  private boolean accountNonLocked = true;
  
  @Column(name = "credentials_non_expired")
  private boolean credentialsNonExpired = true;

  @Column(name = "email_verified")
private boolean emailVerified = false;

@Column(name = "verification_token")
private String verificationToken;

@Column(name = "is_writer")
private boolean writer = false;

  @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
  @JsonManagedReference
  private Set<Story> stories = new HashSet<>();

  @Column(name = "using_mfa")
public boolean usingMfa = false;

  @Column(name = "mfa_secret")
  public String mfaSecret;
  
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_authorities",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "authority_id")
  )
  private Set<Authority> authorities = new HashSet<>();
  
  @Transient
  private Map<String, Object> attributes;
    
  public User(String username, String email, String password) {
      this.username = username;
      this.email = email;
      this.password = password;
      this.createdAt = LocalDateTime.now();
      this.provider = 0; // 0 could represent "local" authentication
  }
  
  // Add the missing method
  public void addAuthority(Authority authority) {
      this.authorities.add(authority);
  }
  
  // Getters and Setters
  public Long getId() {
      return id;
  }
  
  public void setId(Long id) {
      this.id = id;
  }
  
  @Override
  public String getUsername() {
      return username;
  }
  
  public void setUsername(String username) {
      this.username = username;
  }
  
  public String getEmail() {
      return email;
  }
  
  public void setEmail(String email) {
      this.email = email;
  }
  
  @Override
  public String getPassword() {
      return password;
  }
  
  public void setPassword(String password) {
      this.password = password;
  }
  
  public String getProfileImage() {
      return profileImage;
  }
  
  public void setProfileImage(String profileImage) {
      this.profileImage = profileImage;
  }
  
  public String getBio() {
      return bio;
  }
  
  public void setBio(String bio) {
      this.bio = bio;
  }
  
  public LocalDateTime getCreatedAt() {
      return createdAt;
  }
  
  public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
  }
  
  public LocalDateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
  }
  
  public LocalDateTime getLastLogin() {
      return lastLogin;
  }
  
  public void setLastLogin(LocalDateTime lastLogin) {
      this.lastLogin = lastLogin;
  }
  
  public Integer getProvider() {
      return provider;
  }
  
  public void setProvider(Integer provider) {
      this.provider = provider;
  }
  
  // Helper method to set provider as string (for backward compatibility)
  public void setProviderAsString(String providerStr) {
      // Convert string to integer based on your mapping
      if (providerStr == null) {
          this.provider = 0; // Default
      } else if (providerStr.equalsIgnoreCase("local")) {
          this.provider = 0;
      } else if (providerStr.equalsIgnoreCase("google")) {
          this.provider = 1;
      } else {
          this.provider = 0; // Default to local if unknown
      }
  }
  
  public String getOauth2Provider() {
      return oauth2Provider;
  }
  
  public void setOauth2Provider(String oauth2Provider) {
      this.oauth2Provider = oauth2Provider;
  }
  
  public String getOauth2Id() {
      return oauth2Id;
  }
  
  public void setOauth2Id(String oauth2Id) {
      this.oauth2Id = oauth2Id;
  }

  public boolean isUsingMfa() {
    return usingMfa;
}

public void setUsingMfa(boolean usingMfa) {
    this.usingMfa = usingMfa;
}

public String getMfaSecret() {
    return mfaSecret;
}

public void setMfaSecret(String mfaSecret) {
    this.mfaSecret = mfaSecret;
}
  
  @Override
  public boolean isEnabled() {
      return enabled;
  }
  
  public void setEnabled(boolean enabled) {
      this.enabled = enabled;
  }
  
  @Override
  public boolean isAccountNonExpired() {
      return accountNonExpired;
  }
  
  public void setAccountNonExpired(boolean accountNonExpired) {
      this.accountNonExpired = accountNonExpired;
  }
  
  @Override
  public boolean isAccountNonLocked() {
      return accountNonLocked;
  }
  
  public void setAccountNonLocked(boolean accountNonLocked) {
      this.accountNonLocked = accountNonLocked;
  }
  
  @Override
  public boolean isCredentialsNonExpired() {
      return credentialsNonExpired;
  }
  
  public void setCredentialsNonExpired(boolean credentialsNonExpired) {
      this.credentialsNonExpired = credentialsNonExpired;
  }
  
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
      return this.authorities;
  }
  
  public void setAuthorities(Set<Authority> authorities) {
      this.authorities = authorities;
  }
  
//   @Override
//   public Map<String, Object> getAttributes() {
//       return attributes;
//   }
  
  public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
  }
  
  @Override
  public String getName() {
      return String.valueOf(id);
  }
  
  // Equals and HashCode
  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      User user = (User) o;
      return Objects.equals(id, user.id);
  }
  
  @Override
  public int hashCode() {
      return Objects.hash(id);
  }
  
  @Override
  public String toString() {
      return "User{" +
              "id=" + id +
              ", username='" + username + '\'' +
              ", email='" + email + '\'' +
              '}';
  }

  public Set<Story> getStories() {
      return stories;
  }

  public void setStories(Set<Story> stories) {
      this.stories = stories;
  }

  public boolean isEmailVerified() {
    return emailVerified;
}

public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
}

public String getVerificationToken() {
    return verificationToken;
}

public void setVerificationToken(String verificationToken) {
    this.verificationToken = verificationToken;
}



// Update the authorities relationship and writer flag
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
    name = "user_authorities",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "authority_id")
)

// Update the isWriter method to check for the actual authority
// public boolean isWriter() {
//     return writer;
// }

// Update the setWriter method - don't modify authorities directly here
public void setWriter(boolean writer) {
    this.writer = writer;
}

@Override
  public Map<String, Object> getAttributes() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", id);
    attributes.put("username", username);
    attributes.put("email", email);
    return attributes;
  }
  
  public boolean isWriter() {
    return getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("WRITER"));
  }

  public boolean isAdmin() {
    return getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ADMIN"));
}
}

