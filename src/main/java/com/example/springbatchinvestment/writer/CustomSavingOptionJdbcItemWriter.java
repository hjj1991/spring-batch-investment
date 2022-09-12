package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.dto.SavingDto;
import com.example.springbatchinvestment.domain.dto.SavingOptionDto;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomSavingOptionJdbcItemWriter implements ItemWriter<List<SavingDto>> {

    private final DataSource dataSource;
    private final JdbcBatchItemWriter jdbcBatchItemWriter;

    public CustomSavingOptionJdbcItemWriter(DataSource dataSource, JdbcBatchItemWriter jdbcBatchItemWriter){
        this.dataSource = dataSource;
        this.jdbcBatchItemWriter = jdbcBatchItemWriter;
    }

    @Override
    public void write(List<? extends List<SavingDto>> items) throws Exception {

        List<SavingOptionDto> savingOptionDtos = items.stream().flatMap(Collection::stream).map(SavingDto::getOptions)
                .flatMap(Collection::stream).collect(Collectors.toList());


        String sql = "INSERT INTO tb_saving_option " +
                "(fin_co_no, fin_prdt_cd, save_trm, intr_rate_type_nm, intr_rate_type, intr_rate2, intr_rate, dcls_month, rsrv_type, rsrv_type_nm, created_Date, last_modified_date)" +
                " values (:finCoNo, :finPrdtCd, :saveTrm, :intrRateTypeNm, :intrRateType, :intrRate2, :intrRate, :dclsMonth, :rsrvType, :rsrvTypeNm, now(), now())";

        jdbcBatchItemWriter.setDataSource(dataSource);
        jdbcBatchItemWriter.setJdbcTemplate(new NamedParameterJdbcTemplate(dataSource));
        jdbcBatchItemWriter.setSql(sql);
        jdbcBatchItemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider());
        jdbcBatchItemWriter.afterPropertiesSet();
        jdbcBatchItemWriter.write(savingOptionDtos);
    }
}
