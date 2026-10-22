package id.umkm.pos.pro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import id.umkm.pos.pro.core.model.PaymentMethod
import id.umkm.pos.pro.core.model.TransactionStatus

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["invoice_no"], unique = true),
        Index(value = ["created_at"]),
        Index(value = ["customer_id"]),
        Index(value = ["cashier_id"]),
        Index(value = ["shift_id"]),
        Index(value = ["staff_id"]),
        Index(value = ["payment_method"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "invoice_no") val invoiceNo: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "cashier_id") val cashierId: Long?,
    @ColumnInfo(name = "customer_id") val customerId: Long?,
    @ColumnInfo(name = "subtotal") val subtotal: Long,
    @ColumnInfo(name = "discount_total") val discountTotal: Long,
    @ColumnInfo(name = "invoice_discount") val invoiceDiscount: Long,
    @ColumnInfo(name = "tax_total") val taxTotal: Long,
    @ColumnInfo(name = "rounding") val rounding: Long,
    @ColumnInfo(name = "grand_total") val grandTotal: Long,
    @ColumnInfo(name = "total_hpp") val totalHpp: Long,
    @ColumnInfo(name = "net_profit") val netProfit: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "change_amount") val changeAmount: Long,
    @ColumnInfo(name = "remaining_debt") val remainingDebt: Long,
    @ColumnInfo(name = "due_date") val dueDate: Long?,
    @ColumnInfo(name = "status") val status: TransactionStatus,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "cashier_name") val cashierName: String? = null,
    @ColumnInfo(name = "shift_id") val shiftId: Long? = null,
    @ColumnInfo(name = "staff_id") val staffId: Long? = null,
)
