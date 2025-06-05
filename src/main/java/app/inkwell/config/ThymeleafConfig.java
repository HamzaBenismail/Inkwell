package app.inkwell.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.springframework.web.servlet.ViewResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ThymeleafConfig implements WebMvcConfigurer {

    @Bean
    @Description("Thymeleaf Template Resolver")
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // For development
        return templateResolver;
    }

    @Bean
    @Description("Thymeleaf Template Engine")
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        
        // Add Spring Security dialect
        templateEngine.addDialect(new SpringSecurityDialect());
        
        return templateEngine;
    }

    @Bean
    @Description("Thymeleaf View Resolver")
    public ViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");
        viewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        // Add request object to all templates
        viewResolver.setExcludedViewNames(new String[]{"error/*"});
        
        return viewResolver;
    }
}

