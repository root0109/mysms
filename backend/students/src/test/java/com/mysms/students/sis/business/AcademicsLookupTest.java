package com.mysms.students.sis.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mysms.academics.v1.GetGradeLevelResponse;
import com.mysms.academics.v1.GetSectionResponse;
import com.mysms.academics.v1.GradeLevel;
import com.mysms.academics.v1.Section;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.org.v1.AcademicYear;
import com.mysms.org.v1.GetAcademicYearResponse;
import com.mysms.students.sis.integration.AcademicsGrpcClient;
import com.mysms.students.sis.integration.OrgGrpcClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * The check of a promotion's target against org and academics (a unit test over stand-ins of their gRPC clients; the
 * real services answer in the app module's component test): a year, grade or section of another institution, or one
 * the caller cannot see, is a field error; a refused read is the caller's missing permission.
 */
class AcademicsLookupTest {

    private static final UUID INSTITUTION = UUID.randomUUID();
    private final AcademicsGrpcClient academics = mock(AcademicsGrpcClient.class);
    private final OrgGrpcClient org = mock(OrgGrpcClient.class);
    private final AcademicsLookup lookup = new AcademicsLookup(academics, org);
    private final UUID year = UUID.randomUUID();
    private final UUID grade = UUID.randomUUID();
    private final UUID section = UUID.randomUUID();

    private void yearOf(UUID institution) {
        when(org.getAcademicYear(any())).thenReturn(GetAcademicYearResponse.newBuilder()
                .setAcademicYear(AcademicYear.newBuilder().setInstitutionId(institution.toString())).build());
    }

    private void gradeOf(UUID institution) {
        when(academics.getGradeLevel(any())).thenReturn(GetGradeLevelResponse.newBuilder()
                .setGradeLevel(GradeLevel.newBuilder().setInstitutionId(institution.toString())).build());
    }

    @Test
    void theInstitutionsOwnTargetPasses() {
        yearOf(INSTITUTION);
        gradeOf(INSTITUTION);
        when(academics.getSection(any())).thenReturn(GetSectionResponse.newBuilder()
                .setSection(Section.newBuilder().setInstitutionId(INSTITUTION.toString())).build());

        assertThat(lookup.targetErrors(INSTITUTION, year, grade, section, "Proposing promotions")).isEmpty();
        assertThat(lookup.targetErrors(INSTITUTION, year, grade, null, "Proposing promotions"))
                .as("no section named").isEmpty();
    }

    @Test
    void aTargetOfAnotherInstitutionOrUnknownIsAFieldError() {
        yearOf(UUID.randomUUID());
        when(academics.getGradeLevel(any())).thenThrow(GrpcRefusals.refused("academics", HttpStatus.NOT_FOUND,
                "NOT_FOUND"));
        when(academics.getSection(any())).thenThrow(GrpcRefusals.refused("academics", HttpStatus.NOT_FOUND,
                "NOT_FOUND"));

        assertThat(lookup.targetErrors(INSTITUTION, year, grade, section, "Proposing promotions"))
                .extracting(FieldError::field)
                .containsExactly("toAcademicYearId", "toGradeLevelId", "toSectionId");
    }

    @Test
    void aRefusedReadIsTheCallersMissingPermissionAndAnUnreachableServiceIs503() {
        when(org.getAcademicYear(any())).thenThrow(GrpcRefusals.refused("org", HttpStatus.FORBIDDEN,
                "PERMISSION_DENIED"));
        assertThatThrownBy(() -> lookup.targetErrors(INSTITUTION, year, grade, null, "Proposing promotions"))
                .isInstanceOf(PermissionDeniedException.class);

        doThrow(GrpcRefusals.unreachable("org")).when(org).getAcademicYear(any());
        assertThatThrownBy(() -> lookup.targetErrors(INSTITUTION, year, grade, null, "x"))
                .isInstanceOf(UnavailableException.class);
    }
}
