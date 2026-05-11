# QuickMath AI Learning System — Backend API

![Java](https://img.shields.io/badge/Java-17+-red?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-6DB33F?logo=springboot&logoColor=white)
![Security](https://img.shields.io/badge/Auth-JWT%20HTTP--only%20cookies-blue)
![Database](https://img.shields.io/badge/Database-MySQL-4479A1?logo=mysql&logoColor=white)
![AI](https://img.shields.io/badge/Local%20AI-Ollama-black)

Backend service for **QuickMath AI Learning System** — a full-stack adaptive mathematics learning platform with authenticated users, topic-based practice, real-time dashboards, role-based administration, and local LLM assistance through Ollama.

The project demonstrates backend engineering around **Spring Boot**, **Spring Security**, **JWT cookie sessions**, **JPA/Hibernate**, **Server-Sent Events**, **adaptive learning logic**, and **local AI streaming** without depending on paid cloud AI APIs.

---

## What this backend does

QuickMath is designed to help students practice mathematics through dynamically generated exercises and immediate feedback. The backend is responsible for:

- Authenticating users with secure cookie-based JWT sessions.
- Generating arithmetic and geometry questions by topic and difficulty.
- Recording answer attempts, correctness, timing, and solution history.
- Adapting each user's difficulty per subtopic using recent performance signals.
- Streaming personalized AI explanations and learning summaries through a local Ollama model.
- Serving real-time user/admin dashboards using Server-Sent Events.
- Managing topics, subtopics, soft-deletion, and restoration through admin-only APIs.
- Persisting users, topics, questions, progress, history, notifications, and profile data in MySQL.

---

## Key features

### Authentication and authorization

- Register, login, refresh, logout, and current-session inspection endpoints.
- JWT access/refresh tokens stored in **HTTP-only cookies**.
- Stateless Spring Security configuration.
- Role-based access model with `USER` and `ADMIN` roles.
- Admin-only topic management protected server-side.
- Refresh-token flow for long sessions without exposing tokens to frontend JavaScript.

### Adaptive learning engine

The backend tracks performance per user and subtopic. It uses recent answer history, streaks, and cooldown logic to decide when a user should move up or down in difficulty.

Current difficulty levels:

```text
BASIC -> EASY -> MEDIUM -> ADVANCED -> EXPERT
```

The adaptive flow updates `UserSubtopicProgress` records and can emit notifications when a user's difficulty changes.

### Dynamic question generation

The backend supports math practice across seeded topic groups such as:

- Arithmetic
  - Addition
  - Subtraction
  - Multiplication
  - Division
  - Fractions
- Geometry
  - Rectangle
  - Circle
  - Triangle
  - Polygon

Question responses include the generated text, expected answer, difficulty level, topic id, and solution steps.

### Local AI tutor with Ollama

The system integrates with **Ollama** to stream AI-generated explanations and personalized learning summaries. This keeps the AI component local and avoids cloud API keys.

Default model:

```bash
aya-expanse:8b
```

The model was selected for multilingual reasoning, especially English/Hebrew support.

### Real-time dashboards and notifications

The backend exposes SSE streams for:

- User dashboard updates.
- Admin dashboard updates.
- User notifications.
- AI explanation streaming.

This gives the frontend live updates without polling.

---

## Tech stack

| Area | Technology |
|---|---|
| Runtime | Java 17+ |
| Framework | Spring Boot 3.4.x |
| API | Spring Web / REST controllers |
| Security | Spring Security, JWT, HTTP-only cookies |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL |
| AI integration | Ollama, OkHttp, Jackson |
| Realtime | Server-Sent Events |
| Config | dotenv-java + Spring properties |
| Build | Maven / Maven Wrapper |

---

## Project structure

```text
src/main/java/com/learningsystemserver/
├── advice/          # Global exception handling and API error responses
├── config/          # Startup / schema support utilities
├── controllers/     # REST and SSE API controllers
├── dtos/            # Request and response DTOs
├── entities/        # JPA entities and enums
├── exceptions/      # Domain-specific exceptions and messages
├── repositories/    # Spring Data repositories
├── security/        # Spring Security and CORS configuration
├── services/        # Auth, AI, dashboard, adaptive logic, topics, notifications
└── utils/           # Cookie helpers, language utilities, seed data
```

Important backend areas:

| File / package | Purpose |
|---|---|
| `AuthController` | Register, login, refresh, logout, `/me` session inspection |
| `JwtService` | Access/refresh token generation and validation |
| `JwtAuthenticationFilter` | Reads JWT cookies and populates Spring Security context |
| `QuestionController` | Question generation, answer submission, answer history |
| `QuestionGeneratorService` | Arithmetic/geometry question generation logic |
| `AdaptiveService` | Per-subtopic difficulty adjustment logic |
| `DashboardService` | User/admin dashboard aggregation |
| `SseDashboardController` | Live dashboard SSE streams |
| `StreamingOllamaService` | Streams local LLM responses from Ollama |
| `TopicService` | Topic/subtopic CRUD, soft delete, restore |
| `TopicInitializer` | Seeds the initial math topic catalog |
| `NotificationService` | Stores and emits user notifications |

---

## API overview

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create a new user account |
| `POST` | `/api/auth/login` | Authenticate and set access/refresh cookies |
| `POST` | `/api/auth/refresh` | Refresh the access cookie using the refresh cookie |
| `POST` | `/api/auth/logout` | Clear auth cookies |
| `GET` | `/api/auth/me` | Return current authenticated role/session info |

### Profile

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/profile` | Get current user profile |
| `PUT` | `/api/profile` | Update username, password, and interface language |
| `POST` | `/api/profile/uploadImage` | Upload a profile image |
| `DELETE` | `/api/profile/image` | Delete profile image |

### Topics

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/topics` | List top-level topics or subtopics by `parentId` |
| `GET` | `/api/topics/{id}` | Get a specific topic |
| `POST` | `/api/topics` | Create topic or subtopic, admin only |
| `PUT` | `/api/topics/{id}` | Update topic, admin only |
| `DELETE` | `/api/topics/{id}` | Soft-delete topic, admin only |
| `GET` | `/api/topics/deleted` | List deleted topics, admin only |
| `PUT` | `/api/topics/{id}/restore` | Restore deleted topic, admin only |

### Practice

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/questions/generate` | Generate a question for selected topic/subtopic |
| `POST` | `/api/questions/submit` | Submit answer and update history/progress |
| `GET` | `/api/questions/{id}` | Retrieve generated question by id |

### AI and realtime

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/ai/stream` | Stream AI explanation/summary through SSE |
| `GET` | `/api/sse/user-dashboard` | Stream current user's dashboard updates |
| `GET` | `/api/sse/admin-dashboard` | Stream admin dashboard updates |
| `GET` | `/api/notifications/stream` | Stream user notifications |
| `GET` | `/api/notifications` | List current user's notifications |
| `POST` | `/api/notifications/markRead/{id}` | Mark one notification as read |
| `DELETE` | `/api/notifications/clearAll` | Clear current user's notifications |

---

## Prerequisites

Install the following before running the backend:

- Java JDK 17 or newer
- Maven, or use the included Maven Wrapper
- MySQL 8.x
- Ollama, for AI-powered explanations and summaries

Pull the default local AI model:

```bash
ollama pull aya-expanse:8b
```

Start Ollama manually if needed:

```bash
ollama serve
```

---

## Configuration

Create a local `.env` file from `.env.example`:

```bash
cp .env.example .env
```

Example configuration:

```env
DATABASE_URL=jdbc:mysql://localhost:3306/quickmath?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DATABASE_USERNAME=root
DATABASE_PASSWORD=change_me

JWT_SECRET=replace_with_a_strong_secret_of_at_least_32_characters
FRONTEND_ORIGIN=http://localhost:5173
SECURITY_COOKIES_SECURE=false

OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=aya-expanse:8b
OLLAMA_AUTO_START=true
```

Recommended Spring properties:

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

app.frontend.origin=${FRONTEND_ORIGIN:http://localhost:5173}
security.jwt.secret=${JWT_SECRET}
security.cookies.secure=${SECURITY_COOKIES_SECURE:false}

app.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
app.ollama.model=${OLLAMA_MODEL:aya-expanse:8b}
app.ollama.auto-start=${OLLAMA_AUTO_START:true}
```

Never commit real `.env` files or secrets.

---

## Running locally

From the backend repository root:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend starts by default on:

```text
http://localhost:8080
```

The frontend development server should run on:

```text
http://localhost:5173
```

The frontend uses `/api` requests and should be configured to proxy those requests to the backend during development.

---

## Build and test

Run tests:

```bash
./mvnw test
```

Create a production build artifact:

```bash
./mvnw clean package
```

Skip tests only when intentionally creating a quick local build:

```bash
./mvnw clean package -DskipTests
```

---

## Database initialization

The backend uses JPA/Hibernate and MySQL. On startup, the system seeds an initial topic tree through `TopicInitializer`, including arithmetic and geometry topics.

Example initial hierarchy:

```text
Arithmetic
├── Addition
├── Subtraction
├── Multiplication
├── Division
└── Fractions

Geometry
├── Rectangle
├── Circle
├── Triangle
└── Polygon
```

New users receive initial progress records so the adaptive engine can track learning per subtopic from the first practice session.

---

## Security model

The backend is designed around these principles:

- Tokens are stored in HTTP-only cookies, not localStorage.
- Access tokens are short-lived.
- Refresh tokens are used to renew sessions.
- Admin privileges are determined by persisted server-side roles.
- Admin-only endpoints are protected on the backend, not only hidden in the UI.
- CORS is restricted to the configured frontend origin.
- Secrets are provided through environment variables.
- User-scoped actions, such as notifications and profile access, are resolved from the authenticated principal.

---

## AI streaming flow

AI explanations are served through SSE:

1. The frontend opens an `EventSource` connection to `/api/ai/stream`.
2. The backend builds a language-aware prompt.
3. `StreamingOllamaService` sends the prompt to the local Ollama generate API.
4. Tokens are streamed back to the browser as they are produced.
5. The frontend renders the response progressively.

This provides an interactive tutor-like experience without requiring OpenAI, Anthropic, Gemini, or any other cloud LLM provider.

---

## What this project demonstrates

This repository is especially relevant for backend/full-stack roles because it includes:

- Real authentication and session management.
- Role-based authorization.
- Stateful domain modeling with JPA entities and repositories.
- Adaptive business logic beyond simple CRUD.
- SSE-based realtime communication.
- Local LLM integration.
- Multilingual user experience support.
- Admin and user-facing product flows.
- Clean separation between controllers, services, repositories, DTOs, and entities.

---

## Related repository

This backend is intended to run together with the React/Vite frontend client.

```text
learning-system-client  -> React UI
learning-system-server  -> Spring Boot API
```

Run the backend first, then start the client so the Vite proxy can forward `/api` requests to `localhost:8080`.
