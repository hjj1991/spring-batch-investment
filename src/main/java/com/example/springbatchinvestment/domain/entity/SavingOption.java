package com.example.springbatchinvestment.domain.entity;

import lombok.*;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_saving_option")
public class SavingOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long savingOptionNo;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(value = {
            @JoinColumn(name = "finPrdtCd",
                    referencedColumnName = "finPrdtCd"),
            @JoinColumn(name = "finCoNo",
                    referencedColumnName = "finCoNo")
    }, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Saving saving;



    @Column //저축 금리 [소수점 2자리]
    private double intrRate2;
    @Column //최고 우대금리[소수점 2자리]
    private double intrRate;
    @Column(length = 20) //저축 금리 유형명
    private String intrRateTypeNm;
    @Column(length = 10) //저축 기간[단위: 개월]
    private String saveTrm;
    @Column(length = 4) //저축 금리 유형
    private String intrRateType;
    @Column
    private String dclsMonth;
    @Column(length = 10)
    private String rsrvType;
    @Column(length = 100)
    private String rsrvTypeNm;


}
