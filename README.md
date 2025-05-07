## :green_book: Описание проекта

Spring Boot приложение банковского сервиса, которое обрабатывает запросы клиента на перевод денежных средств на счета других клиентов. Авторизация в приложении осуществляется при обращении к внешнему сервису <a target="_blank" href="https://github.com/YuliaOrl/authmock">*Authmock*</a>.

<a target="_blank" href="https://github.com/YuliaOrl/authmock">*Authmock*</a> представляет собой заглушку для на нагрузочного тестирования с возможностью устанавливать таймауты, имитирующие задержку ответа сервера.

### :computer: Использованные технологии

- Мониторинг метрик через Spring Actuator
- Хранение и обработка метрик с использованием Prometheus
- Визуализация метрик в <a target="_blank" href="https://github.com/YuliaOrl/standartmock/blob/master/Metrics_Prometheus_Grafana_StandartMock.jpg/">*Grafana*</a>
- Документация API через Swagger