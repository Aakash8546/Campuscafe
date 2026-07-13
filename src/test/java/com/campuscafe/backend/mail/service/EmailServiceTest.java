package com.campuscafe.backend.mail.service;

import com.campuscafe.backend.mail.config.EmailProperties;
import com.campuscafe.backend.mail.exception.EmailSendFailedException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private Environment environment;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        lenient().when(emailProperties.getMailFrom()).thenReturn("sender@example.com");
        lenient().when(emailProperties.getMailFromName()).thenReturn("Test Sender");
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
    }

    @Test
    void testSendOtpEmail_Success() {
        lenient().when(emailProperties.getMaxAttempts()).thenReturn(3);
        lenient().when(emailProperties.getInitialDelayMs()).thenReturn(0L);

        emailService.sendOtpEmail("user@example.com", "123456", "EMAIL_VERIFICATION");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendOtpEmail_InvalidEmail() {
        assertThrows(EmailSendFailedException.class, () ->
                emailService.sendOtpEmail("invalid-email", "123456", "EMAIL_VERIFICATION")
        );

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendOtpEmail_RetryAndThenSuccess() {
        when(emailProperties.getMaxAttempts()).thenReturn(3);
        when(emailProperties.getInitialDelayMs()).thenReturn(1L); // 1ms delay for fast test

        doThrow(new MailSendException("SMTP error"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        emailService.sendOtpEmail("user@example.com", "123456", "EMAIL_VERIFICATION");

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void testSendOtpEmail_RetryExhausted() {
        when(emailProperties.getMaxAttempts()).thenReturn(3);
        when(emailProperties.getInitialDelayMs()).thenReturn(1L);

        doThrow(new MailSendException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailSendFailedException exception = assertThrows(EmailSendFailedException.class, () ->
                emailService.sendOtpEmail("user@example.com", "123456", "EMAIL_VERIFICATION")
        );

        assertEquals("Unable to send verification email. Please try again later.", exception.getMessage());
        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }
}
