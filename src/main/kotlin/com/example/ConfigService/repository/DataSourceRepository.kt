package com.example.ConfigService.repository

import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import ru.rieksp.coreservice.exception.CoreException
import ru.rieksp.coreservice.exception.ErrorCode
import com.example.ConfigService.model.entity.DataSourceEntity
@Repository
class DataSourceRepository(private val dsl: DSLContext) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun findDataSourceByIdAndFactId(dataSourceId: Int, factId: Int): DataSourceEntity {
        val divisions = REGION_DIVISIONS.`as`("DIVISIONS")
        val respDivisions = divisions.`as`("respDivisions")
        val respUsers = USERS.`as`("respUsers")
        val respRegionDivisions = REGION_DIVISIONS.`as`("respRegionDivisions")

        return dsl.select(
                DATA_SOURCES.ID,
                DATA_SOURCES.REGION_FK,
                DATA_SOURCES.NAME,
                DATA_SOURCES.DESCRIPTION,
                DATA_SOURCES.SEGMENT,
                DATA_SOURCES.RESP_DIV_FK,
                respDivisions.SHORT_NAME.`as`("respDivShortName"),
                respDivisions.LONG_NAME.`as`("respDivLongName"),
                DATA_SOURCES.RESP_USER_FK,
                respUsers.NAME.`as`("respUserName"),
                respUsers.LAST_NAME.`as`("respUserLastName"),
                respUsers.MIDDLE_NAME.`as`("respUserMiddleName"),
                respUsers.REGION_DIV_FK.`as`("respUserDivId"),
                respRegionDivisions.SHORT_NAME.`as`("respUserDivShortName"),
                respRegionDivisions.LONG_NAME.`as`("respUserDivLongName")
        )
                .from(DATA_SOURCES)
                .join(FACT_DATA_SOURCES).on(DATA_SOURCES.ID.eq(FACT_DATA_SOURCES.DATA_SOURCE_FK))
                .join(divisions).on(DATA_SOURCES.RESP_DIV_FK.eq(divisions.ID))
                .join(respUsers).on(DATA_SOURCES.RESP_USER_FK.eq(respUsers.ID))
                .leftJoin(respRegionDivisions).on(respRegionDivisions.ID.eq(respUsers.REGION_DIV_FK))
                .where(FACT_DATA_SOURCES.FACT_FK.eq(factId).and(FACT_DATA_SOURCES.DATA_SOURCE_FK.eq(dataSourceId)))
                .fetchOptional()
                .orElseThrow {
                    log.warn("Не найден источник данных по ID = $dataSourceId и по ID показателя = $factId")
                    throw CoreException(ErrorCode.ENTITY_NOT_FOUND, "Не найден источник данных по ID = $dataSourceId и по ID показателя = $factId")
                }
                .into(DataSourceEntity::class.java)
    }
}