package com.mysms.iam.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The account e-mails: the code, the link into the web app when configured, the validity, the language. */
class AccountMessagesTest {

    private static final String TOKEN = RefreshTokens.generate(UUID.randomUUID());

    @Test
    void aResetMessageCarriesTheCodeTheLinkAndTheValidity() {
        AccountMessages.Message message = AccountMessages.ofCatalogue("https://app.mysms.test/")
                .passwordReset("a@school.test", TOKEN, 30, Locale.ENGLISH);
        assertThat(message.to()).isEqualTo("a@school.test");
        assertThat(message.subject()).isEqualTo("Reset your MySMS password");
        assertThat(message.body()).contains(TOKEN).contains("for 30 minutes")
                .contains("Or open this link: https://app.mysms.test/app/auth/reset?token=" + TOKEN);
    }

    @Test
    void withoutTheAppAddressOnlyTheCodeIsSent() {
        AccountMessages messages = AccountMessages.ofCatalogue("");
        assertThat(messages.verification("a@school.test", TOKEN, 24, Locale.ENGLISH).body())
                .contains(TOKEN).contains("24 hours").doesNotContain("http");
        assertThat(messages.signUp("a@school.test", TOKEN, 24, Locale.ENGLISH).subject())
                .isEqualTo("Finish creating your MySMS account");
        assertThat(messages.forcedReset("a@school.test", TOKEN, 30, Locale.ENGLISH).body())
                .contains("administrator").contains(TOKEN);
        assertThat(messages.signUpExisting("a@school.test", TOKEN, 30, Locale.ENGLISH).body())
                .contains("already has a MySMS account").contains(TOKEN);
    }

    @Test
    void theVerificationLinkNamesTheChannel() {
        assertThat(AccountMessages.ofCatalogue("https://app.mysms.test").verification("a@school.test", TOKEN, 24,
                Locale.ENGLISH).body()).contains("https://app.mysms.test/app/auth/verify?channel=email&code=" + TOKEN);
    }

    @Test
    void messagesComeInTheRecipientsLanguageAndFallBackToEnglish() {
        AccountMessages messages = AccountMessages.ofCatalogue("");
        assertThat(messages.passwordReset("a@school.test", TOKEN, 30, Locale.forLanguageTag("hi")).subject())
                .isEqualTo("अपना MySMS पासवर्ड रीसेट करें");
        assertThat(messages.passwordReset("a@school.test", TOKEN, 30, Locale.forLanguageTag("fr")).subject())
                .isEqualTo("Reset your MySMS password");
    }
}
