package com.example.springbatchinvestment;

import com.example.springbatchinvestment.domain.dto.BankDto;
import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.entity.Bank;
import com.example.springbatchinvestment.domain.entity.Deposit;
import com.example.springbatchinvestment.processor.CustomBankItemProcessor;
import com.example.springbatchinvestment.processor.CustomDepositItemProcessor;
import com.example.springbatchinvestment.reader.CustomBankItemReader;
import com.example.springbatchinvestment.reader.CustomDepositItemReader;
import com.example.springbatchinvestment.repository.BankRepository;
import com.example.springbatchinvestment.writer.JpaItemListWriter;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.persistence.EntityManagerFactory;
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

    @Bean
    public Job bankSyncJob(){
        return jobBuilderFactory.get("bankSyncJob")
                .incrementer(new RunIdIncrementer())
                .start(bankInitStep(null))
                .next(bankSyncStep())
                .build();
    }

    @Bean
    @JobScope
    /* 기존 은행목록들의 사용여부를 0으로 바꿔주는 Step */
    public Step bankInitStep(@Value("#{jobParameters['topFinGrpNo']}") String topFinGrpNo){
        return stepBuilderFactory.get("bankInitStep")
                .tasklet((contribution, chunkContext) -> {
                    bankRepository.updateBankEnable(topFinGrpNo, 0);
                    return RepeatStatus.FINISHED;
                }).build();

    }

    @Bean
    public Step bankSyncStep(){
        return stepBuilderFactory.get("bankSyncStep")
                .<List<BankDto>, List<Bank>>chunk(1)
                .reader(bankItemReader(null))
                .processor(bankItemProcessor())
                .writer(bankItemWriterList())
                .build();
    }

    @Bean
    public ItemProcessor<? super List<BankDto>, ? extends List<Bank>> bankItemProcessor() {
        return new CustomBankItemProcessor();
    }


    public JpaItemListWriter<Bank> bankItemWriterList() {
        JpaItemWriter<Bank> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(em);
        return new JpaItemListWriter<>(writer);

    }

    @Bean
    @StepScope
    public ItemReader<? extends List<BankDto>> bankItemReader(@Value("#{jobParameters['topFinGrpNo']}") String topFinGrpNo) {

        return new CustomBankItemReader(webClient, modelMapper, topFinGrpNo);
    }

    @Bean
    public Step depositSyncStep(){
        return stepBuilderFactory.get("depositSyncStep")
                .<List<DepositDto>, List<Deposit>>chunk(1)
                .reader(depositItemReader(null))
                .processor(depositItemProcessor())
                .writer(depositItemWriterList())
                .build();
    }


    private ItemWriter<? super List<Deposit>> depositItemWriterList() {
        return null;
    }

    @Bean
    public ItemProcessor<? super List<DepositDto>, ? extends List<Deposit>> depositItemProcessor() {
        return new CustomDepositItemProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<? extends List<DepositDto>> depositItemReader(@Value("#{jobParameters['topFinGrpNo']}") String topFinGrpNo) {
        return new CustomDepositItemReader(webClient, modelMapper, topFinGrpNo);

    }

}
