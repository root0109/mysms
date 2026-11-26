package com.mysms.learning.lms.business;

import com.mysms.assessment.v1.GradingBand;
import com.mysms.assessment.v1.ListGradingBandsRequest;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.learning.lms.integration.AssessmentGrpcClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * A cohort's grading scheme as a scale from percentages to grades (an Adapter over assessment's grading bands, asked
 * with the caller's identity). A percentage takes the band whose range holds it (min ≤ percent ≤ max); a percentage
 * no band holds keeps no letter. assessment refusing the caller is this operation's 403 naming the permission;
 * assessment not answering is 503. Never call this inside a database transaction.
 */
@Component
public class GradingScale {

    /** The assessment permission the lookup runs under (the caller's own). */
    public static final String ASSESSMENT_PERMISSION = "assessment.grading_band.read";
    private static final int MAX_BANDS = 100;
    private static final Logger LOG = LoggerFactory.getLogger(GradingScale.class);

    private final AssessmentGrpcClient assessment;

    public GradingScale(AssessmentGrpcClient assessment) {
        this.assessment = assessment;
    }

    /** One band of a scheme. */
    public record Band(BigDecimal minPercent, BigDecimal maxPercent, String label, BigDecimal gradePoint) {

        boolean holds(BigDecimal percent) {
            return minPercent.compareTo(percent) <= 0 && percent.compareTo(maxPercent) <= 0;
        }
    }

    /** The bands of the scheme. */
    public List<Band> bands(UUID schemeId, String purpose) {
        ListGradingBandsRequest request = ListGradingBandsRequest.newBuilder()
                .setFilter("gradingSchemeId = \"" + schemeId + "\"")
                .setPageable(PageableRequest.newBuilder().setPage(1).setSize(MAX_BANDS)).build();
        try {
            return assessment.listGradingBands(request).getItemsList().stream().map(GradingScale::band).toList();
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException(purpose + " also needs " + ASSESSMENT_PERMISSION + ".");
            }
            LOG.warn("the assessment service answered {} ({})", e.httpStatus(), e.statusCode());
            throw new UnavailableException("The assessment service could not answer.");
        }
    }

    /** The band holding the percentage, if any. */
    public static Optional<Band> bandOf(List<Band> bands, BigDecimal percent) {
        return bands.stream().filter(band -> band.holds(percent)).findFirst();
    }

    private static Band band(GradingBand band) {
        return new Band(new BigDecimal(band.getMinPercent()), new BigDecimal(band.getMaxPercent()), band.getGradeLabel(),
                band.hasGradePoint() ? new BigDecimal(band.getGradePoint()) : null);
    }
}
