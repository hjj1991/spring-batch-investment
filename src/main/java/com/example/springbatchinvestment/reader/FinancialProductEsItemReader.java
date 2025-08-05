package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FinancialProductEsItemReader {

    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 10;

    @Bean
    @StepScope
    public JpaPagingItemReader<FinancialProductEntity> financialProductEsReader() {
        return new JpaPagingItemReaderBuilder<FinancialProductEntity>()
                .name("financialProductEsReader")
                .entityManagerFactory(this.entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT p FROM FinancialProductEntity p JOIN FETCH p.financialCompanyEntity")
                .build();
    }
}
