package com.example.springbatchinvestment.processor;

import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.entity.Deposit;
import com.example.springbatchinvestment.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CustomDepositItemProcessor implements ItemProcessor<List<DepositDto>, List<Deposit>>{

    private final BankRepository bankRepository;


    @Override
    public List<Deposit> process(List<DepositDto> items) throws Exception {

        return items.stream().map(depositDto -> depositDto.toEntity(bankRepository.getReferenceById(depositDto.getFinCoNo()))).collect(Collectors.toList());
    }
}
