const jwt = require('jsonwebtoken');
const crypto = require('crypto');

class Authentication {

  constructor(db, jwt, crypto) {
    this.db = db;
    this.jwt = jwt;
    this.crypto = crypto;
  }

    async login(req, res) {
        console.log('Received login request:', req.body);
        const query = {
          email: req.body.email,
          password: req.body.password,
        };
      
        // Find user by email
        const userRef = this.db.collection('users').doc(query.email);
      
        try {
          const doc = await userRef.get();
          if (doc.exists) {
            // Hash the provided password using the same method as during registration
            const hashedPassword = this.hashPassword(query.password);
      
            // Compare hashed passwords
            if (hashedPassword === doc.data().password) {
              // Passwords match
      
              // Generate a unique token for the user
              const token = jwt.sign({ email: doc.data().email }, 'your-secret-key', { expiresIn: '1h' });
      
              // Include the token in the response
              const user = {
                mobile: doc.data().mobile, // Include mobile field
                token: token,
              };
              res.status(200).json(user);
            } else {
              // Passwords do not match
              res.status(404).send('Invalid credentials');
            }
          } else {
            // User not found
            res.status(404).send('User not found or invalid credentials');
          }
        } catch (error) {
          console.error('Error checking user:', error.message);
          res.status(500).send('Internal Server Error');
        }
    }

    async getUserDataByToken (req, res) {
    const userToken = req.query.token;
  
    if (!userToken) {
        return res.status(400).send('Token parameter is missing');
    }
  
    try {
        // Decode the token to get user email
        const decodedToken = jwt.verify(userToken, 'your-secret-key'); // Replace 'your-secret-key' with your actual secret key
  
        const userEmail = decodedToken.email;
  
        // Now you can use the userEmail to retrieve user data
        const userRef = this.db.collection('users').doc(userEmail);
        const userSnapshot = await userRef.get();
  
        if (!userSnapshot.exists) {
            console.log('User not found for email:', userEmail);
            return res.status(404).send('User not found');
        }
  
        // Extract the required fields from the user data
        const userData = userSnapshot.data();
  
        const result = {
            mobile: userData.mobile,
            firstName: userData.personalInfo.firstName,
            lastName: userData.personalInfo.lastName,
            totalBalance: userData.totalBalance,
            lastFourDigits: userData.credentials.lastFourDigits,
            sounds: userData.sounds,
            currency: userData.currency,
            textScalar: userData.textScalar,
            cvv: userData.credentials.cvv,
            monthYear: userData.credentials.monthYear,
            id: userData.personalInfo.id,
            address: userData.personalInfo.address,
            city: userData.personalInfo.city,
            state: userData.personalInfo.state,
        };
  
        //console.log('Retrieved user data:', result);
  
        res.status(200).json(result);
    } catch (error) {
        console.error('Error retrieving user data:', error.message);
        res.status(500).send('Internal Server Error');
    }
  }

  async changePasswordLogin(req, res) {
    const userEmail = req.query.email;
    const newPassword = req.body.newPassword;
  
    console.log('Received request to update user password:', userEmail);
    console.log('New Password:', newPassword);
  
    if (!userEmail || !newPassword) {
      return res.status(400).send('Missing required parameters');
    }
  
    const userRef = this.db.collection('users').doc(userEmail);
  
    try {
      const userSnapshot = await userRef.get();
  
      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }
  
      const hashedPassword = this.hashPassword(newPassword);
  
      // Update user data with hashed password
      await userRef.update({
          'password': hashedPassword,
      });
  
      console.log('User password updated successfully for email:', userEmail);
      res.status(200).send('User password updated successfully');
    } catch (error) {
      console.error('Error updating user mobile:', error.message);
      res.status(500).send('Internal Server Error');
    }
  }



  // Endpoint to get mobile number for a given email
  async getMobileForEmail(req, res) {
    const userEmail = req.query.email;


    if (!userEmail) {
      return res.status(400).send('Missing required parameters');
    }

    const userRef = this.db.collection('users').doc(userEmail);

    try {
      const userSnapshot = await userRef.get();

      if (!userSnapshot.exists) {
        console.log('User not found for email:', userEmail);
        return res.status(404).send('User not found');
      }

      const userData = userSnapshot.data();
      const mobile = userData.mobile; // Assuming the field is named 'mobile', adjust accordingly

      res.status(200).json({ mobile });
    } catch (error) {
      console.error('Error getting user mobile:', error.message);
      res.status(500).send('Internal Server Error');
    }
  }

  hashPassword(password) {
    const hash = crypto.createHash('sha256');
    hash.update(password);
    return hash.digest('base64');
  }

    
}



module.exports = Authentication;