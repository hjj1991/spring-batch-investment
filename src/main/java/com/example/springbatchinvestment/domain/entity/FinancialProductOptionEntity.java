package com.example.springbatchinvestment.domain.entity;

import com.example.springbatchinvestment.domain.FinancialProductOptionModel;
import com.example.springbatchinvestment.domain.InterestRateType;
import com.example.springbatchinvestment.domain.ReserveType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
    @Enumerated(EnumType.STRING)
    private InterestRateType interestRateType;

    @Enumerated(EnumType.STRING)
    private ReserveType reserveType;

    @NotNull private String depositPeriodMonths;

    @Column(precision = 5, scale = 2)
    private BigDecimal baseInterestRate;

    @Column(precision = 5, scale = 2)
    private BigDecimal maximumInterestRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financialProductId")
    private FinancialProductEntity financialProductEntity;

    public void updateByProductOption(FinancialProductOptionModel financialProductOptionModel) {
        this.interestRateType = InterestRateType.fromCode(financialProductOptionModel.intrRateType());
        this.reserveType = ReserveType.fromCode(financialProductOptionModel.rsrvType());
        this.depositPeriodMonths = financialProductOptionModel.saveTrm();
        this.baseInterestRate = BigDecimal.valueOf(financialProductOptionModel.intrRate());
        this.maximumInterestRate = BigDecimal.valueOf(financialProductOptionModel.intrRate2());
    }
}
