package com.hookrelay.hookrelay.repository;

import com.hookrelay.hookrelay.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @Query("SELECT d FROM Delivery d WHERE d.event.tenant.id = :tenantId")
    List<Delivery> findAllForTenant(@Param("tenantId") Long tenantId);
}