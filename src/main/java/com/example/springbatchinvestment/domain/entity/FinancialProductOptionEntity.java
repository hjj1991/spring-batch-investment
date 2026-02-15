package com.example.springbatchinvestment.domain.entity;

import com.example.springbatchinvestment.domain.FinancialProductOptionModel;
import com.example.springbatchinvestment.domain.InterestRateType;
import com.example.springbatchinvestment.domain.ReserveType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "financial_product_option")
public class FinancialProductOptionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financialProductOptionId;

    @NotNull
    @Column(length = 6)
    private String dclsMonth;

    @NotNull
    @Enumerated(EnumType.STRING)
    private InterestRateType interestRateType;

    private String interestRateTypeName;

    @Enumerated(EnumType.STRING)
    private ReserveType reserveType;

    private String reserveTypeName;

    @NotNull private Integer depositPeriodMonths;

    @Column(precision = 8, scale = 5)
    private BigDecimal baseInterestRate;

    @Column(precision = 8, scale = 5)
    private BigDecimal maximumInterestRate;

    @Lob private String sourcePayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_product_id")
    private FinancialProductEntity financialProductEntity;

    public void updateByProductOption(FinancialProductOptionModel financialProductOptionModel) {
        this.dclsMonth = financialProductOptionModel.dclsMonth();
        this.interestRateType = InterestRateType.fromCode(financialProductOptionModel.intrRateType());
        this.interestRateTypeName = financialProductOptionModel.intrRateTypeNm();
        this.reserveType = ReserveType.fromCode(financialProductOptionModel.rsrvType());
        this.reserveTypeName = financialProductOptionModel.rsrvTypeNm();
        this.depositPeriodMonths = Integer.valueOf(financialProductOptionModel.saveTrm());
        this.baseInterestRate =
                Optional.ofNullable(financialProductOptionModel.intrRate())
                        .map(BigDecimal::valueOf)
                        .orElse(null);
        this.maximumInterestRate =
                Optional.ofNullable(financialProductOptionModel.intrRate2())
                        .map(BigDecimal::valueOf)
                        .orElse(null);
        this.sourcePayload = financialProductOptionModel.toString();
    }
}
