package app.inkwell.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {
    private OAuth2User oauth2User;
    private OAuth2UserInfo userInfo;

    public CustomOAuth2User(OAuth2User oauth2User, OAuth2UserInfo userInfo) {
        this.oauth2User = oauth2User;
        this.userInfo = userInfo;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return userInfo.getName();
    }

    public String getEmail() {
        return userInfo.getEmail();
    }

    public String getId() {
        return userInfo.getId();
    }

    public String getImageUrl() {
        return userInfo.getImageUrl();
    }
}


