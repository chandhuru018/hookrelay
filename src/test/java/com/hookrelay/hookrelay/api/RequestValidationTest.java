package com.hookrelay.hookrelay.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void publishRequest_acceptsValidInput() {
        PublishEventRequest request = new PublishEventRequest();
        request.setType("order.paid");
        request.setPayload("{\"orderId\": 1}");
        request.setIdempotencyKey("key-1");

        assertTrue(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void publishRequest_rejectsOversizedFields() {
        PublishEventRequest request = new PublishEventRequest();
        request.setType("x".repeat(101));
        request.setPayload("x".repeat(65537));
        request.setIdempotencyKey("x".repeat(101));

        assertEquals(3, VALIDATOR.validate(request).size());
    }

    @Test
    void endpointRequest_rejectsUrlLongerThan500() {
        RegisterEndpointRequest request = new RegisterEndpointRequest();
        request.setUrl("https://example.com/" + "a".repeat(481));

        assertEquals(1, VALIDATOR.validate(request).size());
    }

    @Test
    void tenantRequest_rejectsNameLongerThan100() {
        TenantRegistrationRequest request = new TenantRegistrationRequest();
        request.setName("n".repeat(101));

        assertEquals(1, VALIDATOR.validate(request).size());
    }
}