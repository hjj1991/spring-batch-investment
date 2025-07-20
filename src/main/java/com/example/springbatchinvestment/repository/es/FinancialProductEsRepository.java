package com.example.springbatchinvestment.repository.es;

import com.example.springbatchinvestment.domain.es.FinancialProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FinancialProductEsRepository
        extends ElasticsearchRepository<FinancialProductDocument, String> {}
