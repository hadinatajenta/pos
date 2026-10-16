package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.Customer
import id.umkm.pos.pro.data.local.entity.CustomerEntity

object CustomerMapper {

    fun toEntity(customer: Customer): CustomerEntity = CustomerEntity(
        id = customer.id,
        name = customer.name,
        whatsappNumber = customer.whatsappNumber,
        totalDebt = customer.totalDebt,
        notes = customer.notes,
    )

    fun toModel(entity: CustomerEntity): Customer = Customer(
        id = entity.id,
        name = entity.name,
        whatsappNumber = entity.whatsappNumber,
        totalDebt = entity.totalDebt,
        notes = entity.notes,
    )
}
