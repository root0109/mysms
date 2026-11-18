package com.mysms.iam.business;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Builds the account e-mails iam sends (Builder over the message catalogue {@code mail/iam-mail*.properties}): plain
 * text in the recipient's language, holding the one-time code, a link into the web app when its address is
 * configured, and how long the code works — and no other personal data (no names, no login names, no account ids).
 */
public final class AccountMessages {

    /** Where the bundles are; a language is added by a {@code mail/iam-mail_<language>.properties}. */
    public static final String BUNDLE = "mail/iam-mail";

    private final MessageSource catalogue;
    private final String appUrl;

    AccountMessages(MessageSource catalogue, String appUrl) {
        this.catalogue = catalogue;
        String trimmed = appUrl == null ? "" : appUrl.trim();
        this.appUrl = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    /** The catalogue over {@link #BUNDLE}; a language without a bundle gets English. */
    public static AccountMessages ofCatalogue(String appUrl) {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename(BUNDLE);
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        return new AccountMessages(source, appUrl);
    }

    /** A password-reset code the account holder asked for (iam.RequestPasswordReset). */
    public Message passwordReset(String to, String token, long minutes, Locale locale) {
        return message("mail.passwordReset", to, locale, token, resetLink(token, locale), minutes);
    }

    /** A password reset an administrator required (iam.ForcePasswordReset). */
    public Message forcedReset(String to, String token, long minutes, Locale locale) {
        return message("mail.forcedReset", to, locale, token, resetLink(token, locale), minutes);
    }

    /** A new self-registered account: the code sets the first password and proves the address (iam.SignUp). */
    public Message signUp(String to, String token, long hours, Locale locale) {
        return message("mail.signUp", to, locale, token, resetLink(token, locale), hours);
    }

    /** A sign-up for an address that already has an account: a reset code instead of a new account. */
    public Message signUpExisting(String to, String token, long minutes, Locale locale) {
        return message("mail.signUpExisting", to, locale, token, resetLink(token, locale), minutes);
    }

    /** An e-mail verification code (iam.RequestVerification). */
    public Message verification(String to, String code, long hours, Locale locale) {
        return message("mail.verification", to, locale, code,
                link("/app/auth/verify?channel=email&code=" + encode(code), locale), hours);
    }

    private Message message(String key, String to, Locale locale, String code, String link, long validity) {
        Object[] args = {code, link, Long.toString(validity)};
        return new Message(to, catalogue.getMessage(key + ".subject", null, locale),
                catalogue.getMessage(key + ".body", args, locale));
    }

    private String resetLink(String token, Locale locale) {
        return link("/app/auth/reset?token=" + encode(token), locale);
    }

    /** The "open this link" line for a path of the web app, or nothing when its address is not configured. */
    private String link(String pathAndQuery, Locale locale) {
        if (appUrl.isEmpty()) {
            return "";
        }
        return catalogue.getMessage("mail.link", new Object[] {appUrl + pathAndQuery}, locale) + "\n\n";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * @param to      recipient address
     * @param subject subject line
     * @param body    plain-text body
     */
    public record Message(String to, String subject, String body) {
    }
}
