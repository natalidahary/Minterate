const crypto = require('crypto');

class Credits {
    constructor(db, crypto) {
        this.db = db;
        this.crypto = crypto;
    }

    // Function to manually insert card details into "VerificationRecords"
    async insertCardDetails(cardDetails) {
        const isManualInsertionEnabled = true;
        if (!isManualInsertionEnabled) {
            console.error('Manual insertion is disabled');
            return;
        }

        const hashedCardNumber = this.hashCredit(cardDetails.credentials.cardNumber);

        // Check if the credit card number already exists
        const hashedCardNumberExists = await this.checkHashedCardNumberExistsForInsert(hashedCardNumber);

        try {
            if (hashedCardNumberExists) {
                console.error('Error adding credit. Card number already exists.');
            } else {
                const credentials = {
                    lastFourDigits: cardDetails.credentials.lastFourDigits,
                    cardNumber: hashedCardNumber,
                    monthYear: cardDetails.credentials.monthYear,
                    cvv: cardDetails.credentials.cvv,
                };

                await this.db.collection('verificationRecords').add({ credentials });

                console.log('Card details inserted successfully into "verificationRecords"');
            }
        } catch (error) {
            console.error('Error inserting card details:', error.message);
        }
    }

    // Function to check if hashed card number already exists
    async checkHashedCardNumberExists(hashedCardNumber, currentUserId = null) {
        let userRefByCredit = this.db.collection('users').where('credentials.cardNumber', '==', hashedCardNumber);

        if (currentUserId) {
            userRefByCredit = userRefByCredit.where(admin.firestore.FieldPath.documentId(), '!=', currentUserId);
        }
        try {
            const querySnapshotByCredit = await userRefByCredit.get();
            return !querySnapshotByCredit.empty;
        } catch (error) {
            console.error('Error checking hashed credit card number:', error.message);
            return true; // Treat error as card number exists to be cautious
        }
    }

    // Function to check if hashed card number already exists internal use
    async checkHashedCardNumberExistsForInsert(hashedCardNumber) {
        try {
            const collectionRef = this.db.collection('verificationRecords');
            const querySnapshot = await collectionRef.where('credentials.cardNumber', '==', hashedCardNumber).get();

            if (!querySnapshot.empty) {
                console.error(`Card number ${hashedCardNumber} already exists.`);
                return true; // Card number exists
            } else {
                return false; // Card number doesn't exist
            }
        } catch (error) {
            console.error('Error checking hashed credit card number:', error.message);
            return true; // Treat error as card number exists to be cautious
        }
    }

    // Hash credit card number function
     hashCredit(creditCardNumber) {
        const hash = this.crypto.createHash('sha256');
        hash.update(creditCardNumber);
        const hashedCredit = hash.digest('base64');
        return hashedCredit;
    }
}

// Example usage:
const cardDetailsToInsert = {
    credentials: {
        lastFourDigits: '4444',
        cardNumber: '1111222233334444',
        monthYear: '04/29',
        cvv: '371',
    },
    // Add other details as needed
};

  
module.exports = Credits;

