# Quick Math Learning System (Refactored)

*A self-paced learning platform for mathematics with dynamic question generation, **per-subtopic adaptive difficulty**, real-time dashboards, and local LLM assistance.*

> This README replaces and updates the previous version to reflect the new **adaptive difficulty algorithm** and **practice flow** (no manual difficulty pickers).

---

## Table of Contents

* [What’s New](#whats-new)
* [Features](#features)
* [Architecture](#architecture)
* [Installation](#installation)

    * [Installing Ollama on PC](#installing-ollama-on-pc)
* [Configuration](#configuration)
* [Running the Project](#running-the-project)
* [User Guide](#user-guide)

    * [Registration & Login](#registration--login)
    * [Practice Flow (Auto Difficulty)](#practice-flow-auto-difficulty)
    * [Adaptive Difficulty Algorithm (Details)](#adaptive-difficulty-algorithm-details)
    * [Notifications & Dashboards](#notifications--dashboards)
    * [Admin Topic Management](#admin-topic-management)
* [Technology Stack](#technology-stack)
* [Troubleshooting](#troubleshooting)
* [Contributing](#contributing)
* [Dynamic Question Generator Overview](#dynamic-question-generator-overview)

    * [Topics and Subtopics](#topics-and-subtopics)
    * [Difficulty Handling](#difficulty-handling)
    * [Random Generation of Questions](#random-generation-of-questions)
    * [Complete Solutions](#complete-solutions)
    * [Summary](#summary)

---

## What’s New

**Core behavior changes:**

* **Per-subtopic adaptive difficulty**
  Difficulty is now determined **automatically** per **user × subtopic**, based solely on the user’s **recent attempts** in that subtopic.
* **No manual difficulty selection** in the practice UI
  The previous difficulty selector was removed. The backend returns the current difficulty for the next question.
* **Hysteresis + streak logic to avoid “yo-yo” effects**
  Promotions/demotions depend on a rolling window of recent answers and require short streaks, so difficulty won’t jump after one lucky/unlucky answer.
* **Dashboard difficulties are user-specific and live**
  `topicDifficulty` (parent) and `subtopicDifficulty` are computed from the user’s own history; parent difficulty aggregates child difficulties.

**API & UI adjustments:**

* **`POST /api/questions/generate`** now ignores any client-provided difficulty. Send only `{ "topicId": <subtopic-or-parent-id> }`. The response includes the difficulty that was chosen.
* **Practice page** displays the current difficulty from the loaded question payload and updates naturally on “Next Question.”
* **Dashboard page** shows current per-topic and per-subtopic difficulties in real time via SSE.

---

## Features

1. **Dynamic Question Generation** — Arithmetic & Geometry questions with difficulty-scaled ranges.
2. **Adaptive Difficulty (Per Subtopic)** — Windowed accuracy + streak-based hysteresis; no manual difficulty picker.
3. **Real-Time UX** — SSE for user dashboard updates and AI streaming.
4. **Admin Dashboard** — System-wide stats and topic management (add/remove when allowed).

---

## Architecture

### Frontend

* **React** (Vite, React Router)
* **Axios** for REST
* **SSE** for dashboards and AI streaming
* **Material-UI (MUI)** components
* **i18next** for i18n (English, Hebrew)

### Backend

* **Spring Boot (Java)**

    * Spring Security + JWT
    * JPA (Hibernate) for persistence
    * SSE endpoints for live updates
* **MySQL** as the primary datastore
* **Ollama** for local LLM generation/streaming

---

## Installation

1. **Prerequisites**

    * Node.js ≥ 16
    * Java (JDK 17+ recommended)
    * Maven
    * MySQL
    * Ollama (see below)

2. **Clone**

   ```bash
   git clone <your-repo>
   ```

3. **Frontend**

   ```bash
   cd client
   npm install
   npm run dev
   ```

   Vite serves at `http://localhost:5173` by default.

4. **Backend**

   ```bash
   cd server
   mvn clean install
   mvn spring-boot:run
   ```

   Spring Boot serves at `http://localhost:8080` by default.

### Installing Ollama on PC

1. Download installer from **[https://ollama.com/](https://ollama.com/)**
2. Install and verify:

   ```bash
   ollama --version
   ```
3. (Optional) Pull a model:

   ```bash
   ollama pull gemma3:27b
   ```
4. Ollama listens on `http://localhost:11434` (default).

---

## Configuration

Use environment variables and `application.properties`:

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

If you change the Ollama port, update references in the backend where the Ollama base URL is used.

---

## Running the Project

1. Ensure **Ollama** is running (`ollama serve`).
2. Start **backend** (`mvn spring-boot:run`).
3. Start **frontend** (`npm run dev`).
4. Open `http://localhost:5173` and sign in.

---

## User Guide

### Registration & Login

* Register a new account or log in with existing credentials.
* Tokens are managed by the app; authenticated calls include `Authorization` headers automatically.

### Practice Flow (Auto Difficulty)

1. Navigate to **Practice**.
2. Select a **parent topic** (e.g., Arithmetic / Geometry) and a **subtopic** (e.g., Addition, Triangle, etc.).
3. Click **Generate**.

    * **No difficulty picker** — the backend **auto-chooses** difficulty per your **history on that subtopic**.
    * The response includes `difficultyLevel`, which the UI displays.
4. Submit your answer. View correctness and expand to see solution steps.
5. Click **Next Question** to continue — your difficulty will adjust **only** when the algorithm rules trigger (see below).

> **Cold start:** If you’ve never practiced a subtopic, the system defaults to **BASIC** until it gathers enough recent attempts.

### Adaptive Difficulty Algorithm (Details)

**Scope**: per **user × subtopic** (not global).

**Signals**:

* Uses a **rolling window** of your **last N attempts** in the current subtopic (current default N = **8**).
* Computes **success rate (SR)** over that window.
* Checks short **streaks** at the head of the window.

**Hysteresis rule (prevents “yo-yo”)**:

* **Promote** one level **if** `SR ≥ 0.80` **and** your **last 2** attempts are **correct**.
* **Demote** one level **if** `SR ≤ 0.40` **and** your **last 2** attempts are **incorrect**.
* **Otherwise** keep the current level.

**Defaults**:

* No history for a subtopic ⇒ **BASIC**.
* Difficulty levels (ordered): `BASIC < EASY < MEDIUM < ADVANCED < EXPERT`.

**Dashboard effects**:

* **Subtopic difficulty** = result of the algorithm above.
* **Parent topic difficulty** = aggregation of the child subtopics (mean of indices, rounded to nearest level).
* Dashboard updates automatically via **SSE** after each submission.

> This design avoids spikes caused by historical high averages and responds to **recent** performance while requiring small streaks to confirm a trend.

### Notifications & Dashboards

* **User Dashboard**: attempts, success rates by topic, and **live difficulties** per topic/subtopic.
* **Admin Dashboard**: global attempts and success rates, plus topic distribution.
* **SSE** streams keep both dashboards updated after each submission.
* Optional AI helper streams solution ideas via a dedicated SSE endpoint.

### Admin Topic Management

* Admins can add **topics** and **subtopics**.
* Deleting a parent requires it to be empty.

---

## Technology Stack

* **Frontend**: React (Vite), MUI, Axios, i18next, SSE
* **Backend**: Spring Boot, Spring Security (JWT), JPA/Hibernate, SSE
* **Database**: MySQL
* **Local LLM**: Ollama

---

## Troubleshooting

* **401 Unauthorized**
  Ensure you’re logged in and the token is present/valid.
* **DB connection issues**
  Verify MySQL credentials and `spring.datasource.*` values.
* **SSE not updating**
  Some proxies/browsers can disrupt SSE. Check DevTools **Network** tab and CORS settings.
* **Auto difficulty “stuck”**
  The algorithm reacts to **recent** attempts. Complete a few more questions in the same subtopic to trigger a change if warranted.
* **Cold Start**
  New subtopic starts at **BASIC** until it has a short history window.

---

## Contributing

1. Fork/branch your changes.
2. Ensure the adaptive difficulty rules remain **pure** (stateless per request, derived from history only).
3. Add tests or manual steps to verify:

    * Promotions/demotions under the stated thresholds.
    * First-time subtopic defaulting to BASIC.
    * Dashboard maps reflect recent changes.

---

# Dynamic Question Generator Overview

The generator (service layer) creates math questions using the selected **topic/subtopic** and the **auto-chosen difficulty** returned by the adaptive logic.

## Topics and Subtopics

* **Arithmetic**: Addition, Subtraction, Multiplication, Division, Fractions…
* **Geometry**: Rectangle, Triangle, Circle, Polygon…

The subtopic decides which `createXxxQuestion` method is used.

## Difficulty Handling

Difficulty controls random ranges for operands. Typical mapping:

```java
switch (difficulty) {
  case BASIC   -> new int[]{1, 10};
  case EASY    -> new int[]{1, 30};
  case MEDIUM  -> new int[]{1, 100};
  case ADVANCED-> new int[]{1, 1000};
  case EXPERT  -> new int[]{1, 9999};
}
```

> The difficulty you see in the Practice page is the **server-selected** level for that subtopic attempt.

## Random Generation of Questions

Each `createXYZQuestion` method:

1. Samples operands within difficulty-scaled ranges.
2. Builds a readable question prompt.
3. Computes the correct answer.
4. Produces solution steps (via `QuestionAlgorithmsFunctions`).
5. Persists a `GeneratedQuestion` record.

## Complete Solutions

The UI shows **correctness** and allows expanding **solution steps** (rendered nicely; math supported via KaTeX).

## Summary

* Topic-aware question creation
* Difficulty-scaled operand ranges
* Auto-selected difficulty per **user × subtopic** with **hysteresis**
* Complete solutions and real-time dashboards

Enjoy practicing!
