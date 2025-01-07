package org.lite.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NotificationService {
    private final JavaMailSender mailSender;
    private final WebClient.Builder webClientBuilder;
    
    @Value("${notifications.email.to}")
    private String emailTo;
    
    @Value("${notifications.slack.webhook-url}")
    private String slackWebhookUrl;
    
    @Value("${notifications.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${notifications.slack.enabled:false}")
    private boolean slackEnabled;

    public NotificationService(JavaMailSender mailSender, WebClient.Builder webClientBuilder) {
        this.mailSender = mailSender;
        this.webClientBuilder = webClientBuilder;
    }

    public void sendEmailAlert(String subject, String message) {
        if (!emailEnabled) {
            log.debug("Email notifications are disabled");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            
            helper.setTo(emailTo);
            helper.setSubject(subject);
            helper.setText(message, true); // true enables HTML content
            
            mailSender.send(mimeMessage);
            log.info("Email alert sent successfully: {}", subject);
        } catch (MessagingException e) {
            log.error("Failed to send email alert: {}", e.getMessage());
        }
    }

    public void sendSlackNotification(String color, String subject, String message) {
        if (!slackEnabled) {
            log.debug("Slack notifications are disabled");
            return;
        }

        String payload = String.format("""
            {
                "attachments": [{
                    "color": "%s",
                    "title": "%s",
                    "text": "%s",
                    "footer": "API Gateway Health Monitor"
                }]
            }""", color, subject, message);

        webClientBuilder.build()
            .post()
            .uri(slackWebhookUrl)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(response -> log.info("Slack notification sent successfully"))
            .doOnError(e -> log.error("Failed to send Slack notification: {}", e.getMessage()))
            .onErrorResume(e -> Mono.empty())
            .subscribe();
    }
} 