package com.proj.orchestrator.api.dto;

import java.util.List;

public record DashboardResponse(
        UserProfileResponse profile,
        List<PaymentResponse> recentPayments
) {
}
