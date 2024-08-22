package com.example.ConfigService.repository

import com.example.ConfigService.model.entity.FactDependentEntity
import com.example.ConfigService.model.entity.FactRelativesEntity
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class FactRelativesRepository(private val dsl: DSLContext) {
    fun findDependentFacts(factId: Int): List<FactRelativesEntity> {
        return dsl.selectFrom(FACT_RELATIVES)
            .where(FACT_RELATIVES.FACT_FK.eq(factId))
            .fetch { r -> r.into(FactRelativesEntity::class.java) }
    }

    fun findRelativeFacts(factId: Int): List<FactRelativesEntity> {
        return dsl.selectFrom(FACT_RELATIVES)
            .where(FACT_RELATIVES.FACT_PASSPORT_FK.eq(factId))
            .fetch { r -> r.into(FactRelativesEntity::class.java) }
    }

    fun deleteByPassportId(passportId: Int) {
        dsl.delete(FACT_RELATIVES)
            .where(FACT_RELATIVES.FACT_PASSPORT_FK.eq(passportId))
            .execute()
    }

    fun findAllByRegionId(regionId: Int): List<FactDependentEntity>  {
        val dependentFacts = FACTS.`as`("DEP_FACTS")
        return dsl.select(FACT_RELATIVES.ID,
            FACT_RELATIVES.FACT_PASSPORT_FK,
            FACT_RELATIVES.FACT_FK,
            FACT_RELATIVES.NAME,
            FACT_RELATIVES.ORDER_NUM,
            FACT_RELATIVES.REPORT_OFFSET,
            FACT_RELATIVES.DATA_SOURCE_FK,
            dependentFacts.DATA_SOURCE_FK.`as`("dependentsDataSource"),
            FACTS.PERIOD.`as`("relativesPeriod"),
            dependentFacts.PERIOD.`as`("dependentsPeriod"))
            .from(FACT_RELATIVES)
            .join(FACTS).on(FACTS.ID.eq(FACT_RELATIVES.FACT_FK))
            .join(dependentFacts).on(dependentFacts.ID.eq(FACT_RELATIVES.FACT_PASSPORT_FK))
            .where(FACTS.REGION_FK.eq(regionId).and(dependentFacts.REGION_FK.eq(regionId)))
            .fetch { r -> r.into(FactDependentEntity::class.java) }
    }
}