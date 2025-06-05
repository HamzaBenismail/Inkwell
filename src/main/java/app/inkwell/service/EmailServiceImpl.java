package app.inkwell.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Override
    public void sendSubscriptionReceipt(String to, String subscriberName, String writerName, String plan, String amount) {
        try {
            logger.info("Preparing to send subscription receipt to {}", to);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Your Inkwell Subscription Receipt");
            
            Context context = new Context();
            context.setVariable("subscriberName", subscriberName);
            context.setVariable("writerName", writerName);
            context.setVariable("plan", plan);
            context.setVariable("amount", amount);
            context.setVariable("subscriptionDate", java.time.LocalDate.now().toString());
            
            String emailContent = templateEngine.process("emails/subscription-receipt", context);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            logger.info("Successfully sent subscription receipt email to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send subscription receipt email: {}", e.getMessage(), e);
        }
    }
}