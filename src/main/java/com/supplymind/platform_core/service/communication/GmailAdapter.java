package com.supplymind.platform_core.service.communication;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class GmailAdapter implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendEmail(String to, String subject, String body, File attachment) {
        try {
            log.info("📤 Sending email to: {}", to);

            MimeMessage message = mailSender.createMimeMessage();
            // True = Multipart (needed for attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);


            if (attachment != null && attachment.exists()) {
                FileSystemResource file = new FileSystemResource(attachment);
                helper.addAttachment(file.getFilename(), file);
            }

            mailSender.send(message);
            log.info("✅ Email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send email", e);
            throw new RuntimeException("Failed to send email", e);
        } finally {
            if (attachment != null && attachment.exists()) {
                boolean deleted = attachment.delete();
                if (!deleted) {
                    log.warn("⚠️ Could not delete temp file: {}", attachment.getAbsolutePath());
                } else {
                    log.debug("🗑️ Temp file cleaned up.");
                }
            }
        }
    }
}