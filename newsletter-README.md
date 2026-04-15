# Newsletter & Email Campaign Manager API

A secure REST API built with **Java 17** and **Spring Boot 3.2** for managing mailing lists, subscribers, and email campaigns. Campaigns are scheduled for future delivery and sending is simulated via structured log output — no real email service required.

---

## Table of Contents
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Project Structure](#project-structure)
- [Setup & Run](#setup--run)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
- [Swagger UI](#swagger-ui)
- [Campaign Lifecycle](#campaign-lifecycle)
- [Simulated Email Sending](#simulated-email-sending)
- [Running Tests](#running-tests)
- [Postman Collection](#postman-collection)
- [Environment Variables](#environment-variables)

---

## Tech Stack

| Layer       | Technology                                    |
|-------------|-----------------------------------------------|
| Language    | Java 17 (no Lombok)                           |
| Framework   | Spring Boot 3.2                               |
| Security    | Spring Security + JWT (JJWT 0.11) + BCrypt    |
| Database    | MongoDB (Spring Data MongoDB)                 |
| Scheduling  | Spring `@Scheduled` (60-second polling)       |
| API Docs    | SpringDoc OpenAPI 3 / Swagger UI              |
| Build       | Maven                                         |
| Testing     | JUnit 5, MockMvc, Flapdoodle Embedded MongoDB |

---

## Features

- **JWT Authentication** — stateless token-based auth, 24-hour expiry, BCrypt password hashing
- **Mailing List Management** — create lists, add/remove subscribers, view subscriber details
- **Subscriber Validation** — email format validation, duplicate email prevention per list
- **Campaign Management** — create as DRAFT, edit name/subject/content, link to mailing list
- **Campaign Scheduling** — schedule campaigns for a future date/time with past-date validation
- **Simulated Email Sending** — background scheduler fires every 60 seconds, logs one line per subscriber, marks campaign SENT
- **Pagination** — campaign list supports page/size/status filtering
- **Ownership Isolation** — users can only access their own mailing lists and campaigns
- **Swagger UI** — full interactive documentation at `/swagger-ui.html`

---

## Project Structure

```
src/main/java/com/newsletter/
├── config/
│   ├── SecurityConfig.java          # JWT filter, stateless sessions, public routes
│   └── SwaggerConfig.java           # OpenAPI 3 + Bearer auth scheme
├── controller/
│   ├── AuthController.java          # POST /api/auth/register, /login
│   ├── MailingListController.java   # CRUD /api/mailing-lists + subscriber endpoints
│   └── CampaignController.java      # CRUD + schedule /api/campaigns
├── document/                        # MongoDB documents
│   ├── User.java                    # Implements UserDetails
│   ├── MailingList.java             # Subscribers embedded as List<Subscriber>
│   ├── Subscriber.java              # Embedded doc with UUID id field
│   ├── Campaign.java
│   └── CampaignStatus.java          # Enum: DRAFT, SCHEDULED, SENT, CANCELLED
├── dto/
│   ├── request/                     # AuthRequest, MailingListRequest, CampaignRequest
│   └── response/                    # ApiResponse (Auth, MailingListSummary, MailingListDetail,
│                                    #   SubscriberResponse, CampaignResponse, MessageResponse, ErrorResponse)
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── BadRequestException.java
│   └── UnauthorizedException.java
├── repository/
│   ├── UserRepository.java
│   ├── MailingListRepository.java
│   └── CampaignRepository.java      # Custom @Query for finding due campaigns
├── scheduler/
│   └── CampaignScheduler.java       # @Scheduled(fixedRate = 60_000)
├── security/
│   ├── JwtUtils.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
└── service/impl/
    ├── AuthServiceImpl.java
    ├── MailingListServiceImpl.java
    └── CampaignServiceImpl.java     # processDueCampaigns() — stock of send logic
```

### MongoDB Collections

| Collection      | Description                                                           |
|-----------------|-----------------------------------------------------------------------|
| `users`         | Registered users with BCrypt-hashed passwords                        |
| `mailing_lists` | Lists owned by a user; subscribers stored as an embedded array       |
| `campaigns`     | Campaigns with status, scheduledAt, sentAt, and owner reference      |

Subscribers are stored as an **embedded array** inside `mailing_lists`. Each subscriber has a UUID `id` for individual addressability without a separate collection.

---

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB (local or Docker)

### Start MongoDB with Docker
```bash
docker run -d -p 27017:27017 --name mongo mongo:7
```

### Build & run
```bash
git clone <repo-url>
cd newsletter-api
mvn clean package -DskipTests
mvn spring-boot:run
```

API available at **`http://localhost:8080`**

---

## Authentication

All endpoints except `/api/auth/**` require a JWT token.

### Step 1 — Register
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "secret123"
}
```

### Step 2 — Login
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "secret123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "john_doe",
  "email": "john@example.com"
}
```

### Step 3 — Use the token
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## API Endpoints

### Authentication

| Method | Endpoint               | Auth | Description                       |
|--------|------------------------|------|-----------------------------------|
| POST   | `/api/auth/register`   | ❌   | Register a new user               |
| POST   | `/api/auth/login`      | ❌   | Login and receive a JWT token     |

---

### Mailing Lists

| Method | Endpoint                                        | Description                          |
|--------|-------------------------------------------------|--------------------------------------|
| POST   | `/api/mailing-lists`                            | Create a new mailing list            |
| GET    | `/api/mailing-lists`                            | Get all mailing lists (own only)     |
| GET    | `/api/mailing-lists/{id}`                       | Get list details + subscriber array  |
| DELETE | `/api/mailing-lists/{id}`                       | Delete a mailing list                |
| POST   | `/api/mailing-lists/{id}/subscribers`           | Add a subscriber                     |
| DELETE | `/api/mailing-lists/{id}/subscribers/{subId}`   | Remove a subscriber                  |

**Create Mailing List:**
```json
{
  "name": "Weekly Newsletter",
  "description": "Our weekly subscriber digest"
}
```

**Add Subscriber:**
```json
{
  "name": "Alice Smith",
  "email": "alice@example.com"
}
```

**Mailing List Detail Response:**
```json
{
  "id": "64abc123def456",
  "name": "Weekly Newsletter",
  "subscriberCount": 2,
  "subscribers": [
    { "id": "uuid-1", "name": "Alice Smith", "email": "alice@example.com", "subscribedAt": "..." },
    { "id": "uuid-2", "name": "Bob Jones",   "email": "bob@example.com",   "subscribedAt": "..." }
  ],
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### Campaigns

| Method | Endpoint                       | Description                                       |
|--------|--------------------------------|---------------------------------------------------|
| POST   | `/api/campaigns`               | Create a campaign (saved as DRAFT)                |
| GET    | `/api/campaigns?page=0&size=10&status=DRAFT` | List campaigns (paginated, filterable) |
| GET    | `/api/campaigns/{id}`          | Get campaign by ID                                |
| PUT    | `/api/campaigns/{id}`          | Update campaign (DRAFT or SCHEDULED only)         |
| POST   | `/api/campaigns/{id}/schedule` | Schedule for a future date/time                   |
| DELETE | `/api/campaigns/{id}`          | Delete campaign (DRAFT or SCHEDULED only)         |

**Create Campaign:**
```json
{
  "name": "Summer Sale 2026",
  "subject": "Huge discounts this summer!",
  "content": "<h1>Summer Sale</h1><p>Up to 50% off on all items!</p>",
  "mailingListId": "64abc123def456"
}
```

**Schedule Campaign:**
```json
{
  "scheduledAt": "2027-01-15T09:00:00",
  "mailingListId": "64abc123def456"
}
```

**Campaign Response:**
```json
{
  "id": "64xyz789abc123",
  "name": "Summer Sale 2026",
  "subject": "Huge discounts this summer!",
  "content": "<h1>Summer Sale</h1>...",
  "status": "SCHEDULED",
  "mailingListId": "64abc123def456",
  "mailingListName": "Weekly Newsletter",
  "scheduledAt": "2027-01-15T09:00:00",
  "sentAt": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

**Status filter values:** `DRAFT` · `SCHEDULED` · `SENT` · `CANCELLED`

---

## Swagger UI

Once the application is running:

| URL | Description |
|-----|-------------|
| **`http://localhost:8080/swagger-ui.html`** | Interactive Swagger UI |
| `http://localhost:8080/api-docs`            | Raw OpenAPI JSON       |
| `http://localhost:8080/api-docs.yaml`       | Raw OpenAPI YAML       |

**How to use Swagger UI:**
1. Open `http://localhost:8080/swagger-ui.html`
2. Call `POST /api/auth/register` to create an account
3. Call `POST /api/auth/login` to get your token
4. Click **Authorize 🔒** and paste your token (no "Bearer" prefix needed in the UI)
5. All secured endpoints are now accessible

The `openapi.yaml` file in the project root can be imported into Swagger Editor at [editor.swagger.io](https://editor.swagger.io) or into Postman via *Import → OpenAPI*.

---

## Campaign Lifecycle

```
Create Campaign
      │
      ▼
   DRAFT ──────────────────► SCHEDULED
      │                           │
      │   (scheduledAt reached)   │
      │                           ▼
      │                        SENT ✓
      │
      └──────────────────────► CANCELLED (manual)
```

**Rules:**
- New campaigns always start as **DRAFT**
- `scheduledAt` must be strictly in the future — past timestamps return `400 Bad Request`
- Only DRAFT and SCHEDULED campaigns can be edited or deleted
- SENT campaigns are read-only — they cannot be modified or deleted

---

## Simulated Email Sending

Campaigns are not sent via a real email service. Instead, the system logs a structured line for each subscriber at send time, then marks the campaign SENT.

**Scheduler:** Runs every **60 seconds** via `@Scheduled(fixedRate = 60_000)`. When a SCHEDULED campaign's `scheduledAt` time has passed, it is processed immediately on the next scheduler tick.

**Log output example:**
```
INFO  CampaignServiceImpl - ========== SENDING CAMPAIGN: 'Summer Sale 2026' (id=64xyz789) ==========
INFO  CampaignServiceImpl - [EMAIL SENT] To: Alice Smith <alice@example.com> | Subject: Huge discounts this summer! | Campaign: Summer Sale 2026
INFO  CampaignServiceImpl - [EMAIL SENT] To: Bob Jones <bob@example.com>    | Subject: Huge discounts this summer! | Campaign: Summer Sale 2026
INFO  CampaignServiceImpl - ========== CAMPAIGN 'Summer Sale 2026' SENT to 2 subscriber(s) ==========
```

---

## Running Tests

Tests use **Flapdoodle Embedded MongoDB** — no external MongoDB instance needed.

```bash
mvn test
```

| Test Class                    | Cases | Coverage                                                        |
|-------------------------------|-------|-----------------------------------------------------------------|
| `AuthControllerTest`          | 7     | Register, login, duplicate username/email, validation, auth guard |
| `MailingListControllerTest`   | 9     | Full CRUD, add/remove subscriber, duplicate email, user isolation |
| `CampaignControllerTest`      | 13    | Create, update, schedule, past-date validation, pagination, status filter, ownership isolation |
| `CampaignServiceTest`         | 8     | Unit: schedule logic, due campaign processing, stock transitions |

---

## Postman Collection

Import **`NewsletterAPI.postman_collection.json`** into Postman.

Requests auto-save tokens and IDs using test scripts. Suggested flow:

1. **Register** → `token` saved automatically
2. **Create Mailing List** → `listId` saved
3. **Add Subscriber** → `subscriberId` saved
4. **Add Another Subscriber**
5. **Create Campaign** → `campaignId` saved
6. **Schedule Campaign** (use a future date)
7. **Get All Campaigns** / **Filter by status**

---

## Error Responses

All errors return a consistent JSON structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Mailing list not found with id: abc123",
  "timestamp": "2026-04-12T18:30:00"
}
```

Validation errors include a `fieldErrors` map:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "email": "Email must be valid",
    "name":  "Mailing list name is required"
  },
  "timestamp": "2026-04-12T18:30:00"
}
```

---

## Environment Variables

| Variable           | Default Value                                       | Description                               |
|--------------------|-----------------------------------------------------|-------------------------------------------|
| `MONGODB_URI`      | `mongodb://localhost:27017/newsletter_db`           | Full MongoDB connection string            |
| `MONGODB_DATABASE` | `newsletter_db`                                     | Database name                             |
| `JWT_SECRET`       | Built-in 64-char hex key                            | HS256 signing secret — **change in production** |

```bash
export MONGODB_URI=mongodb://localhost:27017/newsletter_db
export MONGODB_DATABASE=newsletter_db
export JWT_SECRET=your-own-secure-secret-here
mvn spring-boot:run
```
