package com.example.invoicegenerator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                InvoiceGeneratorApp()
            }
        }
    }
}

data class InvoiceItem(
    val description: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val total: Double
        get() = quantity * unitPrice
}

@Composable
fun InvoiceGeneratorApp() {
    val items = remember { mutableStateListOf<InvoiceItem>() }
    var customerName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("10") }

    val subtotal = items.sumOf { it.total }
    val tax = subtotal * ((taxPercent.toDoubleOrNull() ?: 0.0) / 100)
    val grandTotal = subtotal + tax
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Invoice Generator") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Item", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter(Char::isDigit) },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitPrice,
                            onValueChange = { unitPrice = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Unit Price") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val qty = quantity.toIntOrNull() ?: 0
                            val price = unitPrice.toDoubleOrNull() ?: 0.0
                            if (description.isNotBlank() && qty > 0 && price > 0) {
                                items += InvoiceItem(description.trim(), qty, price)
                                description = ""
                                quantity = ""
                                unitPrice = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Invoice Preview", fontWeight = FontWeight.Bold)
                    Text("Customer: ${if (customerName.isBlank()) "(not set)" else customerName}")
                    Divider()

                    if (items.isEmpty()) {
                        Text("No line items yet.")
                    } else {
                        items.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}. ${item.description} x${item.quantity}", modifier = Modifier.weight(1f))
                                Text(formatter.format(item.total))
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedTextField(
                        value = taxPercent,
                        onValueChange = { taxPercent = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tax %") },
                        modifier = Modifier.size(width = 120.dp, height = 64.dp)
                    )

                    Text("Subtotal: ${formatter.format(subtotal)}")
                    Text("Tax: ${formatter.format(tax)}")
                    Text(
                        "Grand Total: ${formatter.format(grandTotal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    ShareInvoiceButton(
                        customerName = customerName,
                        items = items,
                        taxPercent = taxPercent.toDoubleOrNull() ?: 0.0,
                        subtotal = subtotal,
                        tax = tax,
                        grandTotal = grandTotal,
                        formatter = formatter
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareInvoiceButton(
    customerName: String,
    items: List<InvoiceItem>,
    taxPercent: Double,
    subtotal: Double,
    tax: Double,
    grandTotal: Double,
    formatter: NumberFormat
) {
    val context = LocalContext.current

    Button(onClick = {
        val invoiceText = buildString {
            appendLine("Invoice")
            appendLine("Customer: ${if (customerName.isBlank()) "(not set)" else customerName}")
            appendLine("------------------------------")
            items.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.description} x${item.quantity} = ${formatter.format(item.total)}")
            }
            appendLine("------------------------------")
            appendLine("Subtotal: ${formatter.format(subtotal)}")
            appendLine("Tax (${taxPercent}%): ${formatter.format(tax)}")
            appendLine("Grand Total: ${formatter.format(grandTotal)}")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, invoiceText)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Share invoice")
        context.startActivity(chooser)
    }) {
        Text("Share Invoice")
    }
}
