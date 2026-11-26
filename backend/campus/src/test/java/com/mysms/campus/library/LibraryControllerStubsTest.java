package com.mysms.campus.library;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.campus.library.controller.AuthorController;
import com.mysms.campus.library.controller.LibraryCardController;
import com.mysms.campus.library.controller.LibraryCardTypeController;
import com.mysms.campus.library.controller.LoanController;
import com.mysms.campus.library.controller.MediaController;
import com.mysms.campus.library.controller.MediaCategoryController;
import com.mysms.campus.library.controller.MediaTypeController;
import com.mysms.campus.library.controller.PublisherController;
import com.mysms.campus.library.controller.PurchaseRequestController;
import com.mysms.campus.library.controller.ReservationController;
import org.junit.jupiter.api.Test;

/**
 * The operations library does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class LibraryControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(AuthorController.class)).as("Author").isEqualTo(0);
        assertThat(ControllerStubs.count(LibraryCardController.class)).as("LibraryCard").isEqualTo(0);
        assertThat(ControllerStubs.count(LibraryCardTypeController.class)).as("LibraryCardType").isEqualTo(0);
        assertThat(ControllerStubs.count(LoanController.class)).as("Loan").isEqualTo(0);
        assertThat(ControllerStubs.count(MediaController.class)).as("Media").isEqualTo(0);
        assertThat(ControllerStubs.count(MediaCategoryController.class)).as("MediaCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(MediaTypeController.class)).as("MediaType").isEqualTo(0);
        assertThat(ControllerStubs.count(PublisherController.class)).as("Publisher").isEqualTo(0);
        assertThat(ControllerStubs.count(PurchaseRequestController.class)).as("PurchaseRequest").isEqualTo(0);
        assertThat(ControllerStubs.count(ReservationController.class)).as("Reservation").isEqualTo(0);
    }
}
