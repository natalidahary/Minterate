const jwt = require('jsonwebtoken');
const index = require('./index');
const credits = require('./credits');
const util = require('util');
const { getExchangeRate, extractCurrencyCode , getExchangeRateForAllCurrencies} = require('./currency');


class LoanManagement {
    constructor(db, jwt, admin) {
        this.db = db;
        this.jwt = jwt;
        this.admin = admin;
    }


async saveLoan(req, res) {
    const userToken = req.body.userToken;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const loanData = req.body.loanData;
  
    if (!loanData) {
      return res.status(400).send('Missing required loan data');
    }
  
    try {
      // Decode the token to get user email
      const decodedToken = this.jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
  
      const userEmail = decodedToken.email;
  
      const userRef = this.db.collection('users').doc(userEmail);
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        return res.status(404).send('User not found');
      }
  
      const newLoanRef = this.db.collection('loans').doc();
      const loanWithId = { ...loanData, lId: newLoanRef.id, borrowerId: null };
  
      await newLoanRef.set(loanWithId);
  
      // Here, we ensure the loan data, including the HTML contract, is saved under the user's loans subcollection
      await userRef.collection('loans').doc(newLoanRef.id).set(loanWithId);
  
      res.status(200).json({ message: 'Loan saved successfully', lId: newLoanRef.id });
    } catch (error) {
      console.error('Error saving loan:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }


async updateAndAddLoan(req, res) {
    const userToken = req.body.userToken;
  
    if (!userToken) {
        return res.status(400).send('Token parameter is missing');
    }
    const lId = req.body.lId;
    const loanData = req.body.loanData;
  
    if (!lId || !loanData) {
        return res.status(400).send('Missing loanData parameters');
    }
  
    try {
        const lenderId = loanData.lenderId;
        if (!lenderId) {
            return res.status(400).send('Lender ID missing in loan data');
        }
  
        // Query to find the lender using lenderId in personalInfo
        const lenderQuery = this.db.collection('users').where('personalInfo.id', '==', lenderId);
        const lenderQuerySnapshot = await lenderQuery.get();
  
        if (lenderQuerySnapshot.empty) {
            return res.status(404).send('Lender not found');
        }
  
        const lenderDocRef = lenderQuerySnapshot.docs[0].ref; // Assuming lenderId is unique and only one document is returned
  
        // Update lender's totalBalance
        await this.updateLenderBalance(lenderDocRef, loanData, extractCurrencyCode(loanData.currency));
  
        // Update or create loan in general loans collection
        await this.updateOrCreateLoan(this.db.collection('loans'), lId, loanData);
  
        // Update or create loan in lender's subcollection
        await this.updateOrCreateLoan(lenderDocRef.collection('loans'), lId, loanData);
  
        if (loanData.borrowerId) {
            const borrowerRef = this.db.collection('users').where('personalInfo.id', '==', loanData.borrowerId);
            const borrowerQuerySnapshot = await borrowerRef.get();
  
            if (borrowerQuerySnapshot.empty) {
                return res.status(404).send('Borrower not found');
            }
  
            const borrowerDocRef = borrowerQuerySnapshot.docs[0].ref; // Assuming borrowerId is unique
  
            // Update borrower's totalBalance and create/update loan in borrower's subcollection
            await Promise.all([
                this.updateBorrowerBalance(borrowerDocRef, loanData, extractCurrencyCode(loanData.currency)),
                this.updateOrCreateLoan(borrowerDocRef.collection('loans'), lId, loanData)
            ]);
        }
  
        res.status(200).json({ message: 'Loan processed successfully' });
    } catch (error) {
        console.error('Error processing loan:', error.message);
        res.status(500).send('Internal Server Error');
    }
  }

  async getLoans(req, res) {
    const amount = parseFloat(req.query.amount); // Parse amount to float
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    try {
      const decodedToken = this.jwt.verify(userToken, 'your-secret-key');
      const userEmail = decodedToken.email;
  
      const userRef = this.db.collection('users').doc(userEmail);
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        return res.status(404).send('User not found');
      }
  
      const userData = userSnapshot.data();
      const userCurrency = extractCurrencyCode(userData.currency);
  
      // Query to get only loans with status 'PENDING'
      const loansSnapshot = await this.db.collection('loans').where('status', '==', 'PENDING').get();
  
      if (loansSnapshot.empty) {
        return res.status(404).send('No pending loans found');
      }
  
      const exchangeRates = await getExchangeRateForAllCurrencies(userCurrency);
  
      const filteredLoans = [];
  
      for (const doc of loansSnapshot.docs) {
        const loanData = doc.data();
        const loanCurrency = extractCurrencyCode(loanData.currency);
  
        try {
          let loanAmountInUserCurrency;
  
          if (userCurrency !== loanCurrency) {
            const exchangeRateKey = `${userCurrency}${loanCurrency}`;
            const exchangeRate = exchangeRates[exchangeRateKey];
  
            if (exchangeRate) {
              loanAmountInUserCurrency = loanData.amount / exchangeRate;
            } else {
              console.error('Exchange rate not found for:', exchangeRateKey);
              throw new Error('Exchange rate not found.');
            }
          } else {
            loanAmountInUserCurrency = loanData.amount;
          }
  
          // Check if the amount condition is met
          if (loanAmountInUserCurrency <= amount) {
            filteredLoans.push(loanData);
          }
        } catch (error) {
          console.error('Could not retrieve exchange rate, error:', error.message);
          throw new Error('Could not retrieve exchange rate, error: ', error.message);
        }
      }
  
      res.status(200).json(filteredLoans);
    } catch (error) {
      console.error('Error retrieving loans:', error.message);
      res.status(500).send('Internal Server Error');
    }
  }
  


  /*
async getLoans(req, res) {
    const amount = parseFloat(req.query.amount); // Parse amount to float
    const userToken = req.query.token;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    try {
      const decodedToken = jwt.verify(userToken, 'your-secret-key');
      const userEmail = decodedToken.email;
  
      const userRef = this.db.collection('users').doc(userEmail);
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        return res.status(404).send('User not found');
      }
  
      const userData = userSnapshot.data();
      const userCurrency = extractCurrencyCode(userData.currency);
  
      // Query to get only loans with status 'PENDING'
      const loansSnapshot = await this.db.collection('loans').where('status', '==', 'PENDING').get();
  
      if (loansSnapshot.empty) {
        return res.status(404).send('No pending loans found');
      }
  
      const exchangeRates = await getExchangeRateForAllCurrencies(userCurrency);
  
      const filteredLoans = [];
  
      for (const doc of loansSnapshot.docs) {
        const loanData = doc.data();
        const loanCurrency = extractCurrencyCode(loanData.currency);
  
        console.log('loanCurrency', loanCurrency);
        console.log('userCurrency', userCurrency);
  
        try {
          let loanAmountInUserCurrency;
  
          if (userCurrency !== loanCurrency) {
            const exchangeRateKey = `${userCurrency}${loanCurrency}`;
            const exchangeRate = exchangeRates[exchangeRateKey];
            console.log(`Exchange Rate for ${exchangeRateKey} is: `, exchangeRate);
  
            if (exchangeRate) {
              loanAmountInUserCurrency = loanData.amount / exchangeRate;
            } else {
              console.error('Exchange rate not found for:', exchangeRateKey);
              throw new Error('Exchange rate not found.');
            }
          } else {
            console.log('ExchangeRate is not needed, same currency');
            loanAmountInUserCurrency = loanData.amount;
          }
  
          // Check if the amount condition is met
          if (loanAmountInUserCurrency <= amount) {
            filteredLoans.push(loanData);
          }
        } catch (error) {
          console.error('Could not retrieve exchange rate, error:', error.message);
          throw new Error('Could not retrieve exchange rate, error: ', error.message);
        }
      }
  
      res.status(200).json(filteredLoans);
    } catch (error) {
      console.error('Error retrieving loans:', error.message);
      res.status(500).send('Internal Server Error');
    } finally {
      totalDelay = 0; // Reset totalDelay after each request
    }
  }*/
  
  async lockLoan(req, res) {
    const userToken = req.body.userToken;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const lId = req.body.lId;
  
    if (!lId) {
      return res.status(400).send('Missing required loan ID');
    }
  
    try {
      const decodedToken = this.jwt.verify(userToken, 'your-secret-key');
      const userEmail = decodedToken.email;
  
      // Use a transaction to ensure atomicity
      const lockTransaction = await this.db.runTransaction(async (transaction) => {
        // Check if the loan is already locked
        const loanDocumentRef = this.db.collection('loans').doc(lId);
        const loanDocument = await transaction.get(loanDocumentRef);
  
        if (!loanDocument.exists) {
          // The loan does not exist
          return Promise.reject('The loan does not exist.');
        }
  
        const lockSubcollectionRef = loanDocumentRef.collection('lock');
        const lockQuery = await lockSubcollectionRef.orderBy('timestamp').limit(1).get();
  
        if (!lockQuery.empty) {
          const firstLockDocument = lockQuery.docs[0];
  
          if (firstLockDocument.id !== userEmail) {
            // The loan is currently locked by another user
            return Promise.reject('The loan is currently locked by another user.');
          }
        }
  
        // Add the lock information to the lock subcollection with the user's email as the document ID
        transaction.set(lockSubcollectionRef.doc(userEmail), { locked: true, timestamp: this.admin.firestore.FieldValue.serverTimestamp() });
        console.log('Loan locked successfully by user:', userEmail);
        return Promise.resolve('');
      });
  
      res.status(200).json({ message: lockTransaction });
    } catch (error) {
      console.error('Error locking loan:', error.message);
  
      if (error === 'The loan is currently locked by another user' || error === 'The loan does not exist.') {
        res.status(403).json({ message: error });
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }
  
  async unlockLoan(req, res) {
    const userToken = req.body.userToken;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const lId = req.body.lId;
  
    if (!lId) {
      return res.status(400).send('Missing required loan ID');
    }
  
    try {
      const decodedToken = this.jwt.verify(userToken, 'your-secret-key');
      const userEmail = decodedToken.email;
  
      // Delete the lock subcollection document with the specified loan ID and user email
      const lockDocumentRef = this.db.collection('loans').doc(lId).collection('lock').doc(userEmail);
      await lockDocumentRef.delete();
  
      console.log('Loan unlocked successfully by user:', userEmail);
      res.status(200).json({ message: '' });
    } catch (error) {
      console.error('Error unlocking loan:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else if (error.code === 'NOT_FOUND') {
        res.status(404).send('Lock subcollection document not found.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }

  async getLoanLockStatus(req, res) {
    const loanId = req.params.loanId;
  
    try {
      // Check if the lock subcollection exists for the specified loan ID
      const lockCollectionRef = this.db.collection('loans').doc(loanId).collection('lock');
      const lockCollectionSnapshot = await lockCollectionRef.get();
  
      const isLocked = !lockCollectionSnapshot.empty;
  
      res.status(200).json({ locked: isLocked });
    } catch (error) {
      console.error('Error checking loan lock status:', error.message);
      res.status(500).send('Internal Server Error');
    }
  }


async approveLoanByLock(req, res) {
    const userToken = req.body.userToken;
  
    if (!userToken) {
      return res.status(400).send('Token parameter is missing');
    }
  
    const lId = req.body.lId;
  
    if (!lId) {
      return res.status(400).send('Missing required loan ID');
    }
  
    try {
      const decodedToken = this.jwt.verify(userToken, 'your-secret-key');
      const userEmail = decodedToken.email;
  
      // Check if the lock subcollection has any documents and order them by timestamp
      const lockSubcollectionRef = this.db.collection('loans').doc(lId).collection('lock');
      const lockQuery = await lockSubcollectionRef.orderBy('timestamp').limit(1).get();
  
      if (!lockQuery.empty) {
        const firstLockDocument = lockQuery.docs[0];
  
        if (firstLockDocument.id === userEmail && firstLockDocument.data().locked) {
          // Approve the loan (you can implement your logic here)
          console.log('Processing loan contract..:', userEmail);
          res.status(200).json({ message: 'Processing loan contract..' });
        } else {
          // The first document in the lock subcollection is not the requesting user or is not locked
          res.status(403).send('Permission denied. User not authorized to approve this loan.');
        }
      } else {
        // The lock subcollection is empty
        res.status(403).send('Permission denied. Loan is not locked by any user.');
      }
    } catch (error) {
      console.error('Error proceeding to contract:', error.message);
  
      if (error.code === 'PERMISSION_DENIED') {
        res.status(403).send('Permission denied. User not authorized to perform this action.');
      } else {
        res.status(500).send('Internal Server Error');
      }
    }
  }

  async updateLenderBalance(lenderDocRef, loanData, oldCurrencyCode) {
    const lenderData = (await lenderDocRef.get()).data();
    const currentLenderBalance = lenderData.totalBalance || 0;
    const lenderCurrencyCode = extractCurrencyCode(lenderData.currency);
  
    if (oldCurrencyCode !== lenderCurrencyCode) {
        const exchangeRate = await getExchangeRate(oldCurrencyCode, lenderCurrencyCode);
        const calculatedExchangedAmountLender = loanData.amount * exchangeRate;
        const newLenderBalance = currentLenderBalance + calculatedExchangedAmountLender;
        await lenderDocRef.update({ totalBalance: newLenderBalance });
    } else {
        const newLenderBalance = currentLenderBalance + loanData.amount;
        await lenderDocRef.update({ totalBalance: newLenderBalance });
    }
  }

  async updateBorrowerBalance(borrowerDocRef, loanData, oldCurrencyCode) {
    const borrowerData = (await borrowerDocRef.get()).data();
    const currentBorrowerBalance = borrowerData.totalBalance || 0;
    const borrowerCurrencyCode = extractCurrencyCode(borrowerData.currency);
  
    if (oldCurrencyCode !== borrowerCurrencyCode) {
        const exchangeRate = await getExchangeRate(oldCurrencyCode, borrowerCurrencyCode);
        const calculatedExchangedAmountBorrower = loanData.amount * exchangeRate;
        const newBorrowerBalance = currentBorrowerBalance - calculatedExchangedAmountBorrower;
        await borrowerDocRef.update({ totalBalance: newBorrowerBalance });
    } else {
        const newBorrowerBalance = currentBorrowerBalance - loanData.amount;
        await borrowerDocRef.update({ totalBalance: newBorrowerBalance });
    }
  }

  async updateOrCreateLoan(collectionRef, loanId, loanData) {
    const loanRef = collectionRef.doc(loanId);
    const loanSnapshot = await loanRef.get();
  
    if (!loanSnapshot.exists) {
        await loanRef.set(loanData);
    } else {
        await loanRef.update(loanData);
    }
  }

}


  

module.exports = LoanManagement;