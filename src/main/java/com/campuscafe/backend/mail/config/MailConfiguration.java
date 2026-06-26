package com.campuscafe.backend.mail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Configuration
@Slf4j
public class MailConfiguration {

    private final EmailProperties emailProperties;

    public MailConfiguration(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        String host = emailProperties.getHost() != null ? emailProperties.getHost() : "smtp-relay.brevo.com";
        int port = emailProperties.getPort() != null ? emailProperties.getPort() : 587;
        String username = emailProperties.getUsername();
        String password = emailProperties.getPassword();

        log.info("[SMTP CONFIG] Host: {}, Port: {}, Username: {}, Password Length: {}", 
                 host, port, username, password != null ? password.length() : 0);

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
