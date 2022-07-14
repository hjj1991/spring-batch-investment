package com.example.springbatchinvestment.domain.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity extends BaseTimeEntity {


    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @Column
    @LastModifiedBy
    private Long lastModifiedBy;

}
