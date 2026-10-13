CREATE INDEX IF NOT EXISTS idx_endpoints_tenant_id ON endpoints (tenant_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_event_id ON deliveries (event_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_endpoint_id ON deliveries (endpoint_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_status_created ON deliveries (status, created_at);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events (created_at);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_delivery_id ON delivery_attempts (delivery_id);