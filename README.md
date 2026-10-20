Hotel Booking System

Распределённая система бронирования отелей на Spring Boot с микросервисной архитектурой.

Модули и порты

eureka-server, порт 8761. Реестр сервисов, Service Discovery.

api-gateway, порт 8080. API Gateway, маршрутизация запросов.

hotel-service, порт 8081. Управление отелями и номерами.

booking-service, порт 8082. Бронирования и JWT Security.

Технологии

Java 17, Spring Boot 3.5.6, Spring Cloud 2025.0.0.

Spring Data JPA и H2 в памяти.

Spring Security и JWT, библиотека JJWT 0.12.6.

Spring Cloud Eureka, Gateway, OpenFeign.

Springdoc OpenAPI, Swagger UI.

Lombok, Maven, JUnit 5 и MockMvc.

GitHub Actions для CI.

Функциональность

Аутентификация. Регистрация и вход по JWT, пароли хранятся в BCrypt.

Отели и номера. CRUD, рекомендации свободных номеров с сортировкой по timesBooked.

Бронирование. Сага с компенсацией. Создание в статусе PENDING, подтверждение через Feign, переход в CONFIRMED. При ошибке статус CANCELLED и вызов releaseRoom.

Идемпотентность. Повторный запрос с тем же requestId возвращает существующее бронирование.

Обработка ошибок. RestControllerAdvice и ErrorDTO с корректными статусами 400, 404, 409, 503.

Безопасность. JWT-фильтр, stateless-сессии, роли USER и ADMIN.

Документация. Swagger UI со схемой bearerAuth.

Предзаполнение данных. DataInitializer создаёт 2 отеля и 4 номера при старте hotel-service.

Требования

JDK 17 или выше.

Maven 3.8 или выше.

Порядок запуска

Сначала EurekaServerApplication, порт 8761.

Затем HotelServiceApplication, порт 8081.

Затем BookingServiceApplication, порт 8082.

Опционально ApiGatewayApplication, порт 8080.

Подождите 10–20 секунд, пока сервисы зарегистрируются в Eureka по адресу http://localhost:8761.

Примеры запросов

Регистрация.

curl -X POST http://localhost:8082/user/register -H "Content-Type: application/json" -d "{"username":"testuser","password":"password123"}"

Ответ содержит поле token.

Создание бронирования.

curl -X POST http://localhost:8082/booking -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d "{"startDate":"2026-12-01","endDate":"2026-12-05","autoSelect":true,"requestId":"req-001"}"

Ответ содержит статус CONFIRMED.

Список своих бронирований.

curl -X GET http://localhost:8082/booking/bookings -H "Authorization: Bearer TOKEN"

Отмена бронирования.

curl -X DELETE http://localhost:8082/booking/1 -H "Authorization: Bearer TOKEN"

Swagger UI

Booking Service доступен по адресу http://localhost:8082/swagger-ui/index.html

Hotel Service доступен по адресу http://localhost:8081/swagger-ui/index.html

В Swagger нажмите Authorize и вставьте JWT без префикса Bearer.

Тесты

Команда запуска: mvn clean test -pl booking-service

Покрытие. UserControllerTest содержит 5 тестов, BookingControllerTest содержит 4 теста. CI прогоняет тесты автоматически на каждый push.

CI/CD

Workflow GitHub Actions находится в файле .github/workflows/ci.yml.

Шаги: сборка через mvn clean compile, затем тесты через mvn test.

Запускается на push и pull request в ветку main.

Автор

Timur Nurzhau, репозиторий на GitHub: https://github.com/TimurNurzhau/hotel-booking-system

