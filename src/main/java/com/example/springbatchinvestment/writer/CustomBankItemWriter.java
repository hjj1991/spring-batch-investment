package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.entity.Bank;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class CustomBankItemWriter implements ItemWriter<Bank> {

    @Override
    public void write(List<? extends Bank> items) throws Exception {

    }
}
