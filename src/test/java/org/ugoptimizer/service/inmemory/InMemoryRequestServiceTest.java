package org.ugoptimizer.service.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.ServiceRequest;

class InMemoryRequestServiceTest {

    @Test
    void seedsFourRequests() {
        InMemoryRequestService service = new InMemoryRequestService();
        assertEquals(4, service.findAll().size());
    }

    @Test
    void addAppearsInFindAllAndAdvancesNextId() {
        InMemoryRequestService service = new InMemoryRequestService();
        int nextId = service.nextRequestId();
        assertEquals(5, nextId);

        ServiceRequest added = new ServiceRequest(nextId, 1, 2, "WELFARE_CHECK", 3,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                "PENDING", "PATROL_TEAM", "test");
        service.add(added);

        assertEquals(5, service.findAll().size());
        assertEquals(6, service.nextRequestId());
        assertTrue(service.findAll().stream().anyMatch(r -> r.getRequestId() == nextId));
    }

    @Test
    void updateStatusPersistsAndReturnsUpdatedRequest() {
        InMemoryRequestService service = new InMemoryRequestService();
        int requestId = service.findAll().get(0).getRequestId();

        ServiceRequest updated = service.updateStatus(requestId, "ASSIGNED");

        assertEquals("ASSIGNED", updated.getStatus());
        assertEquals("ASSIGNED", service.findAll().stream()
                .filter(r -> r.getRequestId() == requestId)
                .findFirst()
                .orElseThrow()
                .getStatus());
    }

    @Test
    void updateStatusThrowsForUnknownId() {
        InMemoryRequestService service = new InMemoryRequestService();
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(999_999, "ASSIGNED"));
    }

    @Test
    void sharedInstanceReflectsChangesAcrossReferences() {
        InMemoryRequestService service = new InMemoryRequestService();
        int before = service.findAll().size();

        // Simulates two screens (e.g. RequestResourceMenu and SearchSortMenu) holding
        // the same injected RequestService instance.
        org.ugoptimizer.frontend.RequestService secondScreenReference = service;
        service.add(new ServiceRequest(service.nextRequestId(), 1, 2, "THEFT_REPORT", 1,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                "PENDING", "PATROL_TEAM", "test"));

        assertEquals(before + 1, secondScreenReference.findAll().size());
    }
}
