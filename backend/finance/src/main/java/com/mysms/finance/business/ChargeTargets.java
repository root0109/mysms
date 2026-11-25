package com.mysms.finance.business;

import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.finance.repository.StaffRecordRepository;
import com.mysms.finance.repository.StudentRecordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Whom a charge another module raises may bill: a student of the charge's institution, or the login account of one of
 * its students or staff — as finance knows them from sis's and hr's events. Anyone else is 422, so a charge can never
 * land in another school's student's or a stranger's invoices. Runs inside the caller's transaction.
 */
@Component
public class ChargeTargets {

    private final StudentRecordRepository students;
    private final StaffRecordRepository staff;

    public ChargeTargets(StudentRecordRepository students, StaffRecordRepository staff) {
        this.students = students;
        this.staff = staff;
    }

    /** 422 naming every billed party that is not the institution's. */
    void require(UUID institution, UUID studentId, UUID billToUserId) {
        List<FieldError> errors = new ArrayList<>();
        if (studentId != null && !students.studentIn(studentId, institution)) {
            errors.add(new FieldError("studentId", "is not a student of the institution", "NOT_FOUND"));
        }
        if (billToUserId != null && !students.accountIn(billToUserId, institution)
                && !staff.accountIn(billToUserId, institution)) {
            errors.add(new FieldError("billToUserId", "is not an account of the institution's students or staff",
                    "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }
}
