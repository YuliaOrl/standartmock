package com.bankapp.controller;

import com.bankapp.model.Account;
import com.bankapp.model.Client;
import com.bankapp.repository.ClientRepository;
import com.bankapp.service.TransactionMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final TransactionMetricsService transactionMetrics;

    private Client recipientClient;
    private Account recipientAccount;

    public TransactionController(TransactionMetricsService transactionMetrics) {
        this.transactionMetrics = transactionMetrics;
    }

    // 1️⃣ Получить список всех клиентов перед переводом
    @Operation(
            summary = "Получение списка всех клиентов",
            description = "Получает список всех зарегистрированных клиентов для выбора получателя перевода",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список клиентов успешно получен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Client.class)))})
    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        return transactionMetrics.getGetAllClientsTimer().record(() -> {
            transactionMetrics.getGetAllClientsCalls().increment();
            List<Client> clients = List.copyOf(ClientRepository.getAllClients());
            return ResponseEntity.ok(clients);
        });
    }

    // 2️⃣ Выбрать получателя перевода по логину и номеру счета
    @Operation(
            summary = "Выбор получателя перевода",
            description = "Выбирает получателя перевода по логину клиента и номеру счёта с проверкой авторизации",
            parameters = {
                    @Parameter(
                            name = "username",
                            description = "Логин получателя",
                            required = true,
                            example = "user1",
                            in = ParameterIn.QUERY),
                    @Parameter(
                            name = "accountNumber",
                            description = "Номер счёта получателя",
                            required = true,
                            example = "1107debec4cb",
                            in = ParameterIn.QUERY)},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Получатель успешно выбран",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("✅ Получатель выбран: Danille Prosacco (Счет: de42e1e55641)")})),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Ошибка валидации данных",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Получатель не найден!"),
                                                @ExampleObject("❌ Ошибка: У получателя нет такого счета!")})),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Пользователь не авторизован",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Сначала войдите в систему!")})),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Ошибка сервера при проверке авторизации",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Не удалось получить статус авторизации пользователя.")}))})
    @PostMapping("/select-recipient")
    public ResponseEntity<String> selectRecipient(@RequestParam String username, @RequestParam String accountNumber) {
        transactionMetrics.getSelectRecipientCalls().increment();

        return transactionMetrics.getSelectRecipientTimer().record(() -> {
            Boolean response = restTemplate.getForEntity("http://localhost:8081/auth/isLogged", Boolean.class).getBody();
            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("❌ Ошибка: Не удалось получить статус авторизации пользователя.");
            }
            if (!response) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("❌ Ошибка: Сначала войдите в систему!");
            }

            Optional<Client> recipientOpt = ClientRepository.findByUsername(username);
            if (recipientOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("❌ Ошибка: Получатель не найден!");
            }

            Optional<Account> recipientAccountOpt = recipientOpt.get().getAccounts()
                    .stream()
                    .filter(a -> a.getAccountNumber().equals(accountNumber))
                    .findFirst();

            if (recipientAccountOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("❌ Ошибка: У получателя нет такого счета!");
            }

            this.recipientClient = recipientOpt.get();
            this.recipientAccount = recipientAccountOpt.get();

            return ResponseEntity.ok("✅ Получатель выбран: " + recipientClient.getFullName() +
                    " (Счет: " + recipientAccount.getAccountNumber() + ")");
        });
    }

    // 3️⃣ Выполнить перевод (указать сумму и изменить баланс)
    @Operation(
            summary = "Перевод средств",
            description = "Выполняет перевод указанной суммы на выбранный счет. Проверяет авторизацию, " +
                    "наличие получателя и достаточность средств.",
            parameters = {
                    @Parameter(
                            name = "amount",
                            description = "Сумма перевода в рублях",
                            required = true,
                            example = "1000.5",
                            in = ParameterIn.QUERY)},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Перевод успешно выполнен",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("✅ Перевод завершен! 1000.5₽ переведено на счет b1273bef7742")})),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Ошибка валидации данных",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: У вас нет счета!"),
                                                @ExampleObject("❌ Ошибка: Недостаточно средств на счете!")})),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Пользователь не авторизован",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Сначала войдите в систему!")})),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Получатель не выбран",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Сначала выберите получателя!")})),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Ошибка сервера при проверке авторизации",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Не удалось получить статус авторизации пользователя.")}))})
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestParam double amount) {
        transactionMetrics.getTransferCalls().increment();
        transactionMetrics.incrementTransfers();

        return transactionMetrics.getTransferTimer().record(() -> {
            try {
                Boolean response = restTemplate.getForEntity("http://localhost:8081/auth/isLogged", Boolean.class).getBody();
                if (response == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("❌ Ошибка: Не удалось получить статус авторизации пользователя.");
                }
                if (!response) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("❌ Ошибка: Сначала войдите в систему!");
                }

                if (recipientClient == null || recipientAccount == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("❌ Ошибка: Сначала выберите получателя!");
                }

                String username = restTemplate.getForEntity("http://localhost:8081/auth/loggedUser", String.class).getBody();
                Client sender = ClientRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Клиент не найден"));

                Optional<Account> senderAccountOpt = sender.getAccounts().stream().findFirst();
                if (senderAccountOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("❌ Ошибка: У вас нет счета!");
                }

                Account senderAccount = senderAccountOpt.get();
                if (senderAccount.getBalance() < amount) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("❌ Ошибка: Недостаточно средств на счете!");
                }

                // Обновляем балансы
                senderAccount.setBalance(senderAccount.getBalance() - amount);
                recipientAccount.setBalance(recipientAccount.getBalance() + amount);

                transactionMetrics.decrementTransfers();

                return ResponseEntity.ok("✅ Перевод завершен! " + amount + "₽ переведено на счет " +
                        recipientAccount.getAccountNumber());
            } finally {
                transactionMetrics.decrementTransfers();
            }
        });
    }
}