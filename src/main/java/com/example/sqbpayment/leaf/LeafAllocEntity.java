package com.example.sqbpayment.leaf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Leaf 号段分配表实体
 */
@Entity
@Table(name = "leaf_alloc")
public class LeafAllocEntity {

    @Id
    @Column(name = "biz_tag", length = 128)
    private String bizTag;

    @Column(name = "max_id", nullable = false)
    private long maxId;

    @Column(name = "step", nullable = false)
    private int step;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public String getBizTag() {
        return bizTag;
    }

    public void setBizTag(String bizTag) {
        this.bizTag = bizTag;
    }

    public long getMaxId() {
        return maxId;
    }

    public void setMaxId(long maxId) {
        this.maxId = maxId;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
