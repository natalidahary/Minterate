# Minterate: Client-Server Lending Platform

## About

Minterate is a client-server lending platform designed to facilitate lending transactions between borrowers and lenders. It employs a client-server architecture to handle data storage, processing, and communication between users.

## Technologies Used

- **Client-side:** Android Studio, Kotlin, CurrencyLayer API
- **Server-side:** Node.js, Express.js, Firebase Firestore
- **Communication:** RESTful APIs
- **Authentication:** JSON Web Tokens (JWT), Firebase Authentication

## Features

### User Registration and Loan Interaction:
- Users can register on the platform and interact as both borrowers and lenders.
- Lenders can lend money by choosing an amount, period, expiration date, and interest rate.
- Borrowers can filter loans by amount and period to find suitable loan options.

### Loan Listings and Matching:
- Loan listings are displayed on the client interface.
- Matching filteration facilitate the pairing of borrowers with suitable loans based on lenders criteria and preferences.

### Transparent Loan Terms and Interest Rates:
- The platform provides transparent loan terms, including interest rates, repayment schedules, and borrower credit assessments.
- Lenders have access to comprehensive loan information to make informed lending decisions.

### Automated Payment Processing and Loan Servicing:
- The server automates payment processing, including loan disbursements, borrower repayments, and lender returns.
- Loan servicing functions, such as account management, payment reminders, and collections, are handled centrally by the server.

## Installation

To set up Minterate on your local machine, follow these steps:

Instructions for Use:
- **Requirements to Install:** Visual Studio Code for server, and Android Studio for the client.

Pre-Running Steps:
1. Open the Minterate server folder, paste inside the it the .env file : inside the env file - change the SERVICE_ACCOUNT_PATH to the path where my-application-d346d-firebase-adminsdk-ci5a0-f162b5830c.json is located.
2. Open the terminal and run ifconfig to get the IP address.
3. In Android Studio, import MyApplication, then navigate to serverOperations folder -> config.kt, and change the IP address to the extracted one (keep the same port).

Run the Server:
- Via Visual Studio Code terminal, run node index.js.
- Hit enter, and you will see the server running.

Run the Client:
1. Click "build" on Android Studio.
2. If there's an issue with the SDK, click on "sync project with Gradle file" on the top right side of the toolbar.
3. Click "play" and enjoy.
