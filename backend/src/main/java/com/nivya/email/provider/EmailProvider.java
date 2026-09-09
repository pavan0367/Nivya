package com.nivya.email.provider;

import com.nivya.email.dto.EmailSendResult;

/**
 * Provider SPI for email delivery abstraction.
 * Concrete implementations provide Simulation, SMTP, SendGrid, Amazon SES, or custom providers.
 */
public interface EmailProvider {

    /**
     * Unique identifier for the email provider (e.g. SIMULATION, SMTP, SENDGRID, SES).
     */
    String getProviderName();

    /**
     * Transports an email payload to the destination recipient.
     *
     * @param to Destination email address
     * @param subject Email subject
     * @param bodyHtml Formatted HTML message
     * @param bodyText Plaintext message fallback
     * @return EmailSendResult representing transport success or failure
     */
    EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText);
}
