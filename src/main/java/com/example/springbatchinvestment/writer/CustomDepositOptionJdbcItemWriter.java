package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.dto.DepositOptionDto;
import com.example.springbatchinvestment.domain.entity.DepositOption;
import com.example.springbatchinvestment.domain.entity.DepositPK;
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

public class CustomDepositOptionJdbcItemWriter implements ItemWriter<List<DepositDto>>{


    private final JdbcBatchItemWriter jdbcBatchItemWriter;
    private final DataSource dataSource;

    public CustomDepositOptionJdbcItemWriter(DataSource dataSource, JdbcBatchItemWriter jdbcBatchItemWriter) {

        this.jdbcBatchItemWriter = jdbcBatchItemWriter;
        this.dataSource = dataSource;
    }
    @Override
    public void write(List<? extends List<DepositDto>> items) throws Exception {
        List<DepositDto> depositDtos = new ArrayList<>();
        for (List<DepositDto> item : items) {
            depositDtos.addAll(item);
        }

        List<DepositOptionDto> depositOptionDtos = new ArrayList<>();


        depositDtos.stream().forEach(depositDto -> depositOptionDtos.addAll(depositDto.getOptions()));



        String sql = "INSERT INTO tb_deposit_option " +
                "(fin_co_no, fin_prdt_cd, save_trm, intr_rate_type_nm, intr_rate_type, intr_rate2, intr_rate, dcls_month, created_Date, last_modified_date)" +
                " values (:finCoNo, :finPrdtCd, :saveTrm, :intrRateTypeNm, :intrRateType, :intrRate2, :intrRate, :dclsMonth, now(), now())";

        jdbcBatchItemWriter.setDataSource(dataSource);
        jdbcBatchItemWriter.setJdbcTemplate(new NamedParameterJdbcTemplate(dataSource));
        jdbcBatchItemWriter.setSql(sql);
        jdbcBatchItemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider());
        jdbcBatchItemWriter.afterPropertiesSet();
        jdbcBatchItemWriter.write(depositOptionDtos);

    }
}
