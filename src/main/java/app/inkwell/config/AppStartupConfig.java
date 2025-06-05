package app.inkwell.config;

import app.inkwell.service.TrendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppStartupConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupConfig.class);
    
    @Autowired
    private TrendingService trendingService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeTrendingScores() {
        logger.info("Initializing trending scores on startup");
        trendingService.updateAllTrendingScores();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Initialize trending score cache
        initializeTrendingScores();
    }
}