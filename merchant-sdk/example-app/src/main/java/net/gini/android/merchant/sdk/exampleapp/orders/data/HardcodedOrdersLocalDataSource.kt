package net.gini.android.merchant.sdk.exampleapp.orders.data

import net.gini.android.merchant.sdk.exampleapp.orders.data.model.Order

class HardcodedOrdersLocalDataSource {

    fun getOrders(): List<Order> {
        return listOf(
            Order(
                iban = "DE75201207003100124444",
                recipient = "OTTO GMBH & CO KG",
                amount = "709.97:EUR",
                purpose = "RF7411164022"
            ),
            Order(
                iban = "DE14200800000816170700",
                recipient = "Tchibo GmbH",
                amount = "54.97:EUR",
                purpose = "10020302020"
            ),
            Order(
                iban = "DE86210700200123010101",
                recipient = "Zalando SE",
                amount = "126.62:EUR",
                purpose = "938929192"
            ),
            Order(
                iban = "DE68201207003100755555",
                recipient = "bonprix Handelsgesellschaft mbH",
                amount = "114.88:EUR",
                purpose = "020329984871123"
            ),
            Order(
                iban = "DE13760700120500154000",
                recipient = "Klarna",
                amount = "80.13:EUR",
                purpose = "00425818528311423079"
            )
        )
    }
}
