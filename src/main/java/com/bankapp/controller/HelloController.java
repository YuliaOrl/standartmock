package com.bankapp.controller;

import com.bankapp.service.HelloMetricsService;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final HelloMetricsService helloMetrics;

    public HelloController(HelloMetricsService helloMetrics) {
        this.helloMetrics = helloMetrics;
    }

    @Operation(
            summary = "Приветствие пользователя",
            description = "Возвращает приветственное сообщение с именем пользователя",
            parameters = {
                    @Parameter(
                            name = "name",
                            description = "Имя пользователя",
                            required = false,
                            example = "Алекс",
                            in = ParameterIn.QUERY)})
    @GetMapping("/hello")
    public String sayHello(@RequestParam(defaultValue = "Гость") String name) {
        // Счётчик вызовов
        helloMetrics.incrementRequests(name);

        // Замер времени
        Timer.Sample sample = helloMetrics.startTimer();

        try {
            // Добавление уникального имени
            helloMetrics.addUniqueName(name);

            return "Привет, " + name + "!";
        } finally {
            helloMetrics.recordLatency(sample, name);
        }
    }
}