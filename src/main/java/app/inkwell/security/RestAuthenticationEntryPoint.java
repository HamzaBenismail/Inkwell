package app.inkwell.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, 
                         AuthenticationException authException) throws IOException, ServletException {
        // For AJAX requests, return 401
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        } else {
            // For regular browser requests, redirect to login page
            response.sendRedirect("/SignIn");
        }
    }
}