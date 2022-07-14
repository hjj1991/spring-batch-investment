package com.example.springbatchinvestment.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@Table(name = "tb_bank")
public class Bank extends BaseTimeEntity {

    @RequiredArgsConstructor
    public enum BankType {
        BANK(020000, "은행"),
        SAVING_BANK(030300, "저축은행");

        private final int topFinGrpNo;
        private final String title;
    }

    @Id
    private String finCoNo;

    @Column
    private String dclsMonth;
    @Column
    private String korCoNm;
    @Column
    private String dclsChrgMan;
    @Column
    private String hompUrl;
    @Column
    private String calTel;

    @Column
    @Enumerated(EnumType.STRING)
    private BankType bankType;

    @Column
    private int enable;

}
