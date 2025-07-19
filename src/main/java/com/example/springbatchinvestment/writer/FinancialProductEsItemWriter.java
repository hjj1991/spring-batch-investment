package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.es.FinancialProductDocument;
import com.example.springbatchinvestment.repository.es.FinancialProductEsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialProductEsItemWriter implements ItemWriter<FinancialProductDocument> {

    private final FinancialProductEsRepository financialProductEsRepository;

    @Override
    public void write(Chunk<? extends FinancialProductDocument> chunk) throws Exception {
        this.financialProductEsRepository.saveAll(chunk.getItems());
    }
}
