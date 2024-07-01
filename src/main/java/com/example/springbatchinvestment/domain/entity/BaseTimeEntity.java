package com.example.springbatchinvestment.domain.entity;

import jakarta.persistence.MappedSuperclass;
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
    private ZonedDateTime createdAt;
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
