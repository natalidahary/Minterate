
const axios = require('axios');

const supportedCurrencies = [
    "U.S. dollar (USD $)",
    "Euro (EUR €)",
    "Japanese yen (JPY ¥)",
    "Sterling (GBP £)",
    "Renminbi (CNY ¥)",
    "Australian dollar (AUD A$)",
    "Canadian dollar (CAD C$)",
    "Swiss franc (CHF CHF)",
    "Hong Kong dollar (HKD HK$)",
    "Singapore dollar (SGD S$)",
    "Swedish krona (SEK kr)",
    "South Korean won (KRW ₩)",
    "Norwegian krone (NOK kr)",
    "New Zealand dollar (NZD NZ$)",
    "Indian rupee (INR ₹)",
    "Mexican peso (MXN $)",
    "New Taiwan dollar (TWD NT$)",
    "South African rand (ZAR R)",
    "Brazilian real (BRL R$)",
    "Danish krone (DKK kr)",
    "Polish złoty (PLN zł)",
    "Thai baht (THB ฿)",
    "Israeli new shekel (ILS ₪)",
    "Indonesian rupiah (IDR Rp)",
    "Czech koruna (CZK Kč)",
    "UAE dirham (AED د.إ)",
    "Turkish lira (TRY ₺)",
    "Hungarian forint (HUF Ft)",
    "Chilean peso (CLP CLP$)",
    "Saudi riyal (SAR ﷼)",
    "Philippine peso (PHP ₱)",
    "Malaysian ringgit (MYR RM)",
    "Colombian peso (COP COL$)",
    "Russian ruble (RUB ₽)",
    "Romanian leu (RON L)",
    "Peruvian sol (PEN S/)",
    "Bahraini dinar (BHD .د.ب)",
    "Bulgarian lev (BGN BGN)",
    "Argentine peso (ARS ARG$)"
   ]

   const currenciesString = supportedCurrencies.map(extractCurrencyCode).join(',');

   function extractCurrencyCode(currencyString) {
    const regex = /\((\w{3})\s*.*\)/;
    const match = currencyString.match(regex);
  
    if (match && match[1]) {
      return match[1];
    }
  
    // Return null if no match is found
    return null;
  }

  const getExchangeRate = async (fromCurrency, toCurrency) => {
    try {
      const accessKey = 'c834d92aa7bcfdcf4fb24992bce1e078'; // Replace with your actual access key
      const url = `http://apilayer.net/api/live?access_key=${accessKey}&currencies=${toCurrency}&source=${fromCurrency}&format=1`;
  
      const response = await axios.get(url);
  
      if (response.status === 200 && response.data.success) {
        const exchangeRate = response.data.quotes[`${fromCurrency}${toCurrency}`];
        //console.log('Exchange Rate:', exchangeRate);
        return exchangeRate;
      } else {
        console.error('Error fetching exchange rate:', response.status, response.statusText);
        throw new Error(`Error fetching exchange rate: ${response.status} ${response.statusText}`);
      }
    } catch (error) {
      console.error('Error:', error.message);
      throw new Error(`Error fetching exchange rate: ${error.message}`);
    }
  };
  
  const getExchangeRateForAllCurrencies = async (sourceCurrency) => {
    try {
      const accessKey = '5eaf638c5359f602977c126211c731d8';
      //another key c834d92aa7bcfdcf4fb24992bce1e078
      const url = `http://apilayer.net/api/live?access_key=${accessKey}&currencies=${currenciesString}&source=${sourceCurrency}&format=1`;
  
      const response = await axios.get(url);
  
      console.log(`Currencies exchange rates ${sourceCurrency} :`, response.data.quotes);
  
      if (response.status === 200 && response.data.success) {
        const exchangeRates = response.data.quotes;
        console.log('Exchange Rates:', exchangeRates);
        return exchangeRates;
      } else {
        console.error('Error fetching exchange rates:', response.status, response.statusText);
        throw new Error(`Error fetching exchange rates: ${response.status} ${response.statusText}`);
      }
    } catch (error) {
      console.error('Error:', error.message);
      throw new Error(`Error fetching exchange rates: ${error.message}`);
    }
  };

  module.exports = {
    extractCurrencyCode,
    getExchangeRate,
    getExchangeRateForAllCurrencies
  };