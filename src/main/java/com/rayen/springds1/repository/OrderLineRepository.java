package com.rayen.springds1.repository;

import com.rayen.springds1.dto.BestSellerResponse;
import com.rayen.springds1.model.OrderLine;
import com.rayen.springds1.model.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    boolean existsByProductId(Long productId);

    /** Custom query: products that sold the most units in a period, ignoring one order status. */
    @Query("""
            select new com.rayen.springds1.dto.BestSellerResponse(
                   p.id, p.sku, p.name,
                   sum(line.quantity),
                   sum(line.quantity * line.unitPrice))
            from OrderLine line
            join line.product p
            join line.order customerOrder
            where customerOrder.status <> :excludedStatus
              and customerOrder.orderDate >= :from
              and customerOrder.orderDate < :toExclusive
            group by p.id, p.sku, p.name
            order by sum(line.quantity) desc, p.id asc
            """)
    List<BestSellerResponse> findBestSellingProducts(
            @Param("excludedStatus") OrderStatus excludedStatus,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable
    );
}
