package com.rayen.springds1.repository;

import com.rayen.springds1.model.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    boolean existsByOrderNumberIgnoreCase(String orderNumber);

    boolean existsByCustomerId(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"customer", "lines", "lines.product", "payment"})
    List<CustomerOrder> findAll();

    @Override
    @EntityGraph(attributePaths = {"customer", "lines", "lines.product", "payment"})
    Optional<CustomerOrder> findById(Long id);

    @EntityGraph(attributePaths = {"customer", "lines", "lines.product", "payment"})
    List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);
}
