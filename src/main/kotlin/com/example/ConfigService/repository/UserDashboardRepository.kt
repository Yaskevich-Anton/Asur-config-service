package com.example.ConfigService.repository

import com.example.ConfigService.model.UserDashboards
import com.example.ConfigService.model.entity.UserDashboardEntity
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class UserDashboardRepository(private val dsl: DSLContext) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun findDashboardSettings(userId: Int, factId: Int): Optional<UserDashboardEntity> {
        return dsl.select(
            USER_DASHBOARDS.ID,
            USER_DASHBOARDS.USER_FK.`as`("userId"),
            USER_DASHBOARDS.FACT_FK.`as`("factId"),
            USER_DASHBOARDS.DIM_VALUES,
            USER_DASHBOARDS.VIEW_PARAMS)
            .from(USER_DASHBOARDS)
            .where(USER_DASHBOARDS.USER_FK.eq(userId).and(USER_DASHBOARDS.FACT_FK.eq(factId)))
            .fetchOptional { r -> r.into(UserDashboardEntity::class.java) }
    }

    fun saveDashboardSettings(userId: Int, factId: Int, dimensionsValues: String? = null, viewParams: String? = null) {
        val result = dsl.insertInto(USER_DASHBOARDS)
            .set(USER_DASHBOARDS.FACT_FK, factId)
            .set(USER_DASHBOARDS.USER_FK, userId)
        dimensionsValues?.let { result.set(USER_DASHBOARDS.DIM_VALUES, it) }
        viewParams?.let { result.set(USER_DASHBOARDS.VIEW_PARAMS, it) }
        result.execute()
    }

    fun findAll(): List<UserDashboards> {
        return dsl.select(USER_DASHBOARDS.ID,
            USER_DASHBOARDS.USER_FK.`as`("userId"),
            USER_DASHBOARDS.FACT_FK.`as`("factId"),
            USER_DASHBOARDS.DIM_VALUES,
            USER_DASHBOARDS.VIEW_PARAMS)
            .from(USER_DASHBOARDS)
            .fetch { r -> r.into(UserDashboards::class.java) }
    }

    fun updateDashboardSettings(userId: Int, factId: Int, strDimensions: String?, viewParams: String?) {
        dsl.update(USER_DASHBOARDS)
            .set(USER_DASHBOARDS.DIM_VALUES, strDimensions)
            .set(USER_DASHBOARDS.VIEW_PARAMS, viewParams)
            .where(USER_DASHBOARDS.USER_FK.eq(userId).and(USER_DASHBOARDS.FACT_FK.eq(factId)))
            .execute()
    }
}