package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.dto.DepositOptionDto;
import com.example.springbatchinvestment.domain.entity.Deposit;
import com.example.springbatchinvestment.domain.entity.DepositOption;
import com.example.springbatchinvestment.domain.entity.DepositPK;
import com.example.springbatchinvestment.repository.BankRepository;
import com.example.springbatchinvestment.repository.DepositRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class CustomDepositOptionItemWriter implements ItemWriter<List<DepositDto>>{

    private final DepositRepository depositRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final JpaItemWriter<DepositOption> jpaItemWriter;

    public CustomDepositOptionItemWriter(DepositRepository depositRepository, EntityManagerFactory entityManagerFactory, JpaItemWriter jpaItemWriter) {
        this.entityManagerFactory = entityManagerFactory;
        this.depositRepository = depositRepository;
        this.jpaItemWriter = jpaItemWriter;

    }

    @Override
    public void write(List<? extends List<DepositDto>> items) throws Exception {
        List<DepositOption> depositOptions = new ArrayList<>();
        List<DepositDto> depositDtos = new ArrayList<>();
        for (List<DepositDto> item : items) {
            depositDtos.addAll(item);
        }

        depositDtos.stream().forEach(depositDto -> depositDto.getOptions().stream().forEach(depositOptionDto ->
                depositOptions.add(depositOptionDto.toEntity(depositRepository.getReferenceById(new DepositPK(depositOptionDto.getFinCoNo(), depositOptionDto.getFinPrdtCd()))))));


        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        jpaItemWriter.setUsePersist(true);
        jpaItemWriter.write(depositOptions);
    }
}
