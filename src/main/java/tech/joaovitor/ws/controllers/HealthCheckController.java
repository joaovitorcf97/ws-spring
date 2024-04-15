package tech.joaovitor.ws.controllers;

import java.util.logging.Logger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    private final static Logger LOGGER = Logger.getLogger(HealthCheckController.class.getName());

    @GetMapping
    void healthCheck() {
        LOGGER.info("health check");
    }
}
