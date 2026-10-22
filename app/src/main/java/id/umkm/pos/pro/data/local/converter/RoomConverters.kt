package id.umkm.pos.pro.data.local.converter

import androidx.room.TypeConverter
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.SecurityRole
import id.umkm.pos.pro.core.model.TransactionStatus

class RoomConverters {

    @TypeConverter
    fun paymentMethodToString(paymentMethod: PaymentMethod): String = paymentMethod.name

    @TypeConverter
    fun stringToPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun transactionStatusToString(status: TransactionStatus): String = status.name

    @TypeConverter
    fun stringToTransactionStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)

    @TypeConverter
    fun securityRoleToString(role: SecurityRole): String = role.name

    @TypeConverter
    fun stringToSecurityRole(value: String): SecurityRole = SecurityRole.valueOf(value)
}
