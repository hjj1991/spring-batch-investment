package com.example.springbatchinvestment.processor;

import com.example.springbatchinvestment.domain.dto.BankDto;
import com.example.springbatchinvestment.domain.entity.Bank;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;
import java.util.stream.Collectors;

public class CustomBankItemProcessor implements ItemProcessor<List<BankDto>, List<Bank>> {

    @Override
    public List<Bank> process(List<BankDto> items)  {
        return items.stream().map(BankDto::toEntity).collect(Collectors.toList());
    }
}
