package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import java.util.Optional;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class FinancialCompanyItemWriter implements ItemWriter<Company> {

    private final FinancialCompanyRepository financialCompanyRepository;

    public FinancialCompanyItemWriter(FinancialCompanyRepository financialCompanyRepository) {
        this.financialCompanyRepository = financialCompanyRepository;
    }

    @Override
    public void write(Chunk<? extends Company> chunk) throws Exception {
        chunk
                .getItems()
                .forEach(
                        company -> {
                            Optional<FinancialCompanyEntity> optionalFinancialCompanyEntity =
                                    this.financialCompanyRepository.findByFinancialCompanyCode(company.finCoNo());

                            FinancialCompanyEntity financialCompanyEntity;
                            if (optionalFinancialCompanyEntity.isPresent()) {
                                financialCompanyEntity = optionalFinancialCompanyEntity.get();
                                financialCompanyEntity.updateByCompany(company);
                            } else {
                                financialCompanyEntity =
                                        FinancialCompanyEntity.builder()
                                                .financialCompanyCode(company.finCoNo())
                                                .dclsMonth(company.dclsMonth())
                                                .companyName(company.korCoNm())
                                                .dclsChrgMan(company.dclsChrgMan())
                                                .hompUrl(company.hompUrl())
                                                .calTel(company.calTel())
                                                .financialGroupType(company.financialGroupType())
                                                .build();
                            }
                            this.financialCompanyRepository.save(financialCompanyEntity);
                        });
    }
}
