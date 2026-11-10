package com.rayen.springds1.repository;

import com.rayen.springds1.dto.OverdueCustomerResponse;
import com.rayen.springds1.model.Customer;
import com.rayen.springds1.model.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    /** Custom query: customers with orders in a given status placed before the cutoff date. */
    @Query("""
            select new com.rayen.springds1.dto.OverdueCustomerResponse(
                   c.id, c.companyName, c.email,
                   count(customerOrder),
                   sum(customerOrder.totalAmount),
                   min(customerOrder.orderDate))
            from CustomerOrder customerOrder
            join customerOrder.customer c
            where customerOrder.status = :status
              and customerOrder.orderDate < :cutoff
            group by c.id, c.companyName, c.email
            order by sum(customerOrder.totalAmount) desc, c.id asc
            """)
    List<OverdueCustomerResponse> findOverdueCustomers(
            @Param("status") OrderStatus status,
            @Param("cutoff") LocalDateTime cutoff
    );
}
