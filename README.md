Hotel Booking System
Распределённая система бронирования отелей на Spring Boot с микросервисной архитектурой.

Модули и порты
eureka-server — порт 8761. Реестр сервисов, Service Discovery.

api-gateway — порт 8080. API Gateway, маршрутизация и JWT-проверка на входе.

hotel-service — порт 8081. Управление отелями и номерами.

booking-service — порт 8082. Бронирования, JWT Security, сага.

Технологии
Java 17, Spring Boot 3.5.6, Spring Cloud 2025.0.0.

Spring Data JPA и H2 в памяти.

Spring Security и JWT, библиотека JJWT 0.12.6.

Spring Cloud Eureka, Gateway, OpenFeign.

Resilience4J: retry и circuit breaker для вызовов hotel-service.

Micrometer Tracing + Zipkin для сквозной распределённой трассировки.

Springdoc OpenAPI, Swagger UI.

Lombok, MapStruct, Maven, JUnit 5 и MockMvc.

GitHub Actions для CI.

Архитектура системы

Клиент отправляет HTTP-запросы на API Gateway (порт 8080).

API Gateway маршрутизирует запросы:

путь /api/bookings/** — в Booking Service (порт 8082)

путь /api/hotels/** — в Hotel Service (порт 8081)

Booking Service при создании бронирования вызывает Hotel Service через OpenFeign, используя Eureka для обнаружения сервисов.

Каждый сервис имеет свою in-memory базу данных H2:

Booking Service — bookingdb

Hotel Service — hoteldb

Все сервисы (API Gateway, Booking Service, Hotel Service) регистрируются в Eureka Server (порт 8761).

Все сервисы отправляют трассировки в Zipkin (порт 9411).

Схема взаимодействия:
Клиент -> API Gateway -> Booking Service / Hotel Service
Booking Service -> Hotel Service (через Feign)
Все сервисы -> Eureka Server (регистрация)
Все сервисы -> Zipkin (трассировка)

Функциональность
Аутентификация. Регистрация и вход по JWT, пароли хранятся в BCrypt. Публичная регистрация всегда выдает роль USER. Роль ADMIN назначается только через административные эндпоинты или предзаполнение (DataInitializer).

Отели и номера. CRUD, рекомендации свободных номеров с сортировкой по timesBooked. Эндпоинт /api/rooms/stats возвращает статистику загруженности.

Бронирование. Сага с компенсацией. Создание в статусе PENDING, подтверждение через Feign, переход в CONFIRMED. При ошибке статус CANCELLED и вызов releaseRoom.

Идемпотентность. Повторный запрос с тем же requestId возвращает существующее бронирование.

Пессимистичные блокировки и защита от Race Condition. При подтверждении доступности номера используется PESSIMISTIC_WRITE, а также уникальный индекс в БД на таблицу room_bookings, чтобы параллельные бронирования одного номера не создавали дубликатов.

Проверка пересечения дат. В hotel-service добавлена таблица room_bookings. При подтверждении доступности проверяется, что нет других броней на пересекающиеся даты. Если есть — возвращается false.

Retry и CircuitBreaker. Вызовы hotel-service обёрнуты в Resilience4J. При временной недоступности booking-service повторяет запрос (3 попытки, задержка 500 мс). При частых ошибках цепь размыкается, запросы не висят в таймаутах.

Безопасность. JWT-фильтр на Gateway и в обоих сервисах (booking и hotel). Stateless-сессии, роли USER и ADMIN. @PreAuthorize на методах контроллера.

Маршрутизация Gateway. Gateway срезает префиксы /api/bookings и /api/hotels перед передачей в сервисы. Например, запрос /api/hotels/api/rooms/stats доходит до RoomController как /api/rooms/stats.

Обработка ошибок. @RestControllerAdvice и ErrorDTO с корректными статусами 400, 401, 403, 404, 409, 503.

Документация. Swagger UI со схемой bearerAuth.

Предзаполнение данных. DataInitializer создаёт 2 отеля и 4 номера при старте hotel-service, а также пользователей admin/admin123 и user/user123 при старте booking-service.

Мониторинг и трассировка. Actuator: /actuator/health, /actuator/circuitbreakers, /actuator/retries. Сквозная трассировка через Zipkin.

Маппинг DTO. Реализован через MapStruct (генерация на этапе компиляции, без рефлексии).

Требования
JDK 17 или выше.

Maven 3.8 или выше.

Свободные порты: 8080 (Gateway), 8081 (Hotel), 8082 (Booking), 8761 (Eureka), 9411 (Zipkin).

Docker (для запуска Zipkin).

Порядок запуска
Запустите Zipkin:

bash
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin
Сначала EurekaServerApplication (порт 8761).

Затем HotelServiceApplication (порт 8081).

Затем BookingServiceApplication (порт 8082).

Затем ApiGatewayApplication (порт 8080).

Подождите 10–20 секунд, пока сервисы зарегистрируются в Eureka.

После запуска проверьте, что все сервисы видны в Eureka. В разделе Instances currently registered with Eureka должны быть API-GATEWAY, BOOKING-SERVICE и HOTEL-SERVICE

Примеры запросов
Регистрация:

bash
curl -X POST /api/bookings/user/register \
-H "Content-Type: application/json" \
-d '{"username":"testuser","password":"password123"}'
Ответ содержит поле token.

Создание бронирования:

bash
curl -X POST /api/bookings/booking \
-H "Authorization: Bearer TOKEN" \
-H "Content-Type: application/json" \
-d '{"startDate":"2026-12-01","endDate":"2026-12-05","autoSelect":true,"requestId":"req-001"}'
Ответ содержит статус CONFIRMED.

Список своих бронирований (с пагинацией):

bash
curl -X GET "/api/bookings/bookings?page=0&size=10" \
-H "Authorization: Bearer TOKEN"
Отмена бронирования:

bash
curl -X DELETE /api/bookings/booking/1 \
-H "Authorization: Bearer TOKEN"
Статистика загруженности номеров:

bash
curl -X GET /api/hotels/api/rooms/stats \
-H "Authorization: Bearer TOKEN"
Swagger UI
Booking Service: /swagger-ui/index.html

Hotel Service: /swagger-ui/index.html

В Swagger нажмите Authorize и вставьте JWT без префикса Bearer.

Трассировка (Zipkin)
Система использует Micrometer Tracing и Zipkin. Каждый запрос маркируется единым traceId, который прокидывается через Gateway, Booking и Hotel.

Как проверить:

Выполните любой запрос через Gateway.

Откройте Zipkin UI (порт 9411).

Нажмите Run Query. Появится трейс с цепочкой api-gateway → booking-service → hotel-service.

Пример сквозного трейса:

https://github.com/TimurNurzhau/hotel-booking-system/blob/main/docs/zipkin-trace.png

Тесты
Команда запуска:

bash
mvn clean test -pl booking-service
Покрытие: UserControllerTest (5 тестов), BookingControllerTest (8 тестов, включая тест на сагу, компенсацию и тайм-аут), BookingServiceUnitTest (1 тест на компенсацию при падении БД). Итого: 14 тестов. CI прогоняет тесты автоматически на каждый push.

ADR (Architecture Decision Records)
Использование H2 in-memory вместо PostgreSQL.
Решение: H2 выбрана для упрощения запуска и проверки проекта без внешних зависимостей. В production следует заменить на PostgreSQL.

Сага вместо 2PC (Two-Phase Commit).
Решение: В микросервисной архитектуре глобальные транзакции (2PC) сложны и медленны. Вместо этого используется паттерн Saga с компенсацией: бронирование создаётся в PENDING, затем подтверждается через Feign. При ошибке статус меняется на CANCELLED, а временная блокировка снимается.

Кастомный TraceId vs Micrometer Tracing.
Решение: Сначала был реализован кастомный фильтр для TraceId (X-Correlation-Id). Затем он был заменён на стандартный Micrometer Tracing с Brave, так как это даёт интеграцию с Zipkin, автоматическое прокидывание контекста через Feign и реактивные цепочки Gateway.

Компенсация без CircuitBreaker.
Решение: Метод releaseRoom в HotelServiceClient намеренно оставлен без @Retry и @CircuitBreaker. Если confirmAvailability падает, цепь размыкается, и компенсация не выполнилась бы. Поэтому releaseRoom вызывается напрямую, чтобы гарантировать откат блокировки даже при разомкнутой цепи.

CI/CD
Workflow GitHub Actions находится в файле .github/workflows/ci.yml.
Шаги: сборка через mvn clean compile, затем тесты через mvn test.
Запускается на push и pull request в ветку main.

Автор
Timur Nurzhau, репозиторий на GitHub: https://github.com/TimurNurzhau/hotel-booking-system