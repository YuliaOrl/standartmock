package com.bankapp.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TransactionMetricsService {

    // Счётчики
    private final Counter selectRecipientCalls;
    private final Counter transferCalls;
    private final Counter getAllClientsCalls;

    // Таймеры
    private final Timer selectRecipientTimer;
    private final Timer transferTimer;
    private final Timer getAllClientsTimer;

    // Gauge для текущего количества переводов
    private final ConcurrentMap<String, Integer> currentTransfers = new ConcurrentHashMap<>();

    public TransactionMetricsService(MeterRegistry registry) {
        // Инициализация счётчиков
        this.selectRecipientCalls = Counter.builder("bankapp.transaction.select_recipient.calls")
                .description("Количество вызовов метода выбора получателя перевода").register(registry);

        this.transferCalls = Counter.builder("bankapp.transaction.transfer.calls")
                .description("Количество вызовов метода перевода средств").register(registry);

        this.getAllClientsCalls = Counter.builder("bankapp.transaction.clients.all.calls")
                .description("Количество вызовов получения списка клиентов в TransactionController").register(registry);

        // Инициализация таймеров
        this.selectRecipientTimer = Timer.builder("bankapp.transaction.select_recipient.duration")
                .description("Время выполнения запроса выбора получателя").register(registry);

        this.transferTimer = Timer.builder("bankapp.transaction.transfer.duration")
                .description("Время выполнения запроса перевода").register(registry);

        this.getAllClientsTimer = Timer.builder("bankapp.transaction.clients.all.duration")
                .description("Время выполнения запроса получения всех клиентов").register(registry);

        // Инициализация Gauge
        Gauge.builder("bankapp.transaction.current_transfers", currentTransfers, map -> map.getOrDefault("in_progress", 0))
                .description("Текущее количество активных переводов")
                .register(registry);

        // Начальное значение
        currentTransfers.put("in_progress", 0);
    }

    // Геттеры для счётчиков
    public Counter getSelectRecipientCalls() { return selectRecipientCalls; }
    public Counter getTransferCalls() { return transferCalls; }
    public Counter getGetAllClientsCalls() { return getAllClientsCalls; }

    // Геттеры для таймеров
    public Timer getSelectRecipientTimer() { return selectRecipientTimer; }
    public Timer getTransferTimer() { return transferTimer; }
    public Timer getGetAllClientsTimer() { return getAllClientsTimer; }

    // Управление Gauge
    public void incrementTransfers() {
        currentTransfers.compute("in_progress", (k, v) -> v == null ? 1 : v + 1);
    }

    public void decrementTransfers() {
        currentTransfers.compute("in_progress", (k, v) -> v == null || v <= 0 ? 0 : v - 1);
    }
}