package com.hookrelay.hookrelay.repository;

import com.hookrelay.hookrelay.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}