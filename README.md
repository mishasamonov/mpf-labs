# mpf-labs — комплексна лабораторна робота

Самонов Михайло. Багатомодульний Spring Boot-застосунок «Книжкова полиця» (лабораторні 1–10),
розгорнутий у хмарі Render: Docker-образ веб-модуля + керована PostgreSQL.

| Модуль | Роль |
|---|---|
| `core` | доменні моделі (Book, Comment, User), порти, сервіси бізнес-логіки, винятки |
| `persistence` | JPA/Hibernate-сутності, репозиторії, адаптери портів, Flyway-міграції `db/migration` |
| `web` | Spring MVC + Thymeleaf, REST API, Spring Security, AOP, обробка винятків, Actuator, Dockerfile |

## Локальний запуск (IntelliJ IDEA)

1. `docker compose up -d` — PostgreSQL 16 (`mpf_labs` / `mpf_user` / `mpf_pass`, порт 5432).
2. **mvn test (all modules)** — усі тести, зокрема `PostgresDeploymentTest` проти PostgreSQL.
3. **AppInit MPF (local PostgreSQL)** — профіль `local`, <http://localhost:8088>.
4. Тестові акаунти: `admin@bookshelf.test` / `admin123` (ADMIN), `olena@bookshelf.test` / `user123` (USER).

## Docker

```bash
mvn clean package -pl web -am
docker build -f web/Dockerfile -t mpf-labs-web .
docker run -p 8089:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/mpf_labs \
  -e SPRING_DATASOURCE_USERNAME=mpf_user -e SPRING_DATASOURCE_PASSWORD=mpf_pass mpf-labs-web
```

## Деплой

`predeploy` → GitHub Actions (`.github/workflows/ci.yml`: `mvn test` проти PostgreSQL-сервісу,
`mvn verify`, JAR-артефакт) → fast-forward у `deploy` → Render Web Service (Docker,
`web/Dockerfile`, контекст — корінь репозиторію).

Змінні середовища на Render: `SPRING_DATASOURCE_URL` (internal URL у форматі
`jdbc:postgresql://HOST:5432/DB`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
`APP_BASE_URL`, `APP_MAIL_ADMIN`, `RESEND_API_KEY`. Секрети в репозиторії не зберігаються.

Health-check: `/actuator/health` (публічний), `/actuator/info`, `/actuator/metrics` (ADMIN).
