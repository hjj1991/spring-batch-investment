package com.example.springbatchinvestment.processor;

import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.entity.Deposit;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

public class CustomDepositItemProcessor implements ItemProcessor<List<DepositDto>, List<Deposit>>{


    @Override
    public List<Deposit> process(List<DepositDto> item) throws Exception {
        return null;
    }
}
