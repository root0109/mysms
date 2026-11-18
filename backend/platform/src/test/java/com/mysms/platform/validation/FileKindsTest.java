package com.mysms.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FileKindsTest {

    @Test
    void theExtensionDecidesTheKindAndTheDefaultCatalogue() {
        assertThat(FileKinds.extension("Report.Card.PDF")).isEqualTo("pdf");
        assertThat(FileKinds.extension("README")).isEmpty();
        assertThat(FileKinds.extension("trailing.")).isEmpty();
        assertThat(FileKinds.kindOf("docx")).isEqualTo("document");
        assertThat(FileKinds.kindOf("h5p")).isEqualTo("package");
        assertThat(FileKinds.kindOf("dwg")).isEqualTo("other");
        assertThat(FileKinds.acceptedByDefault("png")).isTrue();
        assertThat(FileKinds.acceptedByDefault("exe")).isFalse();
        assertThat(FileKinds.acceptedByDefault("svg")).as("svg can carry script").isFalse();
    }

    @Test
    void mediaMustDeclareTheirOwnFamilyAndDocumentsMayNotClaimOne() {
        assertThat(FileKinds.contentTypeFits("image", "image/png")).isTrue();
        assertThat(FileKinds.contentTypeFits("image", "application/pdf")).isFalse();
        assertThat(FileKinds.contentTypeFits("pdf", "application/pdf")).isTrue();
        assertThat(FileKinds.contentTypeFits("document", "video/mp4")).isFalse();
        assertThat(FileKinds.contentTypeFits("other", "video/mp4")).isTrue();
    }

    @Test
    void aPolicyListsItsOwnExtensions() {
        assertThat(FileKinds.allowed(" pdf, .DOCX ,,")).contains(Set.of("pdf", "docx"));
        assertThat(FileKinds.allowed(" ")).isEmpty();
        assertThat(FileKinds.allowed(null)).isEmpty();
    }
}
