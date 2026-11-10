package com.rayen.springds1.repository;

import com.rayen.springds1.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
