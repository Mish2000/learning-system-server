# Quick Math Learning System

*A self-paced learning platform for mathematics with dynamic question generation, **per-subtopic adaptive difficulty**, real-time dashboards, and **integrated Local LLM** for personalized summaries and assistance.*

> **System Status:** This project is fully integrated with **Ollama** using the `aya-expanse:8b` model for multi-language (English/Hebrew) math reasoning.

---

## Table of Contents

* [Features](#features)
* [Architecture](#architecture)
* [Installation](#installation)
    * [Prerequisites](#prerequisites)
    * [Ollama Setup (Crucial)](#ollama-setup-crucial)
* [Configuration](#configuration)
* [Running the Project](#running-the-project)
* [User Guide](#user-guide)
    * [Home & AI Summary](#home--ai-summary)
    * [Practice Flow (Notebook UI)](#practice-flow-notebook-ui)
    * [Adaptive Difficulty Algorithm](#adaptive-difficulty-algorithm-details)
    * [Admin Topic Management](#admin-topic-management)
* [Troubleshooting](#troubleshooting)

---

## Features

1.  **Dynamic Question Generation** — Infinite practice for Arithmetic (Add, Sub, Mul, Div, Fractions) and Geometry (Rectangle, Circle, Triangle, Polygon).
2.  **Adaptive Difficulty Engine** — Per-subtopic adaptation based on rolling accuracy windows and streak hysteresis.
3.  **Personalized AI Tutor** —
    * **Home Dashboard:** Streams a personalized text summary of your strengths and weaknesses based on your history.
    * **Practice Mode:** Streams alternative step-by-step explanations for specific questions.
4.  **Interactive Notebook UI** — "Handwritten" style inputs, supporting multi-part answers (e.g., Numerator/Denominator or Area/Perimeter).
5.  **Admin Power Tools** — Soft-delete and **restore** topics; real-time system-wide statistics.
6.  **Localization** — Full support for **English** and **Hebrew** (RTL), including AI prompts and solution steps.

---

## Architecture

### Frontend
* **React 18** (Vite)
* **Material-UI (MUI)** with custom theming.
* **Server-Sent Events (SSE)** for real-time dashboards and AI streaming.
* **KaTeX / React-Markdown** for rendering mathematical formulas and AI responses.

### Backend
* **Spring Boot 3** (Java 17+)
* **Spring Security** with **JWT** (stored in HTTP-only cookies).
* **JPA / Hibernate** & **MySQL**.
* **Ollama Integration**: Custom `StreamingOllamaService` for handling LLM streams via SSE.
* **OllamaBootstrapper**: Attempts to auto-start the local Ollama process on server startup.

---

## Installation

### Prerequisites
* Node.js ≥ 16
* Java JDK 17+
* Maven
* MySQL 8.0+
* **Ollama** installed locally

### Ollama Setup (Crucial)
The system depends on a local AI model for summaries and hints.

1.  Download and install from [ollama.com](https://ollama.com/).
2.  Open your terminal and pull the specific model used by the backend:
    ```bash
    ollama pull aya-expanse:8b
    ```
    *(Note: The code is hardcoded to use `aya-expanse:8b` for its superior multi-language math capabilities. If you wish to use a different model, update `StreamingOllamaService.java`)*.

3.  Ensure Ollama is running (`ollama serve`). The backend will attempt to auto-start it, but manual startup is recommended.

---

## Configuration

Create a `.env` file or update `application.properties` in the backend:

```properties
spring.datasource.url=${DATABASE_URL}       # e.g., jdbc:mysql://localhost:3306/quickmath
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# Security
security.jwt.secret=YOUR_VERY_LONG_SECRET_KEY_HERE
security.cookies.secure=false # Set true for HTTPS