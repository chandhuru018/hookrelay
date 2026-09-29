package com.hookrelay.hookrelay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "endpoints")
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 100)
    private String secret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EndpointStatus status = EndpointStatus.ACTIVE;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Endpoint() {
    }

    public Endpoint(Tenant tenant, String url, String secret) {
        this.tenant = tenant;
        this.url = url;
        this.secret = secret;
    }

    public Long getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getUrl() { return url; }
    public String getSecret() { return secret; }
    public EndpointStatus getStatus() { return status; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(EndpointStatus status) { this.status = status; }
    public void setConsecutiveFailures(int n) { this.consecutiveFailures = n; }
}