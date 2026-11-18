package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** LTI 1.1 Basic Outcomes XML is read safely: the operation, sourcedId and score; anything else is no outcome. */
class LtiOutcomeTest {

    private static final String REPLACE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <imsx_POXEnvelopeRequest xmlns="http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">
              <imsx_POXHeader><imsx_POXRequestHeaderInfo><imsx_version>V1.0</imsx_version>
                <imsx_messageIdentifier>42</imsx_messageIdentifier></imsx_POXRequestHeaderInfo></imsx_POXHeader>
              <imsx_POXBody><replaceResultRequest><resultRecord>
                <sourcedGUID><sourcedId> tenant.sealed </sourcedId></sourcedGUID>
                <result><resultScore><language>en</language><textString>0.85</textString></resultScore></result>
              </resultRecord></replaceResultRequest></imsx_POXBody>
            </imsx_POXEnvelopeRequest>
            """;

    @Test
    void readsAReplaceResultRequest() {
        LtiOutcome outcome = LtiOutcome.parse(REPLACE.getBytes(StandardCharsets.UTF_8)).orElseThrow();
        assertThat(outcome.operation()).isEqualTo("replaceResultRequest");
        assertThat(outcome.sourcedId()).isEqualTo("tenant.sealed");
        assertThat(outcome.score()).isEqualByComparingTo(new BigDecimal("0.85"));
    }

    @Test
    void anythingElseIsNoOutcome() {
        assertThat(LtiOutcome.parse("not xml".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(LtiOutcome.parse("<other/>".getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    void aDoctypeIsRefused() {
        String bomb = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY y \"z\">]><imsx_POXEnvelopeRequest/>";
        assertThat(LtiOutcome.parse(bomb.getBytes(StandardCharsets.UTF_8))).isEmpty();
    }
}
