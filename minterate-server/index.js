const express = require('express');
const admin = require('firebase-admin');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const exec = require('child_process').exec;
const cron = require('node-cron');
const util = require('util');
const currency = require('./currency');
const Authentication = require('./authentication');
const Credits = require('./credits');
const UserManagement = require('./userManagement');
const LoanManagement  = require('./loanManagement');
const TransactionManagement = require('./transactionManagement');
require('dotenv').config();

const serviceAccountPath = process.env.SERVICE_ACCOUNT_PATH;
const databaseUrl = process.env.DATABASE_URL;

// Initialize Firebase Admin SDK
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: databaseUrl,
});

const app = express();
const db = admin.firestore();
const authentication = new Authentication(db, jwt, crypto); // Pass your database instance to the class constructor
const credits = new Credits(db, crypto);
const userManagement = new UserManagement(db, jwt);
const loanManagement = new LoanManagement(db, jwt, admin);
const transactionManagement = new TransactionManagement(db, jwt);


app.use(express.json());

transactionManagement.scheduleDailyLoanCheck();


(async () => {
  try {
    // Example usage: insert card details from within the server code
    const cardDetailsToInsert = {
      credentials: {
        lastFourDigits: '7777',
        cardNumber: '1111222233337777',
        monthYear: '03/28',
        cvv: '163',
      },
      // Add other details as needed
    };
  
    await credits.insertCardDetails(cardDetailsToInsert);
  } catch (error) {
    console.error('Error inserting card details:', error);
    // Handle error appropriately
  }
})();

// Define routes

app.post('/login', async (req, res) => {
  await authentication.login(req, res);
});

app.get('/getUserDataByToken', async (req, res) => {
  await authentication.getUserDataByToken(req, res);
});

app.put('/changePasswordLogin', async (req, res) => {
  await authentication.changePasswordLogin(req, res);
});

app.get('/getMobileForEmail', async (req, res) => {
  await authentication.getMobileForEmail(req, res);
});

app.post('/signup', async (req, res) => {
  await userManagement.signup(req, res);
});

app.post('/check-email', async (req, res) => {
  await userManagement.checkEmail(req, res);
});

app.post('/check-mobile', async (req, res) => {
  await userManagement.checkMobile(req, res);
});

app.post('/check-id', async (req, res) => {
  await userManagement.checkId(req, res);
});

app.post('/check-credit', async (req, res) => {
  await userManagement.checkCredit(req, res);
});

app.put('/updateUserAddress', async (req, res) => {
  await userManagement.updateUserAddress(req, res);
});

app.put('/updateUserCredit', async (req, res) => {
  await userManagement.updateUserCredit(req, res);
});

app.put('/updatePassword', async (req, res) => {
  await userManagement.updatePassword(req, res);
});

app.put('/updateMobile', async (req, res) => {
  await userManagement.updateMobile(req, res);
});

app.put('/updateUserCurrency', async (req, res) => {
  await userManagement.updateUserCurrency(req, res);
});

app.put('/updateUserTextScalar', async (req, res) => {
  await userManagement.updateUserTextScalar(req, res);
});

app.put('/updateSoundSettings', async (req, res) => {
  await userManagement.updateSoundSettings(req, res);
});

app.get('/getUserLoans', async (req, res) => {
  await userManagement.getUserLoans(req, res);
});

app.get('/getILSToUserCurrencyExchangeRate', async (req, res) => {
  await userManagement.getILSToUserCurrencyExchangeRate(req, res);
});

app.delete('/deleteLoan', async (req, res) => {
  await userManagement.deleteLoan(req, res);
});

app.get('/getUserTransactions', async (req, res) => {
  await userManagement.getUserTransactions(req, res);
});

app.get('/checkUserActiveLoans', async (req, res) => {
  await userManagement.checkUserActiveLoans(req, res);
});

app.delete('/deleteUser', async (req, res) => {
  await userManagement.deleteUser(req, res);
});

app.get('/getUserNameById', async (req, res) => {
  await userManagement.getUserNameById(req, res);
});

app.post('/saveLoan', async (req, res) => {
  await loanManagement.saveLoan(req, res);
});

app.post('/updateAndAddLoan', async (req, res) => {
  await loanManagement.updateAndAddLoan(req, res);
});

app.get('/getLoans', async (req, res) => {
  await loanManagement.getLoans(req, res);
});

app.post('/lockLoan', async (req, res) => {
  await loanManagement.lockLoan(req, res);
});

app.post('/unlockLoan', async (req, res) => {
  await loanManagement.unlockLoan(req, res);
});

app.get('/getLoanLockStatus/:loanId', async (req, res) => {
  await loanManagement.getLoanLockStatus(req, res);
});

app.post('/approveLoanByLock', async (req, res) => {
  await loanManagement.approveLoanByLock(req, res);
});

app.post('/completeLoanRepayment', async (req, res) => {
  await transactionManagement.completeLoanRepayment(req, res);
});

app.post('/recordTransaction', async (req, res) => {
  await transactionManagement.recordTransaction(req, res);
});


const PORT = process.env.PORT || 6668;

function startServer() {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on port ${PORT}..`);
  });
}

killProcessOnPort(PORT);


function killProcessOnPort(port) {
  // Command to find a process using the given port on Unix-like systems
  const command = `lsof -i :${port} | awk 'NR!=1 {print $2}' | xargs kill -9`;

  exec(command, (err, stdout, stderr) => {
    if (err) {
      console.error(`Error: ${err}`);
      return;
    }

    if (stderr) {
      console.error(`Error: ${stderr}`);
      return;
    }

    console.log(`Process on port ${port} has been terminated`);
    startServer(); // Start your server after the port has been freed
  });
}

module.exports = {
  db: db
  // Other exports if any
};