package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.dto.SavingDto;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomSavingJdbcItemWriter implements ItemWriter<List<SavingDto>> {

    private final DataSource dataSource;
    private final JdbcBatchItemWriter<SavingDto> jdbcBatchItemWriter;

    public CustomSavingJdbcItemWriter(DataSource dataSource, JdbcBatchItemWriter jdbcBatchItemWriter){
        this.dataSource = dataSource;
        this.jdbcBatchItemWriter = jdbcBatchItemWriter;
    }

    @Override
    public void write(List<? extends List<SavingDto>> items) throws Exception {
        List<SavingDto> savingDtos = items.stream().flatMap(Collection::stream).collect(Collectors.toList());


        String sql = "INSERT INTO tb_saving (fin_co_no, fin_prdt_cd, dcls_end_day, dcls_month, dcls_strt_day, enable, etc_note, fin_co_subm_day, fin_prdt_nm, " +
                "join_deny, join_member, join_way, kor_co_nm, max_limit, mtrt_int, spcl_cnd, created_date, last_modified_date) values " +
                "(:finCoNo, :finPrdtCd, :dclsEndDay, :dclsMonth, :dclsStrtDay, " +
                "1, :etcNote, :finCoSubmDay, :finPrdtNm, :joinDeny, :joinMember, :joinWay, :korCoNm, :maxLimit, :mtrtInt, :spclCnd, now(), now()) " +
                "ON DUPLICATE KEY UPDATE " +
                "dcls_end_day = :dclsEndDay," +
                "dcls_month = :dclsMonth, " +
                "dcls_strt_day = :dclsStrtDay, " +
                "enable = 1, " +
                "etc_note = :etcNote, " +
                "fin_co_subm_day = :finCoSubmDay, " +
                "fin_prdt_nm = :finPrdtNm, " +
                "join_deny = :joinDeny, " +
                "join_member = :joinMember, " +
                "join_way = :joinWay, " +
                "kor_co_nm = :korCoNm, " +
                "max_limit = :maxLimit, " +
                "mtrt_int = :mtrtInt, " +
                "spcl_cnd = :spclCnd, " +
                "last_modified_date = now()";

        jdbcBatchItemWriter.setDataSource(dataSource);
        jdbcBatchItemWriter.setJdbcTemplate(new NamedParameterJdbcTemplate(dataSource));
        jdbcBatchItemWriter.setSql(sql);
        jdbcBatchItemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider());
        jdbcBatchItemWriter.afterPropertiesSet();
        jdbcBatchItemWriter.write(savingDtos);


    }
}
