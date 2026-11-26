package com.mysms.identity.people.business;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ValidationFailedException;
import com.mysms.identity.people.domain.SelfPersonEntity;
import org.junit.jupiter.api.Test;

/** The shapes the self-service profile accepts beyond the contract's lengths. */
class MyPersonRulesTest {

    private static SelfPersonEntity person() {
        return SelfPersonEntity.builder().displayName("A").build();
    }

    @Test
    void acceptsTypicalContactDetails() {
        SelfPersonEntity p = person();
        p.setPrimaryEmail("asha@example.org");
        p.setPrimaryPhone("+91 98765 43210");
        p.setAlternatePhone("(080) 1234-5678");
        p.setWebsiteUrl("https://example.org/asha");
        p.setAboutMe("Teaches physics.");
        assertThatCode(() -> MyPersonService.assertValid(p)).doesNotThrowAnyException();
        assertThatCode(() -> MyPersonService.assertValid(person())).doesNotThrowAnyException();
    }

    @Test
    void rejectsScriptLinksBadAddressesAndPhones() {
        for (String link : new String[] {"javascript:alert(1)", "ftp://example.org", "https://", "not a url"}) {
            SelfPersonEntity p = person();
            p.setWebsiteUrl(link);
            assertThatThrownBy(() -> MyPersonService.assertValid(p)).as(link).isInstanceOf(ValidationFailedException.class);
        }
        SelfPersonEntity email = person();
        email.setPrimaryEmail("no-at-sign");
        assertThatThrownBy(() -> MyPersonService.assertValid(email)).isInstanceOf(ValidationFailedException.class);
        SelfPersonEntity phone = person();
        phone.setWorkPhone("call me");
        assertThatThrownBy(() -> MyPersonService.assertValid(phone)).isInstanceOf(ValidationFailedException.class);
        SelfPersonEntity about = person();
        about.setAboutMe("x".repeat(2001));
        assertThatThrownBy(() -> MyPersonService.assertValid(about)).isInstanceOf(ValidationFailedException.class);
    }
}
