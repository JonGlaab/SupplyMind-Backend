package com.supplymind.platform_core.service.communication;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class GmailAdapter implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendEmail(String to, String subject, String body, File attachment) {
        try {
            System.out.println("📤 Sending email to: " + to);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // 'true' means HTML body

            // Handle Attachment
            if (attachment != null && attachment.exists()) {
                FileSystemResource file = new FileSystemResource(attachment);
                helper.addAttachment(file.getFilename(), file);
            }

            mailSender.send(message);
            System.out.println("✅ Email sent successfully!");

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }
}