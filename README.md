# Quick Math Learning System

A self-paced learning platform for mathematics, offering dynamic question generation, automatic difficulty adaptation, user analytics, and more.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Project](#running-the-project)
- [User Guide](#user-guide)
- [Technology Stack](#technology-stack)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

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
- **SSE** for real-time notifications
- **Material-UI** (MUI) for UI components
- **i18next** for translations (English, Hebrew)

### Backend
- **Spring Boot** (Java) featuring:
    - Spring Security + JWT for user authentication
    - JPA (Hibernate) for DB operations
    - SSE endpoints for real-time updates
- **MySQL** for data storage (user records, questions, notifications, etc.)

---

## Installation

1. **Prerequisites**
    - **Node.js** (version ≥ 16)
    - **Java** (JDK 17 or above recommended)
    - **Maven** (or the Maven wrapper)
    - A running **MySQL** instance

2. **Clone the Repository**
   ```bash
   git clone https://github.com/Mish2000/learning-system-server.git
   cd learning-system-server

(Adjust paths if your project separates `client` and `server` into subfolders.)

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

---

## Running the Project

1. **Start the Backend**  
   From the server directory:
   ```bash
   mvn spring-boot:run
   ```
   This launches the Spring Boot server on `http://localhost:8080`.

2. **Start the Frontend**  
   From the client directory:
   ```bash
   npm run dev
   ```
   This typically launches the React app on `http://localhost:5173`.

3. **Confirm Connectivity**
    - Open `http://localhost:5173` in your browser.
    - Try logging in or registering a new account.

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
- If you have `ADMIN` role, you see a **Manage Topics** or **Admin** link.
- You can add a **new topic** or a subtopic (attached to a parent).
- **Delete** is only possible if the topic has no subtopics (i.e., the parent is empty).

---

## Technology Stack

- **React** (Vite, React Router, MUI, i18next)
- **Spring Boot** (SSE, Security, JWT, JPA)
- **MySQL** (Data persistence)
- **Node.js** (≥16)
- **Maven** (Build + dependency management)

---

## Troubleshooting

1. **Port Conflicts**
    - If 8080 or 5173 is in use, change the port in `application.properties` (server) or `vite.config.js` (client).

2. **Database Connection Errors**
    - Ensure MySQL is running. Verify the username, password, and DB name in your config.

3. **JWT or 401 Unauthorized**
    - Check that your token is stored in `localStorage` after login. The frontend automatically sends it in the `Authorization` header.

4. **SSE Not Working**
    - SSE can fail if the browser or proxy blocks event streams. Make sure the environment supports SSE. Inspect the console or network tab for errors.

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
- Looks up the requested topic (if provided).
- Selects the appropriate generation method (e.g., `createAdditionQuestion`, `createCircleQuestion`, etc.).
- Randomly selects operands (like `a`, `b`, radius, or side length) within a difficulty-specific range.
- Builds the question text, computes the answer, and generates a series of solution steps by calling methods from `QuestionAlgorithmsFunctions`.
- Saves the generated question (represented by the `GeneratedQuestion` entity) to the database.

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
    case EASY -> new int[]{1, 30};
    case MEDIUM -> new int[]{1, 100};
    case ADVANCED -> new int[]{1, 1000};
    default -> new int[]{1, 10};  // BASIC
}
```

This means:
- **BASIC:** Generates numbers between 1 and 10.
- **MEDIUM:** Generates numbers up to 100.
- **ADVANCED:** Generates numbers up to 1000.
- (Other levels follow similar logic.)

---

## Random Generation of Questions

Depending on the method called, the process varies by question type. Here are a couple of examples:

### Example: `createAdditionQuestion`

- **Operands:**  
  Two integers, `a` and `b`, are randomly chosen within the range defined by the current difficulty.
- **Question Text:**  
  Formatted as "`{a} + {b} = ?`".
- **Answer:**  
  The correct answer is simply `a + b`.
- **Solution Steps:**  
  Generated by calling `QuestionAlgorithmsFunctions.simplifyAddition(a, b, answer)`.

### Example: `createCircleQuestion`

- **Parameter:**  
  A random radius is selected (e.g., from 1 to 10).
- **Calculations:**
    - **Area:** Computed as π * r².
    - **Circumference:** Computed as 2 * π * r (with π approximated as 3.14).
- **Question Text:**  
  "Circle with radius {r}. Find its area and circumference."
- **Answer:**  
  For instance, "Area: 78.50, Circumference: 31.40".
- **Solution Steps:**  
  A detailed, textual explanation is generated to describe how to compute the area and circumference, using methods from `QuestionAlgorithmsFunctions`.

Each `createXYZQuestion(...)` method follows a similar pattern:
- Randomly picks parameters for the exercise (numbers, shapes, etc.).
- Constructs a human-readable question string (including necessary instructions, e.g., "Given length = X and width = Y").
- Calls a solution-simplifying function from `QuestionAlgorithmsFunctions` to produce a step-by-step solution.

---

## Complete Solutions

Every generated question stores its final solution steps in the `GeneratedQuestion.solutionSteps` field. This ensures that when a user opts to view the solution, the UI can display a comprehensive, step-by-step explanation.

- **Arithmetic Operations:**  
  Functions such as `simplifyAddition`, `simplifySubtraction`, etc., return a series of text lines that detail the fundamental approach.
- **Geometry Problems:**  
  The solution steps explain the calculations for area, perimeter, or other properties, step by step.

---

## Summary

In summary, the question generator is:

- **Topic-Aware:** Distinguishes between Arithmetic and Geometry topics.
- **Difficulty-Based:** Uses difficulty-specific ranges for generating random numbers.
- **Random:** Provides different operand values with each new question.
- **Solution-Provided:** Calls dedicated logic to produce a detailed textual explanation.

This modular design, with one generation method per topic, ensures clear organization and leverages a single `QuestionRepository` table for storing generated questions.

---