const jwt = require('jsonwebtoken');
const currency = require('./currency');
const util = require('util');
const LoanManagment = require('./loanManagement');
const crypto = require('crypto');
const { extractCurrencyCode, getExchangeRate, getExchangeRateForAllCurrencies } = currency;
const Authentication = require('./authentication');
const Credits = require('./credits');

class UserManagement {

    constructor(db, jwt) {
        this.db = db;
        this.jwt = jwt;
        this.credits = new Credits(db, crypto);
        this.authentication = new Authentication(db, jwt, crypto);
    }
    async signup(req, res) {
        console.log('Received signup request:', req.body);
      
        const hashedCardNumber = this.credits.hashCredit(req.body.credentials.cardNumber);
      
        // Check if the credit card number already exists
        const hashedCardNumberExists = await this.credits.checkHashedCardNumberExists(hashedCardNumber);
      
        try {
          if (hashedCardNumberExists) {
            res.status(400).send('Credit card number already exists');
          } else {
            // Continue with the signup logic
            const newUser = {
              email: req.body.email,
              password: this.authentication.hashPassword(req.body.password),
              mobile: req.body.mobile,
              personalInfo: {
                firstName: req.body.personalInfo.firstName,
                lastName: req.body.personalInfo.lastName,
                dob: req.body.personalInfo.dob,
                id: req.body.personalInfo.id,
                address: req.body.personalInfo.address,
                city: req.body.personalInfo.city,
                state: req.body.personalInfo.state,
              },
              credentials: {
                lastFourDigits: req.body.credentials.lastFourDigits,
                cardNumber: hashedCardNumber, // Hash the card number during signup
                monthYear: req.body.credentials.monthYear,
                cvv: req.body.credentials.cvv,
              },
              totalBalance: req.body.totalBalance,
              textScalar: req.body.textScalar,
              sounds: req.body.sounds || true,
              currency: req.body.currency,
            };
      
            // Check if user with the same email or mobile already exists
            const userRefByEmail = this.db.collection('users').doc(newUser.email);
            const userRefByMobile = this.db.collection('users').where('mobile', '==', newUser.mobile);
      
            const docByEmail = await userRefByEmail.get();
            const querySnapshotByMobile = await userRefByMobile.get();
      
            if (docByEmail.exists || !querySnapshotByMobile.empty) {
              res.status(400).send('User already exists');
            } else {
              // Create a new user
              await userRefByEmail.set(newUser);
              res.status(200).send('User created successfully');
            }
          }
        } catch (error) {
          console.error('Error checking user or creating user:', error.message);
          res.status(500).send('Internal Server Error');
        }
    }

    // Endpoint to check if an email exists
    async checkEmail (req, res) {
        console.log('Received check-email request:', req.body);
        const emailToCheck = req.body.email;
    
        // Check if user with the provided email already exists
        const userRef = this.db.collection('users').doc(emailToCheck);
    
        try {
        const doc = await userRef.get();
        if (doc.exists) {
            // Email already exists
            res.status(200).json({ exists: true });
        } else {
            // Email does not exist
            res.status(200).json({ exists: false });
        }
        } catch (error) {
        console.error('Error checking email:', error.message);
        res.status(500).send('Internal Server Error');
        }
  }

  async checkMobile (req, res) {
    console.log('Received check-mobile request:', req.body);
    const mobileToCheck = req.body.mobile;
  
    // Check if user with the provided mobile number already exists
    const userRef = this.db.collection('users').where('mobile', '==', mobileToCheck);
  
    try {
      const querySnapshot = await userRef.get();
      if (!querySnapshot.empty) {
        // Mobile number already exists
        res.status(200).json({ exists: true });
      } else {
        // Mobile number does not exist
        res.status(200).json({ exists: false });
      }
    } catch (error) {
      console.error('Error checking mobile number:', error.message);
      res.status(500).json({ exists: false, error: 'Internal Server Error' });
    }
  }
  
  async checkId (req, res) {
    console.log('Received check-id request:', req.body);
    const idToCheck = req.body.id;
  
    // Check if user with the provided ID already exists
    const userRefById = this.db.collection('users').where('personalInfo.id', '==', idToCheck);
  
    try {
      const querySnapshotById = await userRefById.get();
      if (!querySnapshotById.empty) {
        // ID already exists
        res.status(200).json({ exists: true });
      } else {
        // ID does not exist
        res.status(200).json({ exists: false });
      }
    } catch (error) {
      console.error('Error checking ID:', error.message);
      res.status(500).json({ exists: false, error: 'Internal Server Error' });
    }
  }



  async checkCredit (req, res) {
    console.log('Received check-credit request:', req.body);
    const cardNumberToCheck = req.body.cardNumber;
    const monthYearToCheck = req.body.monthYear;
    const cvvToCheck = req.body.cvv;
  
    try {
      // Check if the provided credit card number exists in verificationRecords
      
      const internalCardCheck = cardNumberToCheck.replace(/\s/g, '')
      console.log('credit with hash', internalCardCheck);
      const verificationRecordRef = this.db.collection('verificationRecords')
        .where('credentials.cardNumber', '==', internalCardCheck)
        .where('credentials.cvv', '==', cvvToCheck)
        .where('credentials.monthYear', '==', monthYearToCheck);
  
      const verificationRecordSnapshot = await verificationRecordRef.get();
  
      if (verificationRecordSnapshot.empty) {
        // Credit card details do not match in verificationRecords
        res.status(404).send('Credit card not recognized');
        return;
      }
  
      // Your existing code here
      const hashedCardNumberExists = await this.credits.checkHashedCardNumberExists(this.credits.hashCredit(cardNumberToCheck));
  
      if (hashedCardNumberExists) {
        // Credit card number is verified but already exists in other user
        res.status(409).send('Credit card number already exists');
      } else {
        // Credit card number is verified and does not exist in other users
        res.status(200).json({ exists: true });
      }
    } catch (error) {
      console.error('Error checking credit card number:', error.message);
      res.status(500).send('Internal Server Error');
    }
  }

  // Update user data with new address
  async updateUserAddress(req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
        return res.status(400).send('Token parameter is missing');
    }
  
    const newAddress = req.body.address;
    const newCity = req.body.city;
    const newState = req.body.state;
  
    console.log('Received request to update user address:', userToken);
    console.log('New Address:', newAddress);
    console.log('New City:', newCity);
    console.log('New State:', newState);
  
    if (!newAddress || !newCity || !newState) {
        return res.status(400).send('Missing required parameters');
    }
  
    try {
        // Decode the token to get user email
        const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
  
        const userEmail = decodedToken.email;
  
        const userRef = this.db.collection('users').doc(userEmail);
  
        const userSnapshot = await userRef.get();
  
        if (!userSnapshot.exists) {
            console.log('User not found for email:', userEmail);
            return res.status(404).send('User not found');
        }
  
        // Update user data with new address
        await userRef.update({
            'personalInfo.address': newAddress,
            'personalInfo.city': newCity,
            'personalInfo.state': newState,
        });
  
        console.log('User address updated successfully for email:', userEmail);
        res.status(200).send('User address updated successfully');
    } catch (error) {
        console.error('Error updating user address:', error.message);
  
        if (error.code === 'PERMISSION_DENIED') {
            res.status(403).send('Permission denied. User not authorized to perform this action.');
        } else {
            res.status(500).send('Internal Server Error');
        }
    }
  }
  
  async updateUserCredit (req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const newLastFourDigits = req.body.lastFourDigits;
    const newCardNumber = req.body.cardNumber;
    const newMonthYear = req.body.monthYear;
    const newCvv = req.body.cvv;
  
    console.log('Received request to update user credit information:', userToken);
    console.log('New LastFourDigits:', newLastFourDigits);
    console.log('New CardNumber:', newCardNumber);
    console.log('New MonthYear:', newMonthYear);
    console.log('New Cvv:', newCvv);
  
    if (!newLastFourDigits || !newCardNumber || !newMonthYear || !newCvv) {
      return res.status(400).send('Missing required parameters');
    }
  
    try {
      // Decode the token to get user email
      const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
  
      const userEmail = decodedToken.email;
  
      const userRef = this.db.collection('users').doc(userEmail);
  
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
  
      // Check if the new credit card number already exists
      const hashedCardNumber = this.credits.hashCredit(newCardNumber);
      const hashedCardNumberExists = await this.credits.checkHashedCardNumberExists(hashedCardNumber);
  
      if (hashedCardNumberExists) {
        return res.status(400).send('New credit card number already exists');
      }
  
      // Hash the new credit card information
      const hashedNewCardNumber = this.credits.hashCredit(newCardNumber);
  
      // Update user data with hashed credit card information
      await userRef.update({
        'credentials.lastFourDigits': newLastFourDigits,
        'credentials.cardNumber': hashedNewCardNumber,
        'credentials.monthYear': newMonthYear,
        'credentials.cvv': newCvv,
      });
  
      console.log('User credit information updated successfully for email:', userEmail);
      res.status(200).send('User credit information updated successfully');
    } catch (error) {
      console.error('Error updating user credit information:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }
  
  async updatePassword (req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    try {
      // Decode the token to get user email
      const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
      const userEmail = decodedToken.email;
  
      const newPassword = req.body.newPassword;
      const oldPassword = req.body.oldPassword;
  
      console.log('Received request to update user password:', userEmail);
  
      if (!newPassword || !oldPassword) {
        return res.status(400).send('Missing required parameters');
      }
  
      const userRef = this.db.collection('users').doc(userEmail);
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
  
      // Check if the provided old password matches the stored password
      const oldPasswordHash = this.authentication.hashPassword(oldPassword);
  
      if (oldPasswordHash !== userSnapshot.data().password) {
        console.log('Incorrect old password for email:', userEmail);
        return res.status(401).send('Incorrect old password');
      }
  
      // Hash the new password
      const hashedNewPassword = this.authentication.hashPassword(newPassword);
  
      // Update user data with hashed new password
      await userRef.update({
        'password': hashedNewPassword,
      });
  
      console.log('User password updated successfully for email:', userEmail);
      res.status(200).send('User password updated successfully');
    } catch (error) {
      console.error('Error updating user password:', error.message);
      if (error.name === 'JsonWebTokenError') {
        res.status(401).send('Invalid token');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }

  async updateMobile (req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const userEmail = req.userEmail;
    const newMobile = req.body.newMobile;
  
    console.log('Received request to update user mobile:', userEmail);
    console.log('New Mobile:', newMobile);
  
    if (!newMobile) {
      return res.status(400).send('Missing required parameters');
    }
  
    try {
      // Decode the token to get user email
      const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
  
      const userEmail = decodedToken.email;
  
      const userRef = this.db.collection('users').doc(userEmail);
  
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
  
      // Update user data with new mobile
      await userRef.update({
        'mobile': newMobile,
      });
  
      console.log('User mobile updated successfully for email:', userEmail);
      res.status(200).send('User mobile updated successfully');
    } catch (error) {
      console.error('Error updating user mobile:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }


  async updateUserCurrency(req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const userEmail = req.userEmail || jwt.verify(userToken, 'your-secret-key').email;
  
    const newCurrency = req.body.currency;
  
    console.log('Received request to update user currency:', userEmail);
    console.log('New Currency:', newCurrency);
  
    if (!newCurrency) {
      return res.status(400).send('Missing required parameters');
    }
  
    try {
      const userRef = this.db.collection('users').doc(userEmail);
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
  
      // Use userRef directly as the document reference
      const userDocRef = userRef;
  
      const userData = (await userDocRef.get()).data();
  
      const oldCurrencyCode = extractCurrencyCode(userData.currency);
      const newCurrencyCode = extractCurrencyCode(newCurrency);
  
      console.log('Checking exchange rate for:', oldCurrencyCode, newCurrencyCode);
      const exchangeRate = await getExchangeRate(oldCurrencyCode, newCurrencyCode);
  
      const newUserBalance = userData.totalBalance * exchangeRate;
  
      // Update user data with new currency
      await userRef.update({
        currency: newCurrency,
        totalBalance: newUserBalance,
      });
  
      console.log('User currency updated successfully for email:', userEmail);
      res.status(200).send('User currency updated successfully');
    } catch (error) {
      console.error('Error updating user currency:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }

  async getILSToUserCurrencyExchangeRate(req, res) {
    const userToken = req.query.token;

    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }

    const userEmail = req.userEmail || jwt.verify(userToken, 'your-secret-key').email;

    console.log('Received request to get exchange rate for 30,000 ILS to user currency:', userEmail);

    try {
      const userRef = this.db.collection('users').doc(userEmail);
      const userSnapshot = await userRef.get();

      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }

      const userData = userSnapshot.data();

      // Get the currency code of the user's currency
      const userCurrencyCode = extractCurrencyCode(userData.currency);

      console.log('Checking exchange rate for 30,000 ILS to', userCurrencyCode);

      let equivalentAmount = 0;

      // If user's currency is ILS, no exchange is needed, set equivalentAmount to 30000
      if (userCurrencyCode === 'ILS') {
        equivalentAmount = 30000;
      } else {
        // Get the exchange rate for 30,000 ILS to the user's currency
        const exchangeRate = await getExchangeRate('ILS', userCurrencyCode);
        
        // Calculate the equivalent amount in the user's currency for 30,000 ILS
        equivalentAmount = Math.round(30000 * exchangeRate); // Round the equivalent amount
      }

      console.log('Equivalent amount in', userCurrencyCode, 'is', equivalentAmount);

      res.status(200).json({ equivalentAmount: equivalentAmount });
    } catch (error) {
      console.error('Error getting exchange rate:', error.message);

      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }
  
  async updateUserTextScalar(req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const userEmail = req.userEmail;
    const newTextScalar = req.body.textScalar;
  
    console.log('Received request to update user textScalar:', userEmail);
    console.log('New textScalar:', newTextScalar);
  
    if (newTextScalar === undefined) {
      return res.status(400).send('Missing required parameters');
    }
  
    try {
      // Decode the token to get user email
      const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
  
      const userEmail = decodedToken.email;
  
      const userRef = this.db.collection('users').doc(userEmail);
  
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
  
      // Update user data with new textScalar
      await userRef.update({
        textScalar: newTextScalar,
      });
  
      console.log('User textScalar updated successfully for email:', userEmail);
      res.status(200).send('User textScalar updated successfully');
    } catch (error) {
      console.error('Error updating user textScalar:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }
  
  async updateSoundSettings(req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const userEmail = req.userEmail;
    const newSoundsEnabled = req.body.soundsEnabled;
  
    console.log('Received request to update user sound settings:', userEmail);
    console.log('New Sounds Enabled:', newSoundsEnabled);
  
    if (newSoundsEnabled === undefined) {
      return res.status(400).send('Missing required parameters');
    }
  
    try {
      // Decode the token to get user email
      const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
  
      const userEmail = decodedToken.email;
  
      const userRef = this.db.collection('users').doc(userEmail);
  
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
  
      // Update user data with new sound settings
      await userRef.update({
        sounds: newSoundsEnabled,
      });
  
      console.log('User sound settings updated successfully for email:', userEmail);
      res.status(200).send('User sound settings updated successfully');
    } catch (error) {
      console.error('Error updating user sound settings:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }


async getUserLoans (req, res) {
    const userToken = req.query.token;
    console.log('Received request for getUserLoans');
  
    if (!userToken) {
        console.log('Error: Missing user token');
        return res.status(400).send('Missing user token');
    }
  
    try {
        console.log('Decoding token...');
        const decodedToken = jwt.verify(userToken, 'your-secret-key');
        const userEmail = decodedToken.email;
        console.log('Decoded Token:', decodedToken);
  
        if (!userEmail) {
            console.log('Error: User email missing in token');
            return res.status(400).send('User email missing in token');
        }
  
        // Retrieve user document based on email
        console.log('Retrieving user document based on email:', userEmail);
        const userQuery = await this.db.collection('users').where('email', '==', userEmail).get();
        if (userQuery.empty) {
            console.log('Error: User not found');
            return res.status(404).send('User not found');
        }
  
        // Assuming each user document's ID is the user ID
        const userId = userQuery.docs[0].id;
  
        // Fetch loans from the user's subcollection
        console.log('Fetching loans for user ID:', userId);
        const loansSnapshot = await this.db.collection('users').doc(userId).collection('loans').get();
  
        let loans = [];
        loansSnapshot.forEach(doc => loans.push(doc.data()));
  
        console.log(`Found ${loans.length} loans for user`);
        res.status(200).json(loans);
    } catch (error) {
        console.error('Error fetching user loans:', error.message);
        res.status(500).send('Internal Server Error');
    }
  }


async deleteLoan (req, res) {
    const { userToken, loanId } = req.query;
  
    if (!userToken) {
        console.log('Token missing for delete request');
        return res.status(400).send('Token is required');
    }
  
    if (!loanId) {
        console.log('Loan ID missing for delete request');
        return res.status(400).send('Loan ID is required');
    }
  
    try {
        // Decode the token to get user email or ID
        const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace with your JWT secret
        const userEmail = decodedToken.email; // Assuming the token contains email or userId
        console.log(`User identified for deletion: ${userEmail}`);
  
        // Retrieve the loan data
        const loanRef = this.db.collection('loans').doc(loanId);
        const loanSnapshot = await loanRef.get();
  
        if (!loanSnapshot.exists) {
            console.log('Loan not found for deletion:', loanId);
            return res.status(404).send('Loan not found');
        }
  
        // Delete the loan document
        await loanRef.delete();
        console.log(`Loan ${loanId} deleted successfully`);
  
        // Optional: Delete the loan from the user's subcollection if exists
        const userLoansRef = this.db.collection('users').doc(userEmail).collection('loans').doc(loanId);
        await userLoansRef.delete();
        console.log(`Loan ${loanId} removed from user's ${userEmail} subcollection`);
  
        res.status(200).json({ message: 'Loan deleted successfully' });
    } catch (error) {
        console.error('Error deleting loan:', error.message);
        res.status(500).send('Internal Server Error');
    }
  }


  async getUserTransactions(req, res) {
    const userToken = req.query.token;
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    try {
      const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
      const userEmail = decodedToken.email;
      console.log('User email:', userEmail);
  
      // Retrieve the user ID based on the email
      const userSnapshot = await this.db.collection('users').where('email', '==', userEmail).get();
  
      if (userSnapshot.empty) {
        console.log('User not found');
        return res.status(404).send('User not found');
      }
  
      const userId = userSnapshot.docs[0].data().personalInfo.id;
  
      // Retrieve all transactions from the user's loans
      const transactions = [];
  
      const loansSnapshot = await this.db.collection('users').doc(userEmail).collection('loans').get();
  
      for (const loanDoc of loansSnapshot.docs) {
        const loanId = loanDoc.id;
  
        // Fetch transactions from the 'transactions' subcollection within each loan
        const loanTransactionsSnapshot = await loanDoc.ref.collection('transactions').get();
  
        for (const transactionDoc of loanTransactionsSnapshot.docs) {
          transactions.push(transactionDoc.data());
        }
      }
  
      if (transactions.length === 0) {
        console.log('No transactions found for the user');
        return res.status(404).send('No transactions found for the user');
      }
  
      console.log('Retrieved transactions:', transactions);
      res.status(200).json(transactions);
    } catch (error) {
      console.error('Error retrieving user transactions:', error.message);
      res.status(500).send('Internal Server Error');
    }
  }





async checkUserActiveLoans(req, res) {
    const userToken = req.query.token;
    console.log('Received checkUserActiveLoans request, Token:', userToken);
  
    if (!userToken) {
      console.log('Error: Token not provided');
      return res.status(400).send('Token not provided');
    }
  
    try {
      console.log('Verifying token...');
      const decodedToken = jwt.verify(userToken, 'your-secret-key');
      const userEmail = decodedToken.email; // Extract email from token
  
      if (!userEmail) {
        console.log('Error: Decoded token does not contain email');
        return res.status(400).send('Invalid token');
      }
  
      // Fetch user ID based on email
      const userSnapshot = await this.db.collection('users').doc(userEmail).get();
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
      const userId = userSnapshot.data().personalInfo.id;
  
      console.log('Token verified, Decoded userId:', userId);
      console.log('Querying database for active loans...');
      
      // Check for active loans as a borrower or lender
      const borrowerLoansSnapshot = await this.db.collection('loans').where('borrowerId', '==', userId).where('status', '==', 'ACTIVE').get();
      const lenderLoansSnapshot = await this.db.collection('loans').where('lenderId', '==', userId).where('status', '==', 'ACTIVE').get();
  
      if (!borrowerLoansSnapshot.empty || !lenderLoansSnapshot.empty) {
        console.log('User has active loans:', userId);
        res.status(400).json({ message: 'User has active loans' });
      } else {
        console.log('No active loans for user:', userId);
        res.status(200).json({ message: 'No active loans' });
      }
    } catch (error) {
      console.error('Error in checkUserActiveLoans:', error);
      res.status(500).send('Internal Server Error');
    }
  }
  
  
  async deleteUser(req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
        console.log('Error: Token not provided');
        return res.status(400).send('Token not provided');
    }
  
    try {
        const decodedToken = jwt.verify(userToken, 'your-secret-key');
        const userEmail = decodedToken.email;
  
        if (!userEmail) {
            console.log('Error: Decoded token does not contain email');
            return res.status(400).send('Invalid token');
        }
  
        // Fetch user ID based on email
        const userSnapshot = await this.db.collection('users').doc(userEmail).get();
        if (!userSnapshot.exists) {
            console.log('User not found for email:', userEmail);
            return res.status(404).send('User not found');
        }
        const userId = userSnapshot.data().personalInfo.id;
  
        // Check for active loans as a borrower or lender
        const borrowerLoansSnapshot = await this.db.collection('loans').where('borrowerId', '==', userId).where('status', '==', 'ACTIVE').get();
        const lenderLoansSnapshot = await this.db.collection('loans').where('lenderId', '==', userId).where('status', '==', 'ACTIVE').get();
  
        if (!borrowerLoansSnapshot.empty || !lenderLoansSnapshot.empty) {
            return res.status(400).send('User cannot be deleted due to active loans');
        }
  
        // Delete user's loans subcollection
        const userLoansSnapshot = await this.db.collection('users').doc(userEmail).collection('loans').get();
        userLoansSnapshot.forEach(doc => doc.ref.delete());
  
        // Delete user's loans from global loans collection (as borrower and lender)
        const loansToDeleteSnapshot = await this.db.collection('loans').where('borrowerId', '==', userId).get();
        loansToDeleteSnapshot.forEach(doc => doc.ref.delete());
        const lenderLoansToDeleteSnapshot = await this.db.collection('loans').where('lenderId', '==', userId).get();
        lenderLoansToDeleteSnapshot.forEach(doc => doc.ref.delete());
  
        // Delete user's transactions from transactions collection (as origin and destination)
        const originTransactionsToDeleteSnapshot = await this.db.collection('transactions').where('origin', '==', userId).get();
        originTransactionsToDeleteSnapshot.forEach(doc => doc.ref.delete());
        const destinationTransactionsToDeleteSnapshot = await this.db.collection('transactions').where('destination', '==', userId).get();
        destinationTransactionsToDeleteSnapshot.forEach(doc => doc.ref.delete());
  
        // Finally, delete the user document
        await this.db.collection('users').doc(userEmail).delete();
  
        res.status(200).json({ message: 'User deleted successfully' });
    } catch (error) {
        console.error('Error deleting user:', error);
        res.status(500).send('Internal Server Error');
    }
  }

async getUserNameById (req, res){
    const userId = req.query.userId;
    console.log("Received request for getUserNameById with userId:", userId);
  
    if (!userId) {
        console.log("User ID parameter is missing in the request");
        return res.status(400).send('User ID is required');
    }
  
    try {
        // Scan through each document in the users collection
        const usersSnapshot = await this.db.collection('users').get();
        let userFound = false;
        let userData;
  
        usersSnapshot.forEach(doc => {
            if (doc.data().personalInfo && doc.data().personalInfo.id === userId) {
                userFound = true;
                userData = doc.data();
            }
        });
  
        if (!userFound) {
            console.log(`User not found for ID: ${userId}`);
            return res.status(404).send('User not found');
        }
  
        console.log(`Retrieved user data for ID: ${userId}`, userData);
        res.status(200).json({
            firstName: userData.personalInfo.firstName,
            lastName: userData.personalInfo.lastName
            // ... other user details you want to include
        });
  
    } catch (error) {
        console.error('Error retrieving user data for ID:', userId, 'Error:', error.message);
        res.status(500).send('Internal Server Error');
    }
  }  

}
  

module.exports = UserManagement;