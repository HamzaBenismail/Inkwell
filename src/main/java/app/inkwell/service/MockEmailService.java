package app.inkwell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;

@Service
@Primary
@Profile("dev") // Only activate this in dev profile
public class MockEmailService implements JavaMailSender {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);
    
    @Override
    public void send(SimpleMailMessage message) {
        logger.info("MOCK EMAIL SERVICE");
        logger.info("To: {}", String.join(", ", message.getTo()));
        logger.info("Subject: {}", message.getSubject());
        logger.info("Text: {}", message.getText());
    }
    
    @Override
    public void send(SimpleMailMessage... messages) {
        for (SimpleMailMessage message : messages) {
            send(message);
        }
    }
    
    @Override
    public void send(MimeMessage message) {
        logger.info("MOCK: MimeMessage email sent");
    }
    
    @Override
    public void send(MimeMessage... messages) {
        for (MimeMessage message : messages) {
            send(message);
        }
    }
    
    @Override
    public MimeMessage createMimeMessage() {
        throw new UnsupportedOperationException("Mock service doesn't support createMimeMessage");
    }
    
    @Override
    public MimeMessage createMimeMessage(InputStream inputStream) {
        throw new UnsupportedOperationException("Mock service doesn't support createMimeMessage");
    }
}