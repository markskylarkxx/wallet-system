# Wallet System API

A simple wallet system built with Spring Boot that allows user creation, account funding, and fund transfers.

## Technologies
- Java 21
- Spring Boot 3.5.0
- Spring Data JPA
- H2 Database
- Maven

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+

### Run the Application
```bash
 mvn spring-boot:run


Server starts at http://localhost:9090

H2 Console: http://localhost:9090/h2-console (JDBC URL: jdbc:h2:mem:testdb, username: sa)

API Endpoints
Base URL: /api/v1/wallet

1. Create User
bash
POST /users
{
    "email": "john@example.com"
}
2. Fund Account
bash
POST /fund
{
    "accountNumber": "ACCT1771613856392D7AD",
    "amount": 1000.00
}
3. Transfer Funds
bash
POST /transfer
{
    "fromAccount": "ACCT1771613856392D7AD",
    "toAccount": "ACCT1771614062754AC2B",
    "amount": 300.00
}
Sample cURL Commands
bash
# Create user
curl -X POST http://localhost:9090/api/v1/wallet/users \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com"}'

# Fund account  
curl -X POST http://localhost:9090/api/v1/wallet/fund \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": "ACCT1771613856392D7AD", "amount": 1000}'

# Transfer
curl -X POST http://localhost:9090/api/v1/wallet/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccount": "ACCT1771613856392D7AD", "toAccount": "ACCT1771614062754AC2B", "amount": 300}'
Features
 User registration with auto-generated account number

 Account funding (deposits)

 Fund transfers between accounts

 Thread-safe transactions (pessimistic locking)

 Input validation

 Global exception handling

Assumptions
Email must be unique

Initial balance is zero

Account numbers are auto-generated

All amounts are in the same currency


Configuration
properties
# application.properties
server.port=9090
spring.h2.console.enabled=true
spring.datasource.url=jdbc:h2:mem:testdb
Error Responses
json
{
    "success": false,
    "message": "Error description",
    "timestamp": "2026-02-20T20:20:30.123456"
}
Common errors:

User with email xxx already exists

Account not found: xxx

Insufficient balance. Available: X, Requested: Y

Transfer amount must be greater than zero

Build & Test
bash
mvn clean install
- mvn test

