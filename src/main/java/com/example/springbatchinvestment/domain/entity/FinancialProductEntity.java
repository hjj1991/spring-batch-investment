package com.example.springbatchinvestment.domain.entity;

import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.JoinRestriction;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "financial_product")
public class FinancialProductEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financialProductId;

    @NotNull
    @Column(length = 20, unique = true)
    private String financialProductCode;

    @NotNull private String financialProductName;

    private String joinWay;

    private String postMaturityInterestRate;

    private String specialCondition;

    @NotNull
    @Enumerated(EnumType.STRING)
    private JoinRestriction joinRestriction;

    @NotNull
    @Enumerated(EnumType.STRING)
    private FinancialProductType financialProductType;

    @NotNull private String joinMember;

    @NotNull private String additionalNotes;

    private Long maxLimit;

    private String dclsStartDay;

    private String dclsEndDay;

    private String financialSubmitDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financialCompanyId", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private FinancialCompanyEntity financialCompanyEntity;
}
