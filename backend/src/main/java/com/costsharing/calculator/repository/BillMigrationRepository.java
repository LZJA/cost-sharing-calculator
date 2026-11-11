package com.costsharing.calculator.repository;

import com.costsharing.calculator.entity.BillMigration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillMigrationRepository extends JpaRepository<BillMigration, String> {
}
