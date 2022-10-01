package com.example.springbatchinvestment;

import com.example.springbatchinvestment.domain.dto.BankDto;
import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.dto.SavingDto;
import com.example.springbatchinvestment.domain.entity.Bank;
import com.example.springbatchinvestment.domain.entity.DepositOption;
import com.example.springbatchinvestment.processor.CustomBankItemProcessor;
import com.example.springbatchinvestment.reader.CustomBankItemReader;
import com.example.springbatchinvestment.reader.CustomDepositItemReader;
import com.example.springbatchinvestment.reader.CustomSavingItemReader;
import com.example.springbatchinvestment.repository.*;
import com.example.springbatchinvestment.writer.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BachConfig {

    private final WebClient webClient;
    private final ModelMapper modelMapper;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory em;
    private final BankRepository bankRepository;
    private final SavingRepository savingRepository;
    private final SavingOptionRepository savingOptionRepository;
    private final DepositRepository depositRepository;
    private final DepositOptionRepository depositOptionRepository;
    private final DataSource dataSource;

    @Bean
    public Job bankSyncJob(){
        return jobBuilderFactory.get("bankSyncJob")
                .incrementer(new RunIdIncrementer())    /* 여러번 호출할 수 있도록 RunIdIncrementer 메서드를 사용하여 중복되지 않게 실행 */
                .start(bankInitStep())                  /* 기존 은행목록들의 사용여부를 0으로 바꿔주는 Step */
                .next(bankSyncStep())                   /* 금융감독원 OPEN API를 이용하여 동기화하는 Step */
                .build();
    }

    @Bean
    public Job depositSyncJob() {
        return jobBuilderFactory.get("depositSyncJob")
                .incrementer(new RunIdIncrementer())
                .start(depositInitStep())                /* 기존 예금목록들의 사용여부를 0으로 바꿔주는 Step */
                .next(depositSyncStep())                 /* 금융감독원 OPEN API를 이용하여 동기화하는 Step */
                .build();
    }

    @Bean
    public Job savingSyncJob(){
        return jobBuilderFactory.get("savingSyncJob")
                .incrementer(new RunIdIncrementer())
                .start(savingInitStep())    /* 기존 적금목록들의 사용여부를 0으로 바꿔주는 Step */
                .next(savingSyncStep())     /* 금융감독원 OPEN API를 이용하여 동기화하는 Step */
                .build();
    }

    @Bean
    public Step savingSyncStep() {
        return stepBuilderFactory.get("savingSyncStep")
                .<List<SavingDto>, List<SavingDto>>chunk(1)
                .reader(savingItemReader())
                .writer(compositeSavingItemWriter())
                .build();
    }

    @Bean
    public CompositeItemWriter compositeSavingItemWriter() {
        List<ItemWriter> delegates = new ArrayList<>(2);
        delegates.add(new CustomSavingJdbcItemWriter(dataSource, new JdbcBatchItemWriter()));
        delegates.add(new CustomSavingOptionJdbcItemWriter(dataSource, new JdbcBatchItemWriter()));

        CompositeItemWriter compositeItemWriter = new CompositeItemWriter();
        compositeItemWriter.setDelegates(delegates);
        return compositeItemWriter;
    }

    @Bean
    public ItemReader<List<SavingDto>> savingItemReader() {
        return new CustomSavingItemReader(webClient, modelMapper);
    }


    @Bean
    /* 기존 은행목록들의 사용여부를 0으로 바꿔주는 Step */
    public Step bankInitStep(){
        return stepBuilderFactory.get("bankInitStep")
                .tasklet((contribution, chunkContext) -> {
                    bankRepository.updateBankAllEnable(0);
                    return RepeatStatus.FINISHED;
                }).build();

    }
    @Bean
    public Step savingInitStep(){
        return stepBuilderFactory.get("savingInitStep")
                .tasklet((contribution, chunkContext) -> {
                    savingRepository.updateAllSavingEnable(0);
                    savingOptionRepository.truncateSavingOption();
                    return RepeatStatus.FINISHED;
                }).build();
    }

    @Bean
    public Step depositInitStep(){
        return stepBuilderFactory.get("depositInitStep")
                .tasklet((contribution, chunkContext) -> {
                    depositRepository.updateAllDepositEnable(0);
                    depositOptionRepository.truncateDepositOption();
                    return RepeatStatus.FINISHED;
                }).build();
    }

    @Bean
    public Step bankSyncStep(){
        return stepBuilderFactory.get("bankSyncStep")
                .<List<BankDto>, List<Bank>>chunk(1)
                .reader(bankItemReader())
                .processor(bankItemProcessor())
                .writer(bankItemWriterList())
                .build();
    }


    @Bean
    public Step depositSyncStep(){
        return stepBuilderFactory.get("depositSyncStep")
                .<List<DepositDto>, List<DepositDto>>chunk(1)
                .reader(depositItemReader())
                .writer(compositeDepositItemWriter())
                .build();
    }

    public ItemWriter<List<DepositOption>> depositOptionItemWriterList() {
        JpaItemWriter<DepositOption> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(em);
        return new JpaItemListWriter<>(writer);
    }



    public JpaItemListWriter<Bank> bankItemWriterList() {
        JpaItemWriter<Bank> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(em);
        return new JpaItemListWriter<>(writer);
    }

    @Bean
    public ItemProcessor<List<BankDto>, List<Bank>> bankItemProcessor() {
        return new CustomBankItemProcessor();
    }


    @Bean
    public ItemReader<List<BankDto>> bankItemReader() {
        return new CustomBankItemReader(webClient, modelMapper);
    }



    @Bean
    public CompositeItemWriter compositeDepositItemWriter() {
        List<ItemWriter> delegates = new ArrayList<>(2);
        delegates.add(new CustomDepositJdbcItemWriter(dataSource, new JdbcBatchItemWriter()));
        delegates.add(new CustomDepositOptionJdbcItemWriter(dataSource, new JdbcBatchItemWriter()));

        CompositeItemWriter compositeItemWriter = new CompositeItemWriter();
        compositeItemWriter.setDelegates(delegates);
        return compositeItemWriter;
    }



    @Bean
    public ItemReader<List<DepositDto>> depositItemReader() {
        return new CustomDepositItemReader(webClient, modelMapper);
    }

}
