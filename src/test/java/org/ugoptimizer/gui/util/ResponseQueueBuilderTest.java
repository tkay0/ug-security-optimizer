package org.ugoptimizer.gui.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.ugoptimizer.model.ServiceRequest;
import java.time.Instant;

class ResponseQueueBuilderTest {

    @Test
    void returnsEmptyArrayForNullInput() {
        assertEquals(0, ResponseQueueBuilder.orderedOpenRequests(null).length);
    }

    @Test
    void returnsEmptyArrayForEmptyInput() {
        assertEquals(0, ResponseQueueBuilder.orderedOpenRequests(new ServiceRequest[0]).length);
    }

    @Test
    void filtersOnlyOpenRequests() {
        ServiceRequest[] requests = {
                new ServiceRequest(1, 1, 2, "THEFT_REPORT", 5, Instant.now(), Instant.now().plusSeconds(3600),
                        "PENDING", "SECURITY", "Theft reported"),
                new ServiceRequest(2, 1, 2, "THEFT_REPORT", 3, Instant.now(), Instant.now().plusSeconds(3600),
                        "COMPLETED", "SECURITY", "Resolved")
        };
        assertEquals(1, ResponseQueueBuilder.orderedOpenRequests(requests).length);
    }
}
