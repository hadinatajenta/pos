package id.umkm.pos.pro.data.local.mapper

import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.data.local.entity.StaffEntity

object StaffMapper {

    fun toModel(entity: StaffEntity): Staff = Staff(
        id = entity.id,
        name = entity.name,
        role = entity.role,
        isActive = entity.isActive,
    )
}
