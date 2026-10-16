package id.umkm.pos.pro.domain.accounting

import id.umkm.pos.pro.core.contract.AccountingService
import id.umkm.pos.pro.core.model.PricedCart

class DefaultAccountingService : AccountingService {

    override fun netProfit(pricedCart: PricedCart): Long {
        val invoiceNet = (pricedCart.subtotal - pricedCart.invoiceDiscount).coerceAtLeast(0L)
        return invoiceNet - pricedCart.totalHpp
    }
}
