package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.entity.Deposit;
import com.example.springbatchinvestment.repository.BankRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;

import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;

public class CustomDepositItemWriter implements ItemWriter<List<DepositDto>>{

    private final BankRepository bankRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final JpaItemWriter<Deposit> jpaItemWriter;

    public CustomDepositItemWriter(BankRepository bankRepository, EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
        this.bankRepository = bankRepository;
        this.jpaItemWriter = new JpaItemWriter<>();
    }

    @Override
    public void write(List<? extends List<DepositDto>> items) {
        List<DepositDto> depositDtos = new ArrayList<>();
        for (List<DepositDto> item : items) {
            depositDtos.addAll(item);
        }
        List<Deposit> deposits = new ArrayList<>();
        depositDtos.stream().forEach(depositDto -> deposits.add(depositDto.toEntity(bankRepository.getReferenceById(depositDto.getFinCoNo()))));

        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        jpaItemWriter.write(deposits);
    }
}
