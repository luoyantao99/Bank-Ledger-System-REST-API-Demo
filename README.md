Bank Ledger System REST API Demo
===============================
## Overview
This is a demo for a simple bank ledger system that utilizes the event sourcing pattern to maintain a transaction history. The system allows users to perform basic banking operations such as depositing funds, withdrawing funds, and checking balances. The ledger maintains a complete and immutable record of all transactions, enabling auditability and reconstruction of account balances at any point in time.

## Details
The API service accepts two types of transactions:
1. **Loads**: Add money to a user (credit)
2. **Authorizations**: Conditionally remove money from a user (debit)

Every load or authorization PUT returns the updated balance following the transaction. Authorization declines are saved, even if they do not impact balance calculation.

The event sourcing pattern was implemented to record all banking transactions as immutable events. Each event captures relevant information such as transaction type, amount, timestamp, and account identifier.

Unit and integrations tests are included.

## Bootstrap Instructions
### Prerequisites:
- Java JDK 1.8 or later
- Maven

### Step 1: Build the Project
First, navigate to the root directory of the project where the `pom.xml` file is located. Then execute the following Maven command to compile the project and create an executable jar file with all dependencies included:
```bash
mvn clean install
```

### Step 2: Run the Server
Once the build is complete, start the server using the generated jar file. Run the following command in the terminal:
```bash
java -jar target/BankLedgerAPI-1.0.0-jar-with-dependencies.jar
```
This command starts the server on `localhost` with port `7000`.

### Step 3: Test Endpoints
With the server running, use the various API endpoints using curl commands. Here are some examples:
#### Load Funds to a User Account:
```bash
curl -X PUT "http://localhost:7000/load?userId=user1&amount=100.00"
```
#### Authorize a Transaction:
```bash
curl -X PUT "http://localhost:7000/authorization?userId=user1&amount=75.00"
```
#### Check the Balance of a User Account:
```bash
curl http://localhost:7000/balance/user1
```
#### Verify the Account Balance Against the Event Log:
```bash
curl http://localhost:7000/verify/user1
```

## Design Considerations
1) Framework: Javalin was chosen for its simplicity and lightweight nature, which makes it ideal for creating microservices that require a minimal setup.

2) Concurrency Management: `ReentrantLock` was used in the "Balance" class. This ensures that balance updates are thread-safe and Load/Authorization operations are executed atomically, preventing potential discrepancies due to concurrent access. 

3) Input Validation: Each endpoint validates input data such as user IDs and transaction amounts. For Load and Authorization operations, non-negative amount and valid numeric values are enforced. 

4) Message ID: Each transaction is uniquely identified using a UUID. 

5) ConcurrentHashMap: The use of a ConcurrentHashMap for storing balances ensures that access to user balances is efficient and thread-safe. 

6) A single "Transaction" class handles both types of financial requests. Each transaction instance carries all the necessary data to process either a load or an authorization request. The class fields include: userId, messageId, transactionAmount, status (APPROVED or DENIED), and serverTime. 

7) Error message strings are currently hardcoded directly in the Java code. In the future, I will refactor the application to use resource files for managing error messages to enhance maintainability. 

## Assumptions
1) The application currently assumes all transactions are processed in USD. 

2) Data persistence is managed in-memory with structures such as ConcurrentHashMap and does not involve interactions with external databases. 

3) It is assumed that the application operates in a semi-trusted environment where users do not intentionally attempt to breach security. As such, security features such as input sanitization against SQL injections and other malicious attacks are not implemented. 

## Future Deployment Considerations
- **Cloud Hosting Providers**
    - AWS: Utilize EC2 instances for hosting the application server, combined with RDS for database management and S3 for backup storage.

- **Containerization**
    - Docker: Containerize the application using Docker to simplify deployment and ensure consistency across different environments. This involves creating a Dockerfile that specifies the Java environment, application dependencies, and deployment instructions.
    - Kubernetes: For higher scalability and management, deploy the Docker containers into a managed Kubernetes cluster. This allows for automated scaling, load balancing, and self-healing. 

- **Continuous Integration and Deployment**
    - GitHub Actions: Automate the build and deployment process using GitHub Actions that handle testing, building, and deploying the application to the cloud environment.
