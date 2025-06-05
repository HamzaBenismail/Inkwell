package app.inkwell.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Register resource handler for uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:src/main/resources/static/uploads/");


        // Register resource handler for profile images
        registry.addResourceHandler("/images/profile/**")
                .addResourceLocations("file:src/main/resources/static/images/profile/");
        
        // Register resource handler for default images
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        registry.addResourceHandler("/dict/**")
                .addResourceLocations("classpath:/static/dict/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Add the profile path to the list of allowed paths
        registry.addViewController("/profile").setViewName("Profile");
        // Add case-insensitive alternative
        registry.addViewController("/Profile").setViewName("Profile");

    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Make URL paths case-insensitive
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setAlwaysUseFullPath(true);
        configurer.setUrlPathHelper(urlPathHelper);
    }
}

