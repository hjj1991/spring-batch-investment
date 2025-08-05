package com.example.springbatchinvestment.domain.entity;

import com.example.springbatchinvestment.converter.FloatArrayConverter;
import com.example.springbatchinvestment.domain.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "financial_product",
        indexes = {@Index(columnList = "financialProductCode")})
public class FinancialProductEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financialProductId;

    @NotNull
    @Column(length = 100)
    private String financialProductCode;

    @NotNull private String financialProductName;

    private String joinWay;

    @Lob private String postMaturityInterestRate;

    @Lob private String specialCondition;

    @NotNull
    @Enumerated(EnumType.STRING)
    private JoinRestriction joinRestriction;

    @NotNull
    @Enumerated(EnumType.STRING)
    private FinancialProductType financialProductType;

    @NotNull private String joinMember;

    @Lob @NotNull private String additionalNotes;

    private Long maxLimit;

    private String dclsMonth;

    private String dclsStartDay;

    private String dclsEndDay;

    private String financialSubmitDay;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(length = 255) // SHA-256 hash is 64 characters long
    private String productContentHash;

    @Lob
    @Convert(converter = FloatArrayConverter.class)
    private float[] embeddingVector;

    public void updateEmbeddingVector(float[] embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public void updateProductContentHash(String productContentHash) {
        this.productContentHash = productContentHash;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financialCompanyId", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private FinancialCompanyEntity financialCompanyEntity;

    @Builder.Default
    @OneToMany(mappedBy = "financialProductEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialProductOptionEntity> financialProductOptionEntities = new ArrayList<>();

    public void updateByProduct(FinancialProductModel financialProductModel) {
        this.status = ProductStatus.ACTIVE;
        this.dclsMonth = financialProductModel.dclsMonth();
        this.financialProductName = financialProductModel.finPrdtNm();
        this.joinWay = financialProductModel.joinWay();
        this.postMaturityInterestRate = financialProductModel.mtrtInt();
        this.specialCondition = financialProductModel.spclCnd();
        this.joinRestriction =
                JoinRestriction.fromCode(Integer.parseInt(financialProductModel.joinDeny()));
        this.joinMember = financialProductModel.joinMember();
        this.additionalNotes = financialProductModel.etcNote();
        this.maxLimit =
                Optional.ofNullable(financialProductModel.maxLimit()).map(Long::valueOf).orElse(null);
        this.dclsStartDay = financialProductModel.dclsStrtDay();
        this.dclsEndDay = financialProductModel.dclsEndDay();
        this.financialSubmitDay = financialProductModel.finCoSubmDay();
        this.financialProductOptionEntities =
                financialProductModel.financialProductOptionModels().stream()
                        .map(this::updateOrCreateOption)
                        .toList();
    }

    private FinancialProductOptionEntity updateOrCreateOption(
            FinancialProductOptionModel financialProductOptionModel) {
        return this.financialProductOptionEntities.stream()
                .filter(
                        financialProductOptionEntity ->
                                this.isMatchingOption(financialProductOptionEntity, financialProductOptionModel))
                .findFirst()
                .map(
                        existingEntity -> {
                            existingEntity.updateByProductOption(financialProductOptionModel);
                            return existingEntity;
                        })
                .orElseGet(() -> this.createNewOption(financialProductOptionModel));
    }

    private boolean isMatchingOption(
            FinancialProductOptionEntity entity, FinancialProductOptionModel model) {
        return model.dclsMonth().equals(this.dclsMonth)
                && InterestRateType.fromCode(model.intrRateType()).equals(entity.getInterestRateType())
                && (model.rsrvType() == null
                        || Objects.equals(ReserveType.fromCode(model.rsrvType()), entity.getReserveType()))
                && model.saveTrm().equals(entity.getDepositPeriodMonths());
    }

    private FinancialProductOptionEntity createNewOption(FinancialProductOptionModel model) {
        return FinancialProductOptionEntity.builder()
                .financialProductEntity(this)
                .interestRateType(InterestRateType.fromCode(model.intrRateType()))
                .reserveType(ReserveType.fromCode(model.rsrvType()))
                .depositPeriodMonths(model.saveTrm())
                .baseInterestRate(BigDecimal.valueOf(model.intrRate()))
                .maximumInterestRate(BigDecimal.valueOf(model.intrRate2()))
                .build();
    }
}
