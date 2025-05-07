package com.bankapp.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AccountMetricsService {

    // Счётчики
    private final Counter accountCreateCalls;

    // Таймеры
    private final Timer accountCreateTimer;

    // Gauge для текущего количества активных операций создания счетов
    private final ConcurrentMap<String, Integer> activeAccountCreations = new ConcurrentHashMap<>();

    public AccountMetricsService(MeterRegistry registry) {
        // Инициализация счётчиков
        this.accountCreateCalls = Counter.builder("bankapp.accounts.create.calls")
                .description("Количество вызовов метода создания счета")
                .register(registry);

        // Инициализация таймеров
        this.accountCreateTimer = Timer.builder("bankapp.accounts.create.duration")
                .description("Время выполнения запроса создания счета")
                .register(registry);

        // Инициализация Gauge
        Gauge.builder("bankapp.accounts.active_creations", activeAccountCreations,
                        map -> map.getOrDefault("in_progress", 0))
                .description("Текущее количество активных созданий счетов")
                .register(registry);

        // Начальное значение
        activeAccountCreations.put("in_progress", 0);
    }

    // Геттеры
    public Counter getAccountCreateCalls() {
        return accountCreateCalls;
    }

    public Timer getAccountCreateTimer() {
        return accountCreateTimer;
    }

    // Управление Gauge
    public void incrementActiveCreations() {
        activeAccountCreations.compute("in_progress", (k, v) -> v == null ? 1 : v + 1);
    }

    public void decrementActiveCreations() {
        activeAccountCreations.compute("in_progress", (k, v) -> v == null || v <= 0 ? 0 : v - 1);
    }
}