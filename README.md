Hotel Booking System

Распределённая система бронирования отелей на Spring Boot с микросервисной архитектурой.

Модули и порты

eureka-server, порт 8761. Реестр сервисов, Service Discovery.

api-gateway, порт 8080. API Gateway, маршрутизация и JWT-проверка на входе.

hotel-service, порт 8081. Управление отелями и номерами.

booking-service, порт 8082. Бронирования, JWT Security, сага.

Технологии

Java 17, Spring Boot 3.5.6, Spring Cloud 2025.0.0.

Spring Data JPA и H2 в памяти.

Spring Security и JWT, библиотека JJWT 0.12.6.

Spring Cloud Eureka, Gateway, OpenFeign.

Resilience4J: retry и circuit breaker для вызовов hotel-service.

Micrometer Tracing + Zipkin для сквозной распределённой трассировки.

Springdoc OpenAPI, Swagger UI.

Lombok, Maven, JUnit 5 и MockMvc.

GitHub Actions для CI.

Функциональность

Аутентификация. Регистрация и вход по JWT, пароли хранятся в BCrypt. Роль ADMIN присваивается, если username содержит admin.

Отели и номера. CRUD, рекомендации свободных номеров с сортировкой по timesBooked. Эндпоинт /api/rooms/stats возвращает статистику загруженности.

Бронирование. Сага с компенсацией. Создание в статусе PENDING, подтверждение через Feign, переход в CONFIRMED. При ошибке статус CANCELLED и вызов releaseRoom.

Идемпотентность. Повторный запрос с тем же requestId возвращает существующее бронирование.

Пессимистичные блокировки. При подтверждении доступности номера используется PESSIMISTIC_WRITE, чтобы параллельные бронирования одного номера не создавали гонку.

Проверка пересечения дат. В hotel-service добавлена таблица room_bookings. При подтверждении доступности проверяется, что нет других броней на пересекающиеся даты. Если есть — возвращается false.

Retry и CircuitBreaker. Вызовы hotel-service обёрнуты в Resilience4J. При временной недоступности hotel-service booking-service повторяет запрос (3 попытки, задержка 500 мс). При частых ошибках цепь размыкается, запросы не висят в таймаутах.

Безопасность. JWT-фильтр на Gateway и в обоих сервисах (booking и hotel). Stateless-сессии, роли USER и ADMIN. @PreAuthorize на методах контроллера.

Обработка ошибок. RestControllerAdvice и ErrorDTO с корректными статусами 400, 401, 403, 404, 409, 503.

Документация. Swagger UI со схемой bearerAuth.

Предзаполнение данных. DataInitializer создаёт 2 отеля и 4 номера при старте hotel-service, а также пользователей admin/admin123 и user/user123 при старте booking-service.

Мониторинг и трассировка. Actuator: /actuator/health, /actuator/circuitbreakers, /actuator/retries. Сквозная трассировка через Zipkin.

Требования

JDK 17 или выше.

Maven 3.8 или выше.

Docker (для запуска Zipkin).

Порядок запуска

Запустите Zipkin:
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin

Сначала EurekaServerApplication, порт 8761.

Затем HotelServiceApplication, порт 8081.

Затем BookingServiceApplication, порт 8082.

Затем ApiGatewayApplication, порт 8080.

Подождите 10–20 секунд, пока сервисы зарегистрируются в Eureka по адресу http://localhost:8761.

Примеры запросов

Регистрация:
curl -X POST http://localhost:8080/api/bookings/user/register -H "Content-Type: application/json" -d '{"username":"testuser","password":"password123"}'
Ответ содержит поле token.

Создание бронирования:
curl -X POST http://localhost:8080/api/bookings/booking -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"startDate":"2026-12-01","endDate":"2026-12-05","autoSelect":true,"requestId":"req-001"}'
Ответ содержит статус CONFIRMED.

Список своих бронирований (с пагинацией):
curl -X GET "http://localhost:8080/api/bookings/booking/bookings?page=0&size=10" -H "Authorization: Bearer TOKEN"

Отмена бронирования:
curl -X DELETE http://localhost:8080/api/bookings/booking/1 -H "Authorization: Bearer TOKEN"

Статистика загруженности номеров:
curl -X GET http://localhost:8081/api/rooms/stats -H "Authorization: Bearer TOKEN"

Swagger UI

Booking Service: http://localhost:8082/swagger-ui/index.html
Hotel Service: http://localhost:8081/swagger-ui/index.html
В Swagger нажмите Authorize и вставьте JWT без префикса Bearer.

Трассировка (Zipkin)

Система использует Micrometer Tracing и Zipkin. Каждый запрос маркируется единым traceId, который прокидывается через Gateway, Booking и Hotel.

Как проверить:

Выполните любой запрос через Gateway.

Откройте Zipkin UI: http://localhost:9411

Нажмите Run Query. Появится трейс с цепочкой api-gateway → booking-service → hotel-service.

Пример сквозного трейса:

https://docs/zipkin-trace.png

Тесты

Команда запуска: mvn clean test -pl booking-service
Покрытие: UserControllerTest (5 тестов), BookingControllerTest (5 тестов, включая тест на сагу и компенсацию). CI прогоняет тесты автоматически на каждый push.

ADR (Architecture Decision Records)

Использование H2 in-memory вместо PostgreSQL.
Решение: H2 выбрана для упрощения запуска и проверки проекта без внешних зависимостей. В production следует заменить на PostgreSQL.

Сага вместо 2PC (Two-Phase Commit).
Решение: В микросервисной архитектуре глобальные транзакции (2PC) сложны и медленны. Вместо этого используется паттерн Saga с компенсацией: бронирование создаётся в PENDING, затем подтверждается через Feign. При ошибке статус меняется на CANCELLED, а временная блокировка снимается.

Кастомный TraceId vs Micrometer Tracing.
Решение: Сначала был реализован кастомный фильтр для TraceId (X-Correlation-Id). Затем он был заменён на стандартный Micrometer Tracing с Brave, так как это даёт интеграцию с Zipkin, автоматическое прокидывание контекста через Feign и реактивные цепочки Gateway.

CI/CD

Workflow GitHub Actions находится в файле .github/workflows/ci.yml.
Шаги: сборка через mvn clean compile, затем тесты через mvn test.
Запускается на push и pull request в ветку main.

Автор

Timur Nurzhau, репозиторий на GitHub: https://github.com/TimurNurzhau/hotel-booking-system