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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CustomDepositOptionItemProcessor implements ItemProcessor<List<DepositDto>, List<DepositOption>> {

    private final DepositRepository depositRepository;



    @Override
    public List<DepositOption> process(List<DepositDto> items) throws Exception {

        List<DepositOption> depositOptions = new ArrayList<>();

        items.stream().forEach(depositDto -> depositDto.getOptions().stream().forEach(depositOptionDto ->
                depositOptions.add(depositOptionDto.toEntity(depositRepository.getReferenceById(new DepositPK(depositOptionDto.getFinCoNo(), depositOptionDto.getFinPrdtCd()))))));


        return depositOptions;
    }
}