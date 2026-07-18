package com.biyesheji.order.mapper;

import com.biyesheji.order.dto.InventoryInsightRow;
import com.biyesheji.order.dto.InventoryLedgerEvidenceRow;
import com.biyesheji.order.dto.InventorySaleEvidenceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InventoryInsightMapper {

    List<InventoryInsightRow> selectSnapshots(
            @Param("asOf") LocalDateTime asOf,
            @Param("start7") LocalDateTime start7,
            @Param("start30") LocalDateTime start30,
            @Param("start90") LocalDateTime start90,
            @Param("keyword") String keyword
    );

    InventoryInsightRow selectSnapshotBySkuId(
            @Param("skuId") Long skuId,
            @Param("asOf") LocalDateTime asOf,
            @Param("start7") LocalDateTime start7,
            @Param("start30") LocalDateTime start30,
            @Param("start90") LocalDateTime start90
    );

    List<InventorySaleEvidenceRow> selectRecentConfirmedSales(
            @Param("skuId") Long skuId,
            @Param("asOf") LocalDateTime asOf,
            @Param("limit") int limit
    );

    List<InventoryLedgerEvidenceRow> selectRecentStockLedgers(
            @Param("skuId") Long skuId,
            @Param("limit") int limit
    );
}
