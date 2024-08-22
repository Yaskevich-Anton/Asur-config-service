package com.example.ConfigService.repository

import com.example.ConfigService.model.entity.UserFactsEntity
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class UserFactsRepository(private val dsl: DSLContext) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun findUserFactsByIdAndUserId(factId: Int, currentUserId: Int, moduleId: Int? = null): Optional<UserFactsEntity> {
        val moduleCondition = moduleId?.let { USER_FACTS.MODULE_FK.eq(it) } ?: USER_FACTS.MODULE_FK.isNull
        return dsl.selectFrom(USER_FACTS)
            .where(USER_FACTS.FACT_FK.eq(factId).and(USER_FACTS.USER_FK.eq(currentUserId))).and(moduleCondition)
            .fetchOptional { r -> r.into(UserFactsEntity::class.java) }
    }

    fun findMaxOrderNumByUserIdAndModuleId(userId: Int, moduleId: Int?): Int {
        var condition = moduleId?.let { USER_FACTS.MODULE_FK.eq(it) } ?: USER_FACTS.MODULE_FK.isNull
        condition = condition.and(USER_FACTS.USER_FK.eq(userId))
        return dsl.select(DSL.max(USER_FACTS.ORDER_NUM))
            .from(USER_FACTS)
            .where(condition)
            .fetchSingle()
            .into(Int::class.java)
    }

    fun pin(currentUserId: Int, factId: Int, moduleId: Int? = null, position: Int? = null) {
        val orderNum = position ?: (findMaxOrderNumByUserIdAndModuleId(currentUserId, moduleId) + 1)
        dsl.insertInto(USER_FACTS)
            .set(USER_FACTS.USER_FK, currentUserId)
            .set(USER_FACTS.FACT_FK, factId)
            .set(USER_FACTS.MODULE_FK, moduleId)
            .set(USER_FACTS.ORDER_NUM, orderNum)
            .execute()
    }

    fun updateOrderNum(oldPosition: Int, newPosition: Int, currentUserId: Int, viewModuleId: Int?) {
        val moduleCondition = viewModuleId?.let { USER_FACTS.MODULE_FK.eq(it) } ?: USER_FACTS.MODULE_FK.isNull
        dsl.update(USER_FACTS)
            .set(USER_FACTS.ORDER_NUM, newPosition)
            .where(USER_FACTS.USER_FK.eq(currentUserId).and(USER_FACTS.ORDER_NUM.eq(oldPosition).and(moduleCondition)))
            .execute()
    }

    fun delete(factId: Int, currentUserId: Int, moduleId: Int? = null) {
        var condition = USER_FACTS.USER_FK.eq(currentUserId).and(USER_FACTS.FACT_FK.eq(factId))
        condition = moduleId?.let {
            condition.and(USER_FACTS.MODULE_FK.eq(it))
        } ?: condition.and(USER_FACTS.MODULE_FK.isNull)
        dsl.delete(USER_FACTS)
            .where(condition)
            .execute()
    }

    fun findUserFactsByFactIdAndUserId(factId: Int, currentUserId: Int) =
        dsl.selectFrom(USER_FACTS)
            .where(USER_FACTS.FACT_FK.eq(factId).and(USER_FACTS.USER_FK.eq(currentUserId)))
            .fetch { r -> r.into(UserFactsEntity::class.java) }
}