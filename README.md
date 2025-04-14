
---

# Quick Math Learning System

A self-paced learning platform for mathematics, offering dynamic question generation, automatic difficulty adaptation, user analytics, and more.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Installation](#installation)
    - [Installing Ollama on PC](#installing-ollama-on-pc)
- [Configuration](#configuration)
- [Running the Project](#running-the-project)
- [User Guide](#user-guide)
- [Technology Stack](#technology-stack)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Dynamic Question Generator Overview](#dynamic-question-generator-overview)
    - [Topics and Subtopics](#topics-and-subtopics)
    - [Difficulty Handling](#difficulty-handling)
    - [Random Generation of Questions](#random-generation-of-questions)
    - [Complete Solutions](#complete-solutions)
    - [Summary](#summary)

---

## Features

1. **Dynamic Question Generation** – Users can generate arithmetic and geometry questions at various difficulty levels.
2. **Adaptive Difficulty** – The system adjusts the user’s difficulty level (and potentially sublevels) based on recent performance.
3. **Notifications** – Real-time SSE notifications for difficulty changes, sublevel changes, new messages, etc.
4. **Admin Dashboard** – Real-time usage statistics and the ability to manage question topics (create, delete if empty, etc.).

---

## Architecture

### Frontend
- **React** (using React Router for navigation)
- **Axios** for REST requests
- **SSE** for real-time notifications and AI chunk streaming
- **Material-UI** (MUI) for UI components
- **i18next** for translations (English, Hebrew)

### Backend
- **Spring Boot** (Java) featuring:
    - Spring Security + JWT for user authentication
    - JPA (Hibernate) for DB operations
    - SSE endpoints for real-time updates
- **MySQL** for data storage (user records, questions, notifications, etc.)
- **Ollama** for local LLM calls and SSE partial chunk responses

---

## Installation

1. **Prerequisites**
    - **Node.js** (version ≥ 16)
    - **Java** (JDK 17 or above recommended)
    - **Maven** (or the Maven wrapper)
    - **MySQL** instance running locally or accessible
    - **Ollama** – see [Installing Ollama on PC](#installing-ollama-on-pc) below

2. **Clone the Repository**
   ```bash
   git clone https://github.com/Mish2000/learning-system-client.git
   cd learning-system-client
   ```
   Adjust if your project is split into separate folders for `client` and `server`.

3. **Frontend Setup**
    - Navigate to the front-end directory (e.g., `cd client`).
    - Install dependencies:
      ```bash
      npm install
      ```
    - Start the development server:
      ```bash
      npm run dev
      ```
    - By default, Vite hosts on `http://localhost:5173`.

4. **Backend Setup**
    - Navigate to your Spring Boot project directory (e.g., `cd server`).
    - Configure DB credentials in `application.properties` or as environment variables (see [Configuration](#configuration)).
    - Build or run the application:
      ```bash
      mvn clean install
      mvn spring-boot:run
      ```
    - By default, it starts on `http://localhost:8080`.

### Installing Ollama on PC

1. **System Requirements**
    - Ollama typically requires macOS (for official releases).
    - **For Windows or Linux**: The official Ollama GitHub project provides instructions and builds. Alternatively, you can run Ollama in WSL (Windows Subsystem for Linux) or in Docker.
    - Make sure you have enough RAM to run large LLMs locally.

2. **Download Ollama**
    - Visit the [Ollama GitHub](https://github.com/jmorganca/ollama) or official releases.
    - Choose your platform (macOS / Windows / Linux).
    - Download the latest `.tar.gz` or `.exe`, as appropriate.

3. **Install Ollama**
    - On macOS, you can use Homebrew:
      ```bash
      brew install ollama/tap/ollama
      ```
    - On Windows / Linux, follow the instructions provided on the [Ollama GitHub](https://github.com/jmorganca/ollama). For example:
      ```bash
      # Example for .deb-based Linux distribution
      sudo dpkg -i ollama_<VERSION>_amd64.deb
 
      # Or on Windows, run the .exe installer
      ```
    - Confirm Ollama is installed by running:
      ```bash
      ollama version
      ```

4. **Run Ollama**
    - Start the Ollama service or simply run the CLI.
    - Make sure it listens on the default port `11434` (or update the Spring Boot code to match your custom port).
    - If you want a specific model, you can load it. For example:
      ```bash
      ollama pull llama2:7b
      ```
      Then in your configuration or calls, reference that model name.

5. **Confirm Connection**
    - By default, the server code calls Ollama at `http://localhost:11434/api/generate`.
    - Once Ollama is running, you should see it listening on port `11434`.
    - Use a test call (like `curl http://localhost:11434/api/generate`) to confirm you get a response (you’ll need a proper request body).

Once Ollama is installed and running, the backend code can successfully contact it for local LLM inference.

---

## Configuration

You can configure database credentials and other settings in:
- `src/main/resources/application.properties`, or
- Environment variables.

For example, environment variables:
```bash
export DATABASE_URL=jdbc:mysql://localhost:3306/your_database_name
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=password
```
Then in `application.properties`:
```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```
Adjust to your DB details (username, password, schema name, etc.).

**Ollama** typically runs by default at `localhost:11434`. If you change that port, remember to update references in your Java code, specifically in any place referencing `http://localhost:11434`.

---

## Running the Project

1. **Start Ollama**
    - Run it in a separate terminal, or as a service listening on port `11434`.

2. **Start the Backend**
    - From the server directory:
      ```bash
      mvn spring-boot:run
      ```
    - This launches the Spring Boot server on `http://localhost:8080`. It will attempt to reach Ollama at `http://localhost:11434`.

3. **Start the Frontend**
    - From the client directory:
      ```bash
      npm run dev
      ```
    - Typically on `http://localhost:5173`.

4. **Confirm Connectivity**
    - Open `http://localhost:5173` in your browser.
    - Try logging in or registering a new account.
    - Generate a question, ask for AI solution—requests to the SSE endpoint (`/api/ai/stream2`) should yield partial responses from Ollama.

---

## User Guide

### 1. Registration & Login
- Go to `http://localhost:5173`.
- If you don’t have an account, click **Register**. Supply a username, password, and email.
- After successfully registering, log in with your new credentials.

### 2. Generating a Question
- Once logged in, click **Practice** in the navbar.
- Select a top-level topic (e.g., **Arithmetic** or **Geometry**) and a subtopic (Addition, Circle, etc.).
- Choose a difficulty level (BASIC, EASY, MEDIUM, etc.).
- Click **Generate** to create a new, randomized question.
- Submit an answer and see if it’s correct. You can also expand and view the solution steps.

### 3. Adaptive Difficulty
- The system looks at your last 5 attempts. If you’re doing well, it may increase your difficulty level or lower your sublevel. If you’re struggling, it can decrease your difficulty or raise the sublevel.
- **Notifications** pop up in real-time to inform you of difficulty changes or sublevel changes.

### 4. Notifications & Dashboards
- A **Notifications** icon in the top-right streams messages via SSE (e.g., “Difficulty changed from EASY to MEDIUM”).
- The **User Dashboard** shows personal stats like attempts, success rate by topic, and your current difficulty.
- The **Admin Dashboard** (for admin users) displays global usage metrics, attempts by topic, success rates, and more.

### 5. Admin Topic Management
- If you have an `ADMIN` role, you see a **Manage Topics** or **Admin** link.
- You can add a **new topic** or a subtopic (attached to a parent).
- **Delete** is only possible if the topic has no subtopics (i.e., the parent is empty).

---

## Technology Stack

- **React** (Vite, React Router, MUI, i18next)
- **Spring Boot** (SSE, Security, JWT, JPA)
- **MySQL** (Data persistence)
- **Node.js** (≥16)
- **Maven** (Build + dependency management)
- **Ollama** (Local LLM calls)

---

## Troubleshooting

1. **Port Conflicts**
    - If 8080 or 5173 is in use, change the port in `application.properties` (server) or `vite.config.js` (client).
    - If 11434 is in use or you changed the Ollama port, update references in the server code.

2. **Database Connection Errors**
    - Ensure MySQL is running. Verify the username, password, and DB name in your config.

3. **JWT or 401 Unauthorized**
    - Check that your token is stored in `localStorage` after login. The frontend automatically sends it in the `Authorization` header.

4. **SSE Not Working**
    - SSE can fail if the browser or proxy blocks event streams. Make sure the environment supports SSE. Inspect the console or network tab for errors.

5. **Ollama Fails to Respond**
    - Confirm that Ollama is up and running on the correct port.
    - Try making a direct test call to `http://localhost:11434/api/generate` with a valid JSON body to ensure it’s functioning.

---

## Contributing

1. **Fork** or clone this repository.
2. Create a **feature branch** for your changes.
3. Commit and push your branch, then open a **Pull Request** describing your changes.
4. We welcome feedback, bug fixes, and enhancements!

---

# Dynamic Question Generator Overview

The application includes a class called `QuestionGeneratorService` that dynamically produces new math questions based on several factors:

- **Topic:** Examples include Addition, Subtraction, Fractions, Geometry, etc.
- **Difficulty Level:** Options such as BASIC, EASY, MEDIUM, ADVANCED, and EXPERT.
- **Randomization Logic:** Internal parameters, like random number ranges, influence each question.

Each time a user requests a new question, the system:

1. Looks up the requested topic (if provided).
2. Selects the appropriate generation method (e.g., `createAdditionQuestion`, `createCircleQuestion`, etc.).
3. Randomly selects operands (like `a`, `b`, radius, or side length) within a difficulty-specific range.
4. Builds the question text, computes the answer, and generates a series of solution steps by calling methods from `QuestionAlgorithmsFunctions`.
5. Saves the generated question (represented by the `GeneratedQuestion` entity) to the database.

---

## Topics and Subtopics

The system differentiates between two main categories:

- **Arithmetic Topics:**  
  Topics such as Addition, Subtraction, Multiplication, Division, and Fractions that generate numeric Q&A.

- **Geometry Topics:**  
  Topics such as Rectangle, Triangle, Circle, and Polygon that produce geometry-based problems (e.g., calculating area, perimeter, or circumference).

When a user selects a parent topic (Arithmetic or Geometry) along with a subtopic, the method to generate the question is chosen based on the lowercase version of the topic name. For example, selecting "Triangle" triggers the `createTriangleQuestion` method.

---

## Difficulty Handling

Numeric ranges for random number generation are defined by the selected `DifficultyLevel`. For example:

```java
switch (difficulty) {
    case BASIC -> new int[]{1, 10};
    case EASY -> new int[]{1, 30};
    case MEDIUM -> new int[]{1, 100};
    case ADVANCED -> new int[]{1, 1000};
    case EXPERT -> new int[]{1, 9999};
}
```

- **BASIC:** Generates numbers between 1 and 10.
- **EASY:** Up to 30.
- **MEDIUM:** Up to 100.
- **ADVANCED:** Up to 1,000.
- **EXPERT:** Up to 9,999.

---

## Random Generation of Questions

Each `createXYZQuestion` method in `QuestionGeneratorService`:
1. Randomly picks parameters for the exercise.
2. Constructs a human-readable question string (e.g., "What is 12 ÷ 3?").
3. Uses `QuestionAlgorithmsFunctions` to generate textual “solution steps.”

### Example: `createRectangleQuestion`
```java
int length = randomInRange(1, 20);
int width = randomInRange(1, 20);
int area = length * width;
int perimeter = 2 * (length + width);
String questionText = "...";
String solutionSteps = "...";
return saveQuestion(questionText, solutionSteps, "Area: " + area + ", Perimeter: " + perimeter, topic, difficulty);
```

---

## Complete Solutions

Every generated question stores its final solution steps in `GeneratedQuestion.solutionSteps`. When the user clicks **See Steps**, the UI displays these steps in a visually pleasant format (including math expressions with KaTeX, if needed).

---

## Summary

- **Topic-Aware** question creation (Arithmetic vs. Geometry).
- **Difficulty-Based** numeric ranges.
- **Random** operand selection.
- **Solution-Provided** for each question.

Enjoy exploring the code in `QuestionGeneratorService` and `QuestionAlgorithmsFunctions` to see exactly how each question is formed and solved!

---