package com.bankapp.controller;

import com.bankapp.model.Account;
import com.bankapp.service.AccountMetricsService;
import com.bankapp.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMetricsService accountMetrics;

    public AccountController(AccountService accountService, AccountMetricsService accountMetrics) {
        this.accountService = accountService;
        this.accountMetrics = accountMetrics;
    }

    @Operation(
            summary = "Создание счета",
            description = "Создание нового счета для клиента с указанием его ID",
            parameters = {
                    @Parameter(
                            name = "clientId",
                            description = "Идентификатор клиента для создания счета",
                            required = true,
                            example = "152554f1-cbf8-4ef5-b409-2d886cc2b0cd",
                            in = ParameterIn.QUERY)})
    @ApiResponse(
            responseCode = "200",
            description = "Счет успешно создан",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Account.class)))
    @PostMapping("/create")
    public Account create(@RequestParam String clientId) {
        accountMetrics.getAccountCreateCalls().increment();
        accountMetrics.incrementActiveCreations();

        try {
            return accountMetrics.getAccountCreateTimer().record(() ->
                    accountService.createAccount(clientId)
            );
        } finally {
            accountMetrics.decrementActiveCreations();
        }
    }
}
