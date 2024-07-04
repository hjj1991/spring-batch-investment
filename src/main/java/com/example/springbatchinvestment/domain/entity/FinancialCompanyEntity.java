package com.example.springbatchinvestment.domain.entity;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.domain.FinancialGroupType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "financial_company",
        indexes = {@Index(columnList = "financialCompanyCode")})
public class FinancialCompanyEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financialCompanyId;

    @NotNull
    @Column(length = 20, unique = true)
    private String financialCompanyCode;

    @NotNull private String dclsMonth;

    @NotNull private String companyName;

    private String dclsChrgMan;

    private String hompUrl;

    private String calTel;

    @NotNull
    @Enumerated(EnumType.STRING)
    private FinancialGroupType financialGroupType;

    public void updateByCompany(Company company) {
        this.dclsMonth = company.dclsMonth();
        this.companyName = company.korCoNm();
        this.dclsChrgMan = company.dclsChrgMan();
        this.hompUrl = company.hompUrl();
        this.calTel = company.calTel();
    }
}
