package com.proj.orchestrator.application.usecase;

import com.proj.orchestrator.api.dto.*;
import com.proj.orchestrator.infrastructure.client.PaymentServiceClient;
import com.proj.orchestrator.infrastructure.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetDashboardUseCase {

    private final UserServiceClient userServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    public ApiResponse<DashboardResponse> execute(String userId) {
        ApiResponse<UserProfileResponse> profileResponse = userServiceClient.getProfile(userId);
        ApiResponse<List<PaymentResponse>> paymentsResponse = paymentServiceClient.getPaymentHistory(userId);

        DashboardResponse dashboard = new DashboardResponse(
                profileResponse.data(),
                paymentsResponse.data()
        );

        return new ApiResponse<>(200, "Dashboard retrieved", dashboard);
    }
}
