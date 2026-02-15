package com.example.springbatchinvestment.domain.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Clock;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseTimeEntity {
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "modified_at")
    private ZonedDateTime modifiedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = ZonedDateTime.now(Clock.systemUTC());
        this.modifiedAt = ZonedDateTime.now(Clock.systemUTC());
    }

    @PreUpdate
    public void preUpdate() {
        this.modifiedAt = ZonedDateTime.now(Clock.systemUTC());
    }
}
