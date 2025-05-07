package com.bankapp.util;

import com.bankapp.model.Account;
import com.bankapp.model.Client;
import com.bankapp.repository.ClientRepository;
import com.bankapp.repository.AccountRepository;
import com.github.javafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@Component
public class TestDataInitializer implements CommandLineRunner {
    AccountRepository accountRepository;
    private final Faker faker = new Faker();
    private final Random random = new Random();
    private final RestTemplate restTemplate = new RestTemplate();

    public TestDataInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("📌 Генерация тестовых данных...");

        for (int i = 0; i < 10; i++) {
            // Генерируем имя, телефон, логин и пароль
            String fullName = faker.name().fullName();
            String phone = "+79" + (random.nextInt(900000000) + 100000000);
            String username = "user" + (i + 1);
            String password = "pass" + (i + 1);

            //Обращение в AuthController для регистрации пользователей в отдельном сервисе
            String regUrl = "http://localhost:8081/auth/register?fullName=" + fullName + "&phone=" + phone
                    + "&username=" + username + "&password=" + password;
            restTemplate.postForObject(regUrl, null, String.class);

            // Создаем клиента и сохраняем в локальном репозитории банка
            Client client = new Client(fullName, phone, username, password);
            ClientRepository.save(client);
            System.out.println("✅ Создан клиент: " + fullName + " (" + phone + ") | Логин: " + username
                    + ", Пароль: " + password);

            // Создаем случайное количество счетов (от 1 до 3)
            int accountCount = random.nextInt(3) + 1;
            for (int j = 0; j < accountCount; j++) {
                Account account = new Account();
                double initialBalance = random.nextInt(9000) + 1000; // Баланс от 1000 до 10000₽
                account.setBalance(initialBalance);
                client.getAccounts().add(account);
                accountRepository.save(account);
                System.out.println("  ➕ Счет: " + account.getAccountNumber() + " | Карта: "
                        + account.getCardNumber() + " | Баланс: " + initialBalance + "₽");
            }
        }

        System.out.println("🎉 Генерация тестовых данных завершена!");
    }
}