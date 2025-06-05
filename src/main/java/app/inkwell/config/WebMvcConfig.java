package app.inkwell.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
                
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
                
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Add fallback controller for Home
        registry.addViewController("/").setViewName("redirect:/Home");
    }
}

