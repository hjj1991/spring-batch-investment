package com.example.springbatchinvestment.domain.entity;

import lombok.*;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_deposit_option", uniqueConstraints = {
            @UniqueConstraint(columnNames = {"finCoNo", "finPrdtCd", "saveTrm"})
})
public class DepositOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column //저축 금리 [소수점 2자리]
    private double intrRate2;
    @Column //최고 우대금리[소수점 2자리]
    private double intrRate;
    @Column //저축 기간[단위: 개월]
    private String saveTrm;
    @Column //저축 금리 유형명
    private String intrRateTypeNm;
    @Column //저축 금리 유형
    private String intrRateType;
    @Column
    private String finPrdtCd;
    @Column //금융회사코드
    private String finCoNo;
    @Column //금융상품코드
    private String dclsMonth;
    @Column
    private int enable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Deposit deposit;
}
