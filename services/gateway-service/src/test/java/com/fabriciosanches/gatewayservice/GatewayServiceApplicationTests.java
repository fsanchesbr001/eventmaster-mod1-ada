package com.fabriciosanches.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayServiceApplicationTests {

    @Autowired
    private WebTestClient webTestClient;


    @Test
    void contextLoads() {
    }

    @Test
    void swaggerUiIndexHtmlAliasShouldRedirectToConfiguredSwaggerPath() {
        webTestClient.get()
                .uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/swagger-ui.html");
    }

}
