package com.mysms.people;

import com.mysms.privacyclient.ActivityCode;
import com.mysms.privacyclient.testing.TestConsents;
import java.util.UUID;

/**
 * The consent the standard-operation contracts need: the person crud-spec.json gives a public profile has consented
 * to one, so the generic create/get/update checks run as for any other record.
 */
final class PublicProfileConsents {

    /** crud-spec.json's PublicProfile fixture person. */
    private static final UUID SPEC_PERSON = UUID.fromString("a2da1934-b1b5-5f1f-8564-66e9edef348f");

    private PublicProfileConsents() {
    }

    static void allowSpecPerson() {
        TestConsents.allow(new ActivityCode("people.public_profile"), SPEC_PERSON);
    }
}
