package com.example.springbatchinvestment.processor;

import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.dto.DepositOptionDto;
import com.example.springbatchinvestment.domain.entity.Deposit;
import com.example.springbatchinvestment.domain.entity.DepositOption;
import com.example.springbatchinvestment.domain.entity.DepositPK;
import com.example.springbatchinvestment.repository.BankRepository;
import com.example.springbatchinvestment.repository.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CustomDepositOptionItemProcessor implements ItemProcessor<List<DepositOptionDto>, List<DepositOption>> {

    private final DepositRepository depositRepository;



    @Override
    public List<DepositOption> process(List<DepositOptionDto> items) throws Exception {

        return items.stream().map(depositOptionDto -> depositOptionDto.toEntity(depositRepository.getReferenceById(new DepositPK(depositOptionDto.getFinCoNo(), depositOptionDto.getFinPrdtCd())))).collect(Collectors.toList());
    }
}