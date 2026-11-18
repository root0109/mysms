package com.mysms.finance.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.domain.RefundEntity;
import com.mysms.finance.mapper.RefundMapper;
import com.mysms.finance.repository.RefundRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of finance.refund: reads only — a refund is opened by finance.RefundPayment and settled by the
 * gateway's answer or notification ({@link PaymentService}). A refund belongs to the institution of its payment and is
 * owner-only below the fee office: a caller reads the refunds of the institutions where they work on fees
 * ({@link FinanceStaff}), and otherwise only refunds of payments they made.
 */
@Service
public class RefundService extends AppTransactionManager {

    public static final String AGGREGATE = RefundEntity.TABLE;
    static final String PERMISSION_READ = "finance.refund.read";

    private final RefundRepository repository;
    private final RefundMapper mapper;
    private final Clock clock;

    public RefundService(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            RefundRepository repository, RefundMapper mapper, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** One refund: in the fee office's reach, or of a payment the caller made (404 otherwise). */
    public Refund getRefund(UUID id) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_READ);
        return doInReadTransaction(() -> {
            RefundEntity refund = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE,
                    id));
            RefundRepository.Owner owner = repository.ownerOf(refund.getPaymentId());
            boolean office = FinanceStaff.reaches(FinanceStaff.reach(caller), scope, owner.institutionId());
            if (!scope.canRead(owner.institutionId()) || !office && !caller.userId().equals(owner.payerUserId())) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            return mapper.toDto(refund);
        });
    }

    /** The refunds the caller may read: the fee office's reach, plus refunds of the payments they made. */
    public Page<Refund> listRefunds(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(sortBy, RefundRepository.SORT_FIELDS, RefundRepository.DEFAULT_ORDER,
                RefundRepository.ID_COLUMN);
        DataScope scope = DataScope.current(PERMISSION_READ);
        DataScope reach = FinanceStaff.reach(caller);
        SqlFragment where = FilterParser.parse(filter, RefundRepository.FILTER_FIELDS)
                .and(RefundRepository.scope(scope));
        if (!FinanceStaff.covers(reach, scope)) {
            where = where.and(RefundRepository.reachableOrPaid(reach, caller.userId()));
        }
        SqlFragment filtered = where;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, filtered).map(mapper::toDto));
    }
}
