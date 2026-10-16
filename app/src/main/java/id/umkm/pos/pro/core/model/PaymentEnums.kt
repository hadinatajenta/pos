package id.umkm.pos.pro.core.model

enum class PaymentMethod {
    CASH,
    QRIS,
    TRANSFER,
    SPLIT,
    KASBON,
}

enum class TransactionStatus {
    COMPLETED,
    PARTIAL_DEBT,
    RETURNED,
}
