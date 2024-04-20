const jwt = require('jsonwebtoken');
const cron = require('node-cron');
const {extractCurrencyCode, getExchangeRate } = require('./currency');

class TransactionManagement {
    constructor(db, jwt) {
        this.db = db;
        this.jwt = jwt;
    }
    
    async scheduleDailyLoanCheck() {
      // Schedule a task to run every day at 14:55
      cron.schedule('37 22 * * *', async () => {
          try {
              console.log('Running a daily check on active loans for transactions.');
              const activeLoans = await this.getActiveLoans();
          
              const currentDate = new Date();
              const currentDayOfMonth = currentDate.getDate();
          
              const formattedCurrentDate = currentDate.toISOString().split('T')[0];
          
              for (let loan of activeLoans) {
                  try {
                      const loanStartDate = new Date(loan.startDate);
                      const loanStartDay = loanStartDate.getDate();
                      const loanEndDate = new Date(loan.endDate);
          
                      const formattedLoanStartDate = loanStartDate.toISOString().split('T')[0];
                      const formattedLoanEndDate = loanEndDate.toISOString().split('T')[0];
          
                      if (loanStartDay === currentDayOfMonth && formattedLoanStartDate !== formattedCurrentDate && formattedCurrentDate <= formattedLoanEndDate) {
                          await this.processLoanTransaction(loan);
                      }
                  } catch (loanProcessingError) {
                      console.error('Error processing loan:', loanProcessingError);
                  }
              }
              console.log('finished.');
              try {
                  console.log('Running a daily check on pending loans for expiration.');
                  await this.expireLoans();
              } catch (expireLoansError) {
                  console.error('Error in processing daily loan expiration check:', expireLoansError);
              }
              console.log('finished.');
          
              try {
                  console.log('Running a daily check on expired loans for deletion.');
                  await this.deleteExpiredLoans();
              } catch (deleteExpiredLoansError) {
                  console.error('Error in processing daily loan expiration deletion check:', deleteExpiredLoansError);
              }
              console.log('finished.');
          } catch (error) {
              console.error('Error in the daily loan check process:', error);
          }
      });
  }
    

    async completeLoanRepayment (req, res) {
        const { userToken, loanId } = req.body;
      
        if (!userToken) {
            console.log('Token missing for complete repayment request');
            return res.status(400).send('Token is required');
        }
      
        if (!loanId) {
            console.log('Loan ID missing for complete repayment request');
            return res.status(400).send('Loan ID is required');
        }
      
        try {
            // Decode the token to get user email
            const decodedToken = this.jwt.verify(userToken, 'your-secret-key');
            const userEmail = decodedToken.email;
      
            // Retrieve the loan data
            const loanRef = this.db.collection('loans').doc(loanId);
            const loanSnapshot = await loanRef.get();
      
            if (!loanSnapshot.exists || loanSnapshot.data().status !== 'ACTIVE') {
                console.log('Loan not found or not active for repayment:', loanId);
                return res.status(404).send('Loan not found or not active');
            }
      
            const loanData = loanSnapshot.data();
      
            // Update the loan status to 'COMPLETED'
            await loanRef.update({ status: 'COMPLETED' });
            console.log(`Loan ${loanId} marked as completed successfully`);
      
            // Update the loan status in the borrower's subcollection
            const borrowerLoansRef = this.db.collection('users').doc(userEmail).collection('loans').doc(loanId);
            await borrowerLoansRef.update({ status: 'COMPLETED' });
            console.log(`Loan ${loanId} status updated to completed in borrower's subcollection`);
      
            // Retrieve lender's email from the users collection
            let lenderEmail = null;
            const usersSnapshot = await this.db.collection('users').get();
            usersSnapshot.forEach(doc => {
                if (doc.data().personalInfo && doc.data().personalInfo.id === loanData.lenderId) {
                    lenderEmail = doc.id; // doc.id is the email
                }
            });
      
            if (!lenderEmail) {
                console.log(`Lender with ID ${loanData.lenderId} not found`);
                return res.status(404).send('Lender not found');
            }
      
            // Update the loan status in the lender's subcollection
            const lenderLoansRef = this.db.collection('users').doc(lenderEmail).collection('loans').doc(loanId);
            await lenderLoansRef.update({ status: 'COMPLETED' });
            console.log(`Loan ${loanId} status updated to completed in lender's subcollection`);
      
            res.status(200).json({ message: 'Loan repaid successfully', loanId: loanId });
        } catch (error) {
            console.error('Error completing loan repayment:', error.message);
            res.status(500).send('Internal Server Error');
        }
      }


  async recordTransaction (req, res) {
    const { userToken, transaction } = req.body;
  
    // Log the request
    //console.log("Received transaction request:", transaction);
  
    // Authentication and validation checks
    if (!userToken) {
      console.error('User token is missing');
      return res.status(401).send({ message: 'Unauthorized: Token is required' });
    }
  
    try {
      // Decode the token to validate it
      const decodedToken = this.jwt.verify(userToken, 'your-secret-key');
      console.log(`Token decoded successfully for user: ${decodedToken.email}`);
  
      // Validate transaction data
      if (!transaction || !transaction.loanId || !transaction.amount || isNaN(transaction.amount)) {
        console.error('Invalid transaction data:', transaction);
        return res.status(400).send({ message: 'Invalid transaction data' });
      }
  
      // Record the transaction
      const transactionRef = await this.db.collection('transactions').add(transaction);
      console.log('Transaction recorded:', transaction);
  
      // Update balances
      await updateBalances(this.db, transaction);
      console.log('Balances updated successfully for the transaction');
  
      // Get the destination user
      const originUser = await this.getUserById(transaction.origin);
  
      // Ensure that the user was found
      if (!originUser) {
        throw new Error('Origin user not found.');
      }
  
      // Save the transaction in the origin user's subcollection within the loan document
      const originUserEmail = originUser.email;
      console.log('originEmail:',originUserEmail);
      const originUserLoanRef = this.db.collection('users').doc(originUserEmail).collection('loans').doc(transaction.loanId);
      console.log('transactionRefId:',transactionRef.id);
      // Create or update the 'transactions' subcollection within the loan document
      const originUserTransactionsRef = originUserLoanRef.collection('transactions').doc(transactionRef.id);
  
      await originUserTransactionsRef.set({
        transactionId: transactionRef.id,
        ...transaction,
      });
  
       // Get the destination user
       const destinationUser = await this.getUserById(transaction.destination);
  
       // Ensure that the user was found
       if (!destinationUser) {
         throw new Error('Destination user not found.');
       }
  
      // Save the transaction in the destination user's subcollection within the loan document
      const destinationUserEmail = destinationUser.email;
      console.log('destinationEmail:',destinationUserEmail);
      const destinationUserLoanRef = this.db.collection('users').doc(destinationUserEmail).collection('loans').doc(transaction.loanId);
  
      // Create or update the 'transactions' subcollection within the loan document
      const destinationUserTransactionsRef = destinationUserLoanRef.collection('transactions').doc(transactionRef.id);
  
      await destinationUserTransactionsRef.set({
        transactionId: transactionRef.id,
        ...transaction,
      });
  
  
      res.status(200).send({ message: 'Transaction recorded and balances updated successfully' });
    } catch (error) {
      console.error('Error processing transaction:', error.message);
      res.status(500).send({ message: 'Internal Server Error' });
    }
  }

  async getActiveLoans() {
    try {
      // Query to get only loans with status 'ACTIVE'
      const loansSnapshot = await this.db.collection('loans').where('status', '==', 'ACTIVE').get();
  
      if (loansSnapshot.empty) {
        return [];
      }
  
      // Map the loan documents to data
      const activeLoans = loansSnapshot.docs.map(doc => doc.data());
      return activeLoans;
    } catch (error) {
      console.error('Error fetching active loans:', error);
      throw error; // Rethrow the error to handle it in the scheduler
    }
  }
  
  async expireLoans() {
    try {
      // Fetch PENDING loans
      const pendingLoansSnapshot = await this.db.collection('loans').where('status', '==', 'PENDING').get();
  
      if (pendingLoansSnapshot.empty) {
        console.log('No pending loans found');
        return;
      }
  
      // Iterate over each pending loan
      for (let loanDoc of pendingLoansSnapshot.docs) {
        try {
          const loanData = loanDoc.data();
  
          // Parse the expirationDate using the parseExpirationDate function
          const expirationDateParts = loanData.expirationDate.split('/');
          const expirationDate = new Date(
            parseInt(expirationDateParts[2]), // Year
            parseInt(expirationDateParts[1]) - 1, // Month (subtract 1 since months are zero-based)
            parseInt(expirationDateParts[0]), // Day
            0, 0, 0, 0 // Set hours, minutes, seconds, and milliseconds to 0
          );
  
          // Adjust the current date to the same time zone as expirationDate
          const currentDate = new Date();
          currentDate.setHours(0, 0, 0, 0); // Set hours, minutes, seconds, and milliseconds to 0
  
          // Check if the current date is greater than or equal to the expiration date
          if (currentDate >= expirationDate) {
            // Change the status of the loan to "EXPIRED"
            await loanDoc.ref.update({ status: 'EXPIRED' });
  
            // Update the status of the loan in the lender user's loans subcollection
            const lenderId = loanData.lenderId; // Assuming lenderId is under personalInfo
  
            // Update in the lender user's loans subcollection
            const lenderSnapshot = await this.db.collection('users').where('personalInfo.id', '==', lenderId).get();
  
            if (!lenderSnapshot.empty) {
              const lenderUserDoc = lenderSnapshot.docs[0];
              await lenderUserDoc.ref.collection('loans').doc(loanDoc.id).update({ status: 'EXPIRED' });
              console.log('Loan expired:', loanDoc.id);
            }
          }
        } catch (error) {
          console.error('Error processing loan:', error);
        }
      }
    } catch (error) {
      console.error('Error expiring loans:', error);
    }
  }
  
  async deleteExpiredLoans() {
    try {
      // Fetch all loans with status 'EXPIRED'
      const allLoansSnapshot = await this.db.collection('loans').where('status', '==', 'EXPIRED').get();
  
      if (allLoansSnapshot.empty) {
        console.log('No expired loans found');
        return;
      }
  
      // Iterate over each expired loan
      for (let loanDoc of allLoansSnapshot.docs) {
        try {
          const loanData = loanDoc.data();
  
          // Parse the expirationDate using the parseExpirationDate function
          const expirationDateParts = loanData.expirationDate.split('/');
          const expirationDate = new Date(
            parseInt(expirationDateParts[2]), // Year
            parseInt(expirationDateParts[1]) - 1, // Month (subtract 1 since months are zero-based)
            parseInt(expirationDateParts[0]), // Day
            0, 0, 0, 0 // Set hours, minutes, seconds, and milliseconds to 0
          );
  
          // Check if the current date is greater than the expiration date plus one month
          const currentDate = new Date();
          expirationDate.setMonth(expirationDate.getMonth() + 1); // Add one month to the expiration date
  
          if (currentDate >= expirationDate) {
            // Delete the loan from the loans collection
            await loanDoc.ref.delete();
  
            // Delete the loan from the lender user's loans subcollection
            const lenderId = loanData.lenderId;
  
            // Fetch the lender user document using personalInfo.id
            const lenderSnapshot = await this.db.collection('users').where('personalInfo.id', '==', lenderId).get();
  
            if (!lenderSnapshot.empty) {
              // Get the first document (assuming personalInfo.id is unique)
              const lenderDoc = lenderSnapshot.docs[0];
  
              // Delete the loan from the lender user's loans subcollection
              const loanSubcollectionRef = lenderDoc.ref.collection('loans');
              const loanSubcollectionDocs = await loanSubcollectionRef.where('lId', '==', loanDoc.id).get();
  
              if (!loanSubcollectionDocs.empty) {
                // Use batched writes to perform multiple delete operations atomically
                const batch = this.db.batch();
                loanSubcollectionDocs.docs.forEach((loanSubDoc) => {
                  batch.delete(loanSubDoc.ref);
                });
  
                await batch.commit();
  
                console.log('Expired loan deleted from subcollection:', loanDoc.id);
              } else {
                console.log('Loan not found in lender subcollection:', loanDoc.id);
              }
            } else {
              console.log('Lender user not found:', lenderId);
            }
          }
        } catch (error) {
          console.error('Error deleting expired loan:', error);
        }
      }
    } catch (error) {
      console.error('Error deleting expired loans:', error);
    }
  }

  async processLoanTransaction(loan) {
    try {
      // Log the structure of the 'loan' object
      console.log('Loan object:', loan);
  
      // Ensure that the 'amount' property exists in the 'loan' object
      if (typeof loan.amount !== 'number') {
        throw new Error('Invalid loan object. Amount property is missing or not a number.');
      }
  
      // Calculate the monthly repayment amount
      const amount = roundTo2DecimalPlaces(calculateMonthlyRepayment(loan.amount, loan.interestRate, loan.period));
  
      // Log the calculated amount
      console.log('Calculated amount:', amount);
  
      // Retrieve currency, origin, and destination names
      const loanCurrency = loan.currency;
  
      // Get the origin user
      const originUser = await this.getUserById(loan.borrowerId);
      console.log('allOriginUser:', originUser);
  
      // Ensure that the user was found
      if (!originUser) {
        throw new Error('Origin user not found.');
      }
  
      // Get the destination user
      const destinationUser = await this.getUserById(loan.lenderId);
  
      // Ensure that the user was found
      if (!destinationUser) {
        throw new Error('Destination user not found.');
      }
  
      // Calculate the payment count based on the current date, loan start date, and period
      const currentDate = new Date();
      const startDate = new Date(loan.startDate);
      const monthsDifference = (currentDate.getFullYear() - startDate.getFullYear()) * 12 + currentDate.getMonth() - (startDate.getMonth() + 1);
  
      // Adjust the paymentCount to start from 1
      const paymentCount = monthsDifference + 1;
  
      // Prepare the transaction object with additional fields
      const transaction = {
        amount: amount,
        currency: loanCurrency,
        date: new Date().toISOString().split('T')[0], // Format: YYYY-MM-DD
        loanId: loan.lId,
        origin: loan.borrowerId,
        destination: loan.lenderId,
        originFirstName: originUser.personalInfo.firstName,
        originLastName: originUser.personalInfo.lastName,
        destinationFirstName: destinationUser.personalInfo.firstName,
        destinationLastName: destinationUser.personalInfo.lastName,
        paymentCount: `${paymentCount}/${loan.period}`,
      };
  
      // Insert the transaction into the transactions collection
      const transactionRef = await this.db.collection('transactions').add(transaction);
      console.log(`Transaction for loan ID: ${loan.lId} processed successfully`);
  
      // Save the transaction in the origin user's subcollection within the loan document
      const originUserEmail = originUser.email;
      console.log('originEmail:',originUserEmail);
      const originUserLoanRef = this.db.collection('users').doc(originUserEmail).collection('loans').doc(loan.lId);
      console.log('loanId', loan.lId);
      console.log('transactionRefId:',transactionRef.id);
      // Create or update the 'transactions' subcollection within the loan document
      const originUserTransactionsRef = originUserLoanRef.collection('transactions').doc(transactionRef.id);
  
      await originUserTransactionsRef.set({
        transactionId: transactionRef.id,
        ...transaction,
      });
  
      // Save the transaction in the destination user's subcollection within the loan document
      const destinationUserEmail = destinationUser.email;
      console.log('destinationEmail:',destinationUserEmail);
      const destinationUserLoanRef = this.db.collection('users').doc(destinationUserEmail).collection('loans').doc(loan.lId);
  
      // Create or update the 'transactions' subcollection within the loan document
      const destinationUserTransactionsRef = destinationUserLoanRef.collection('transactions').doc(transactionRef.id);
  
      await destinationUserTransactionsRef.set({
        transactionId: transactionRef.id,
        ...transaction,
      });
  
      // Balance calculations:
      const loanRegularAmount = loan.amount / loan.period
  
      // Update origin totalBalance
      const originQuery = this.db.collection('users').where('personalInfo.id', '==', loan.borrowerId);
      const originQuerySnapshot = await originQuery.get();
      if(originQuerySnapshot.empty){
        return res.status(404).send('Origin not found');
      }
      const originUserDocRef = originQuerySnapshot.docs[0].ref;
  
      const originData = (await originUserDocRef.get()).data();
      const currentOriginBalance = originData.totalBalance || 0;
  
      const oldCurrencyCode = extractCurrencyCode(loan.currency);
      const newCurrencyCodeOrigin = extractCurrencyCode(originData.currency);
      let newOriginBalance;
  
      if(oldCurrencyCode != newCurrencyCodeOrigin){
        const exchangeRateOrigin = await getExchangeRate(oldCurrencyCode, newCurrencyCodeOrigin);
        const originLoanRepayment = loanRegularAmount*exchangeRateOrigin;
        newOriginBalance = currentOriginBalance + originLoanRepayment;
      } else{
        newOriginBalance = currentOriginBalance + loanRegularAmount;
      }
  
      // Update destination totalBalance
      const destinationQuery = this.db.collection('users').where('personalInfo.id', '==', loan.lenderId);
      const destinationQuerySnapshot = await destinationQuery.get();
      if(destinationQuerySnapshot.empty){
        return res.status(404).send('Destination not found');
      }
      const destinationUserDocRef = destinationQuerySnapshot.docs[0].ref;
  
      const destinationData = (await destinationUserDocRef.get()).data();
      const currentDestinationBalance = destinationData.totalBalance || 0;
  
      const newCurrencyCodeDestination = extractCurrencyCode(destinationData.currency);
      let newDestinationBalance;
  
      if(oldCurrencyCode != newCurrencyCodeDestination){
        const exchangeRateDestination = await getExchangeRate(oldCurrencyCode, newCurrencyCodeDestination);
        const DestinationLoanDeposite = loanRegularAmount*exchangeRateDestination;
        newDestinationBalance = currentDestinationBalance - DestinationLoanDeposite;
      } else{
        newDestinationBalance = currentDestinationBalance - loanRegularAmount;
      }
  
      // Check if this is the last payment (12/12)
      if (paymentCount === loan.period) {
        if (Math.abs(newOriginBalance) < 1) {
          newOriginBalance = 0;
        }
        if (Math.abs(newDestinationBalance) < 1) {
          newDestinationBalance = 0;
        }
      
        // Update loan status to COMPLETED in the general loans collection
        const loanRef = this.db.collection('loans').doc(loan.lId);
        await loanRef.update({ status: 'COMPLETED' });
  
        // Update loan status to COMPLETED in the lender's subcollection
        await destinationUserDocRef.collection('loans').doc(loan.lId).update({ status: 'COMPLETED' });
  
        // Update loan status to COMPLETED in the borrower's subcollection
        await originUserDocRef.collection('loans').doc(loan.lId).update({ status: 'COMPLETED' });
      }
  
       // Update origin and destination balances
       await originUserDocRef.update({ totalBalance: newOriginBalance});
       await destinationUserDocRef.update({ totalBalance: newDestinationBalance});
  
      // Additional logic as needed (e.g., update loan record, notify parties)
    } catch (error) {
      console.error('Error in processing loan transaction:', error.message);
      // Additional error handling logic
    }
  }

  async getUserById(userId) {
    try {
      const userQuery = this.db.collection('users').where('personalInfo.id', '==', userId);
      const userSnapshot = await userQuery.get();
  
      if (userSnapshot.empty) {
        return null; // or throw an error as needed
      }
  
      const userData = userSnapshot.docs[0].data();
      return userData;
    } catch (error) {
      console.error('Error fetching user by ID:', error.message);
      throw error;
    }
  }
  
}




  function roundTo2DecimalPlaces(number) {
    return Math.round(number * 100) / 100;
  }
  
  
  

  function calculateMonthlyRepayment(amount, rate, period) {
    const principal = parseFloat(amount);
    const annualRate = parseFloat(rate);
    const periods = parseFloat(period);
  
    const monthlyRate = annualRate / 12.0 / 100.0;
    const numerator = monthlyRate * Math.pow(1 + monthlyRate, periods);
    const denominator = Math.pow(1 + monthlyRate, periods) - 1;
  
    return principal * (numerator / denominator);
  }


  async function updateBalances(db, transaction) {
    // Retrieve and update borrower's balance
    const borrowerRef = db.collection('users').where('personalInfo.id', '==', transaction.origin);
    const borrowerSnapshot = await borrowerRef.get();
    if (borrowerSnapshot.empty) {
      throw new Error('Borrower not found.');
    }
    let borrowerData = borrowerSnapshot.docs[0].data();
  
    const oldCurrencyCode = extractCurrencyCode(transaction.currency);
    const newCurrencyCodeBorrower = extractCurrencyCode(borrowerData.currency);
    if(oldCurrencyCode != newCurrencyCodeBorrower){
      const exchangeRateBorrower = await getExchangeRate(oldCurrencyCode, newCurrencyCodeBorrower);
      const borrowerLoanRepayment = transaction.amount*exchangeRateBorrower;
      let newBorrowerBalance = borrowerData.totalBalance + borrowerLoanRepayment;
      await borrowerSnapshot.docs[0].ref.update({ totalBalance: newBorrowerBalance });
    } else{
      let newBorrowerBalance = borrowerData.totalBalance + transaction.amount;
      await borrowerSnapshot.docs[0].ref.update({ totalBalance: newBorrowerBalance });
    }
  
    // Retrieve and update lender's balance
    const lenderRef = db.collection('users').where('personalInfo.id', '==', transaction.destination);
    const lenderSnapshot = await lenderRef.get();
    if (lenderSnapshot.empty) {
      throw new Error('Lender not found.');
    }
    let lenderData = lenderSnapshot.docs[0].data();
  
    const newCurrencyCodeLender = extractCurrencyCode(lenderData.currency);
    if(oldCurrencyCode != newCurrencyCodeLender){
      const exchangeRateLender = await getExchangeRate(oldCurrencyCode, newCurrencyCodeLender);
      const lenderLoanRepayment = transaction.amount*exchangeRateLender;
      let newLenderBalance = lenderData.totalBalance - lenderLoanRepayment;
      await lenderSnapshot.docs[0].ref.update({ totalBalance: newLenderBalance });
    } else{
      let newLenderBalance = lenderData.totalBalance - transaction.amount;
      await lenderSnapshot.docs[0].ref.update({ totalBalance: newLenderBalance });
    }
  }
  




  module.exports = TransactionManagement;