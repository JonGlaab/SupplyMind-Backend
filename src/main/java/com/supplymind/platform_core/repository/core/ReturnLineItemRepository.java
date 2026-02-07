package com.supplymind.platform_core.repository.core;

import com.supplymind.platform_core.model.core.ReturnLineItem;
import com.supplymind.platform_core.common.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface ReturnLineItemRepository extends JpaRepository<ReturnLineItem, Long> {

    @Query("""
        select coalesce(sum(li.qtyApproved), 0)
        from ReturnLineItem li
        join li.returnRequest rr
        where li.poItem.poItemId = :poItemId
          and rr.status in :countStatuses
    """)
    Integer sumApprovedQtyByPoItemAndStatuses(
            @Param("poItemId") Long poItemId,
            @Param("countStatuses") Set<ReturnStatus> countStatuses
    );
}
