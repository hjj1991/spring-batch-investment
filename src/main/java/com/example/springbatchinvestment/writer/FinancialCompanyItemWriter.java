package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.client.dto.CompanyArea;
import com.example.springbatchinvestment.domain.CompanySyncItem;
import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class FinancialCompanyItemWriter implements ItemWriter<CompanySyncItem> {

    private final FinancialCompanyRepository financialCompanyRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    public FinancialCompanyItemWriter(
            FinancialCompanyRepository financialCompanyRepository,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ObjectMapper objectMapper) {
        this.financialCompanyRepository = financialCompanyRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(Chunk<? extends CompanySyncItem> chunk) throws Exception {
        chunk
                .getItems()
                .forEach(
                        companySyncItem -> {
                            Company company = companySyncItem.company();
                            List<CompanyArea> companyAreas = companySyncItem.companyAreas();
                            Optional<FinancialCompanyEntity> optionalFinancialCompanyEntity =
                                    this.financialCompanyRepository.findByFinancialCompanyCode(company.finCoNo());

                            FinancialCompanyEntity financialCompanyEntity;
                            if (optionalFinancialCompanyEntity.isPresent()) {
                                financialCompanyEntity = optionalFinancialCompanyEntity.get();
                                financialCompanyEntity.updateByCompany(company);
                                financialCompanyEntity.updateSourcePayload(
                                        this.toJsonStringOrNull(company));
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
                                                .sourcePayload(this.toJsonStringOrNull(company))
                                                .build();
                            }
                            FinancialCompanyEntity savedCompanyEntity =
                                    this.financialCompanyRepository.save(financialCompanyEntity);
                            this.upsertCompanyAreas(savedCompanyEntity.getFinancialCompanyId(), companyAreas);
                        });
    }

    private void upsertCompanyAreas(Long financialCompanyId, List<CompanyArea> companyAreas) {
        for (CompanyArea companyArea : companyAreas) {
            OffsetDateTime currentUtcDateTime = OffsetDateTime.now(Clock.systemUTC());
            MapSqlParameterSource parameters =
                    new MapSqlParameterSource()
                            .addValue("financialCompanyId", financialCompanyId)
                            .addValue("areaCode", companyArea.areaCd())
                            .addValue("areaName", companyArea.areaNm())
                            .addValue("isAvailable", "Y".equalsIgnoreCase(companyArea.exisYn()))
                            .addValue("currentUtcDateTime", currentUtcDateTime);

            this.namedParameterJdbcTemplate.update(
                    """
                    INSERT INTO financial_company_area(
                        financial_company_id,
                        area_code,
                        area_name,
                        is_available,
                        created_at,
                        modified_at
                    ) VALUES (
                        :financialCompanyId,
                        :areaCode,
                        :areaName,
                        :isAvailable,
                        :currentUtcDateTime,
                        :currentUtcDateTime
                    )
                    ON CONFLICT (financial_company_id, area_code)
                    DO UPDATE SET
                        area_name = EXCLUDED.area_name,
                        is_available = EXCLUDED.is_available,
                        modified_at = :currentUtcDateTime
                    """,
                    parameters);
        }
    }

    private String toJsonStringOrNull(Object value) {
        try {
            return this.objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            return null;
        }
    }
}
