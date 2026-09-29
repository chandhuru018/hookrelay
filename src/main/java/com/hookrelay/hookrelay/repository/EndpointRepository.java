package com.hookrelay.hookrelay.repository;

import com.hookrelay.hookrelay.domain.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    List<Endpoint> findByTenantId(Long tenantId);
}