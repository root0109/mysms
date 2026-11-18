package com.mysms.library;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.library.controller.AuthorController;
import com.mysms.library.controller.LibraryCardController;
import com.mysms.library.controller.LibraryCardTypeController;
import com.mysms.library.controller.LoanController;
import com.mysms.library.controller.MediaController;
import com.mysms.library.controller.MediaCategoryController;
import com.mysms.library.controller.MediaTypeController;
import com.mysms.library.controller.PublisherController;
import com.mysms.library.controller.PurchaseRequestController;
import com.mysms.library.controller.ReservationController;
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
        assertThat(ControllerStubs.count(LoanController.class)).as("Loan").isEqualTo(4);
        assertThat(ControllerStubs.count(MediaController.class)).as("Media").isEqualTo(0);
        assertThat(ControllerStubs.count(MediaCategoryController.class)).as("MediaCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(MediaTypeController.class)).as("MediaType").isEqualTo(0);
        assertThat(ControllerStubs.count(PublisherController.class)).as("Publisher").isEqualTo(0);
        assertThat(ControllerStubs.count(PurchaseRequestController.class)).as("PurchaseRequest").isEqualTo(0);
        assertThat(ControllerStubs.count(ReservationController.class)).as("Reservation").isEqualTo(1);
    }
}
