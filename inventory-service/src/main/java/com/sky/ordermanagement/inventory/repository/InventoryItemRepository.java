package com.sky.ordermanagement.inventory.repository;

import com.sky.ordermanagement.inventory.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    /**
     * SELECT ... FOR UPDATE, ordered by SKU so concurrent reservations always lock rows in the same
     * order (prevents deadlocks between two orders touching overlapping SKUs).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.sku in :skus order by i.sku")
    List<InventoryItem> findAllForUpdate(@Param("skus") Collection<String> skus);
}
