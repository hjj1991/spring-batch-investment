package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.*;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import com.example.springbatchinvestment.service.embedding.EmbeddingService;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FinancialProductItemWriter implements ItemWriter<FinancialProductModel> {

    private static final String LINE_BREAK = "\n";
    private static final String SINGLE_SPACE = " ";
    private static final String MULTI_SPACE_REGEX = "\\s+";
    private static final String EMBEDDING_TEMPLATE =
            "%s에서 제공하는 '%s' 상품입니다. "
                    + "주요 우대조건은 '%s'이며, 가입은 주로 %s 방식으로 가능합니다. "
                    + "만기 후 이자율은 %s 입니다. "
                    + "기타 참고사항으로는 '%s' 내용이 있습니다.";

    private final FinancialProductRepository financialProductRepository;
    private final FinancialCompanyRepository financialCompanyRepository;
    private final EmbeddingService embeddingService;
    private final boolean embeddingEnabled;
    private ZonedDateTime runStartedAt;

    public FinancialProductItemWriter(
            FinancialProductRepository financialProductRepository,
            FinancialCompanyRepository financialCompanyRepository,
            EmbeddingService embeddingService,
            @Value("${api.gemini.embedding-enabled:false}") boolean embeddingEnabled) {
        this.financialProductRepository = financialProductRepository;
        this.financialCompanyRepository = financialCompanyRepository;
        this.embeddingService = embeddingService;
        this.embeddingEnabled = embeddingEnabled;
    }

    @BeforeStep
    public void captureRunStartedAt(StepExecution stepExecution) {
        this.runStartedAt = stepExecution.getJobExecution().getCreateTime().atZone(ZoneOffset.UTC);
    }

    @Override
    public void write(Chunk<? extends FinancialProductModel> chunk) throws Exception {
        chunk
                .getItems()
                .forEach(
                        financialProductModel -> {
                            Optional<FinancialProductEntity> optionalFinancialProductEntity =
                                    this.financialProductRepository
                                            .findByFinancialCompanyEntityFinancialCompanyCodeAndFinancialProductCodeAndFinancialProductType(
                                                    financialProductModel.finCoNo(),
                                                    financialProductModel.finPrdtCd(),
                                                    financialProductModel.financialProductType());
                            FinancialProductEntity financialProductEntity;
                            String newContentHash = financialProductModel.generateContentHash();
                            String embeddingText = this.createEmbeddingText(financialProductModel);

                            if (optionalFinancialProductEntity.isPresent()) {
                                financialProductEntity = optionalFinancialProductEntity.get();
                                financialProductEntity.updateByProduct(financialProductModel);
                                financialProductEntity.updateLastSeenAt(this.runStartedAt);

                                if (!newContentHash.equals(financialProductEntity.getProductContentHash())) {
                                    financialProductEntity.updateProductContentHash(newContentHash);
                                    financialProductEntity.updateEmbeddingVector(
                                            this.createEmbeddingVector(embeddingText));
                                }
                            } else {
                                financialProductEntity =
                                        FinancialProductEntity.builder()
                                                .financialCompanyEntity(
                                                        this.financialCompanyRepository
                                                                .findByFinancialCompanyCode(financialProductModel.finCoNo())
                                                                .orElseThrow(
                                                                        () ->
                                                                                new IllegalArgumentException(
                                                                                        "financialCompanyEntity not found")))
                                                .financialProductCode(financialProductModel.finPrdtCd())
                                                .financialProductType(financialProductModel.financialProductType())
                                                .status(ProductStatus.ACTIVE)
                                                .build();
                                financialProductEntity.updateByProduct(financialProductModel);
                                financialProductEntity.updateLastSeenAt(this.runStartedAt);
                                financialProductEntity.updateProductContentHash(newContentHash);
                                financialProductEntity.updateEmbeddingVector(
                                        this.createEmbeddingVector(embeddingText));
                            }
                            this.financialProductRepository.save(financialProductEntity);
                        });
    }

    private float[] createEmbeddingVector(String embeddingText) {
        if (!this.embeddingEnabled) {
            return null;
        }
        return this.embeddingService.embed(embeddingText);
    }

    private String createEmbeddingText(FinancialProductModel item) {
        // 1. 데이터 정제
        String productName = item.finPrdtNm().trim();
        String companyName = item.korCoNm().trim();
        String specialCondition =
                item.spclCnd() != null
                        ? item.spclCnd()
                                .replace(LINE_BREAK, SINGLE_SPACE)
                                .replaceAll(MULTI_SPACE_REGEX, SINGLE_SPACE)
                                .trim()
                        : "";
        String joinWay = item.joinWay() != null ? item.joinWay().trim() : "";
        String postMaturityInterestRate =
                item.mtrtInt() != null
                        ? item.mtrtInt()
                                .replace(LINE_BREAK, SINGLE_SPACE)
                                .replaceAll(MULTI_SPACE_REGEX, SINGLE_SPACE)
                                .trim()
                        : "";
        String etcNote =
                item.etcNote() != null
                        ? item.etcNote()
                                .replace(LINE_BREAK, SINGLE_SPACE)
                                .replaceAll(MULTI_SPACE_REGEX, SINGLE_SPACE)
                                .trim()
                        : "";

        // 2. 자연스러운 문장으로 조합
        return String.format(
                EMBEDDING_TEMPLATE,
                companyName, productName, specialCondition, joinWay, postMaturityInterestRate, etcNote);
    }
}
