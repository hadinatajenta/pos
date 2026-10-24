package id.umkm.pos.pro.core.model

enum class PaymentMethod {
    CASH,
    QRIS,
    TRANSFER,
    DEBIT,
    CREDIT,
}

enum class TransactionStatus {
    COMPLETED,
    RETURNED,
}
