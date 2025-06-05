package app.inkwell.config;

import app.inkwell.security.CustomOAuth2UserService;
import app.inkwell.security.CustomAuthenticationSuccessHandler;
import app.inkwell.security.JwtAuthenticationFilter;
import app.inkwell.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/Home", "/SignIn", "/SignUp", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/api/stories/random/**").permitAll()
                .requestMatchers("/api/stories/random/**").permitAll()
                .requestMatchers("/api/2fa/**").authenticated()
                .requestMatchers("/2fa/**").permitAll()
                .requestMatchers("/api/debug/**").permitAll()
                .requestMatchers("/Dashboard", "/Writing", "/Statistics").hasAuthority("WRITER") 
                .requestMatchers("/Settings", "/Profile/**", "/api/auth/send-verification-email", "/api/auth/become-writer").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/SignIn")
                .loginProcessingUrl("/api/auth/login")
                .successHandler(customAuthenticationSuccessHandler)
                .permitAll()
            )
            .requestCache(cache -> cache
                .requestCache(new NullRequestCache())
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/SignIn?logout")
                .deleteCookies("JSESSIONID", "jwt", "jwtToken", "rememberDevice")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/SignIn")
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorization"))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/login/oauth2/code/*"))
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptionHandling -> exceptionHandling
               .accessDeniedPage("/access-denied"));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

