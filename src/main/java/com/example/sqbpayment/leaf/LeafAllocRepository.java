package com.example.sqbpayment.leaf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leaf 号段分配表 Repository
 * 通过原子 UPDATE 实现号段分配
 */
@Repository
public interface LeafAllocRepository extends JpaRepository<LeafAllocEntity, String> {

    /**
     * 原子更新 max_id，分配一个新号段
     * UPDATE leaf_alloc SET max_id = max_id + step, update_time = NOW() WHERE biz_tag = ?
     */
    @Modifying
    @Transactional
    @Query("UPDATE LeafAllocEntity e SET e.maxId = e.maxId + e.step, e.updateTime = CURRENT_TIMESTAMP WHERE e.bizTag = :bizTag")
    int updateMaxId(@Param("bizTag") String bizTag);
}
