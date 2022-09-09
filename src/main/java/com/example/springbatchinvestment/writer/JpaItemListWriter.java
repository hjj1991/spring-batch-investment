package com.example.springbatchinvestment.writer;

import org.springframework.batch.item.database.JpaItemWriter;

import java.util.List;
import java.util.stream.Collectors;

public class JpaItemListWriter<T> extends JpaItemWriter<List<T>> {
    private JpaItemWriter<T> jpaItemWriter;

    public JpaItemListWriter(JpaItemWriter<T> jpaItemWriter){
        this.jpaItemWriter = jpaItemWriter;
    }

    @Override
    public void write(List<? extends List<T>> items) {
        jpaItemWriter.write(items.stream().flatMap(innerArray -> innerArray.stream()).collect(Collectors.toList()));
    }
}
