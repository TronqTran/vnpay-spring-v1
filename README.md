### 🌟 Project Overview

This is a Spring Boot project named "vnpay-spring-v1" that implements VNPay payment gateway functionality. VNPay is a
popular payment gateway service in Vietnam.

### 🏗️ Main Components

1. **Configuration** ⚙️
    - `VnPayConfig`: Likely contains VNPay-specific configuration settings and properties

2. **Controllers** 🎮
    - `VnPayController`: Handles payment-related HTTP requests and endpoints

3. **DTOs (Data Transfer Objects)** 📦
    - `RefundRequest`: Object for handling refund requests
    - `VnPayRequest`: Object for handling payment requests

4. **Services** 🛠️
    - `VnPayService`: Contains business logic for payment processing

5. **Utilities** 🔧
    - `VnPayUtil`: Helper methods for VNPay integration

### 💻 Technical Stack

- ☕ Java 17
- 🍃 Spring Boot
- 🌐 Jakarta EE
- 🔄 Spring MVC
- 🎯 Lombok

### 📁 Project Structure

The project follows standard Spring Boot project structure:

- 📂 `src/main/java`: Contains Java source code
- 📂 `src/main/resources`: Contains configuration files and static resources
- 📂 `src/test`: Contains test cases
- ⚙️ `application.yaml`: Main configuration file