package com.rayen.springds1.repository;

import com.rayen.springds1.dto.RevenueTotals;
import com.rayen.springds1.model.Payment;
import com.rayen.springds1.model.enums.PaymentStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderId(Long orderId);

    /** Custom query: number and total of payments with a given status in a period. */
    @Query("""
            select new com.rayen.springds1.dto.RevenueTotals(count(payment), sum(payment.amount))
            from Payment payment
            where payment.status = :status
              and payment.paidAt >= :from
              and payment.paidAt < :toExclusive
            """)
    RevenueTotals sumRevenue(
            @Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive
    );
}
