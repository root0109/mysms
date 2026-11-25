package com.mysms.privacyclient;

import com.mysms.core.error.UnavailableException;
import java.util.UUID;

/**
 * The port to privacy's legal holds (design §10): which active holds cover a subject's data in a module. Asked outside
 * any transaction, just before the module removes the subject's data, so a hold placed a moment ago counts.
 */
public interface LegalHolds {

    /**
     * The active holds of the subject and of everyone that cover the module's data.
     *
     * @throws UnavailableException when privacy cannot answer: the caller removes nothing (fail closed)
     */
    ActiveHolds activeHolds(String module, UUID subjectPersonId);
}
