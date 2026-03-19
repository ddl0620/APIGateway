package com.proj.payment_microservice.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "wiremock.server.enabled", havingValue = "true", matchIfMissing = false)
public class WireMockConfig {

    @Value("${wiremock.server.port}")
    private int port;

    private WireMockServer wireMockServer;

    @PostConstruct
    public void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
        wireMockServer.start();
        WireMock.configureFor("localhost", port);

        setupStubs();
        log.info("WireMock server started on port {}", port);
    }

    private void setupStubs() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/charges"))
                .withRequestBody(matchingJsonPath("$.amount"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "transactionId": "txn_{{randomValue type='UUID'}}",
                                    "status": "CAPTURED",
                                    "message": "Payment processed successfully"
                                }
                                """)
                        .withTransformers("response-template")
                        .withFixedDelay(1500)
                )
        );

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/charges"))
                .atPriority(1)
                .withRequestBody(matchingJsonPath("$.amount", matching("^[1-9]\\d{4,}.*")))
                .willReturn(aResponse()
                        .withStatus(402)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "transactionId": null,
                                    "status": "DECLINED",
                                    "message": "Payment declined: insufficient funds"
                                }
                                """)
                        .withFixedDelay(800)
                )
        );

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/refunds"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "refundId": "ref_{{randomValue type='UUID'}}",
                                    "status": "REFUNDED",
                                    "message": "Refund processed successfully"
                                }
                                """)
                        .withTransformers("response-template")
                        .withFixedDelay(2000)
                )
        );
    }

    @PreDestroy
    public void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("WireMock server stopped");
        }
    }
}
