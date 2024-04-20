package com.example.myapplication.userActions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.requestResponse.Transaction

class TransactionAdapter(private val id: String, private var transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Define views inside the rectangle item
        val transactionName: AppCompatTextView = itemView.findViewById(R.id.transaction_adapter_TVW_nameTextView)
        val transactionAmount: AppCompatTextView = itemView.findViewById(R.id.transaction_adapter_TVW_amountTextView)
        val transactionDate: AppCompatTextView = itemView.findViewById(R.id.transaction_adapter_TVW_dateTextView)
        val transactionCount: AppCompatTextView = itemView.findViewById(R.id.transaction_adapter_TVW_countTextView)
    }
    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_transaction_adapter, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        var income = true
        if(transaction.destination != id){
            income = false
        }
        if (income) {
            holder.transactionName.text = transaction.originFirstName + " " + transaction.originLastName
            holder.transactionAmount.setTextColor(ContextCompat.getColor(holder.itemView.context,
                R.color.greenAmount
            ))
            holder.transactionAmount.text = "+" + formatWithCommas(transaction.amount.toString()) + extractCurrencySymbol(transaction.currency.toString())

        } else {
            // Set the default color or positive color
            holder.transactionName.text = transaction.destinationFirstName + " " + transaction.destinationLastName
            holder.transactionAmount.setTextColor(ContextCompat.getColor(holder.itemView.context,
                R.color.redAmount
            ))
            holder.transactionAmount.text = "-" + formatWithCommas(transaction.amount.toString()) + extractCurrencySymbol(transaction.currency.toString())

        }
        holder.transactionDate.text = transaction.date
        holder.transactionCount.text = transaction.paymentCount
    }


    override fun getItemCount(): Int {
        return transactions.size
    }

    fun formatWithCommas(input: String): String {
        val number = try {
            input.toDouble()
        } catch (e: NumberFormatException) {
            return input // Return input as is if it's not a valid number
        }

        return String.format("%,.2f", number)
    }


    fun extractCurrencySymbol(currencyString: String): String {
        val regex = Regex("\\(([^ ]+) ([^)]+)\\)") // Matches two groups of characters inside parentheses, separated by a space
        val matchResult = regex.find(currencyString)
        return matchResult?.groupValues?.get(2) ?: "" // Returns the second captured group, which is the currency symbol
    }
}