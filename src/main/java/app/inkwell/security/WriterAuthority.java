package app.inkwell.security;

import app.inkwell.model.Authority;
import app.inkwell.model.User;
import app.inkwell.repository.AuthorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class WriterAuthority {

    @Autowired
    private AuthorityRepository authorityRepository;

    public Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Add existing authorities
        for (GrantedAuthority authority : user.getAuthorities()) {
            authorities.add(new SimpleGrantedAuthority(((Authority) authority).getName()));
        }
        
        // Add ROLE_WRITER if the user is a writer
        if (user.isWriter()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_WRITER"));
        }
        
        return authorities;
    }
    
    public void addWriterAuthority(User user) {
        if (user.isWriter()) {
            Optional<Authority> writerAuthority = authorityRepository.findByName("ROLE_WRITER");
            
            if (writerAuthority.isPresent()) {
                user.addAuthority(writerAuthority.get());
            } else {
                Authority newAuthority = new Authority("ROLE_WRITER");
                authorityRepository.save(newAuthority);
                user.addAuthority(newAuthority);
            }
        }
    }
}