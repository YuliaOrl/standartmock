package com.bankapp.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class HelloMetricsService {

    private final MeterRegistry registry;
    private final Set<String> uniqueNames = new ConcurrentSkipListSet<>();

    public HelloMetricsService(MeterRegistry registry) {
        this.registry = registry;
        registerMetrics();
    }

    // Регистрация Gauge для подсчёта уникальных имён
    private void registerMetrics() {
        registry.gauge("hello.unique.users", Tags.empty(), uniqueNames, Set::size);
    }

    // Увеличиваем счётчик запросов
    public void incrementRequests(String name) {
        registry.counter("hello.requests.count", Tags.of("name", name)).increment();
    }

    // Замеряем время выполнения
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordLatency(Timer.Sample sample, String name) {
        sample.stop(registry.timer("hello.requests.latency", Tags.of("name", name)));
    }

    // Добавляем имя в список уникальных
    public void addUniqueName(String name) {
        uniqueNames.add(name);
    }
}