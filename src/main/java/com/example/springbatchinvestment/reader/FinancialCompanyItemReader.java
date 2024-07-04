package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.client.FssClient;
import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.client.dto.CompanyArea;
import com.example.springbatchinvestment.client.dto.CompanyResult;
import com.example.springbatchinvestment.client.dto.FssResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

@Slf4j
public class FinancialCompanyItemReader
        extends AbstractFinancialItemReader<Company, CompanyArea, Company, CompanyResult> {

    public FinancialCompanyItemReader(final String baseUrl, final String authKey) {
        super(new FssClient(baseUrl, authKey));
    }

    @Override
    protected FssResponse<CompanyResult> fetchDataFromClient() {
        return super.fssClient.getCompanies(
                super.currentFinancialGroupType.getFinancialGroupCode(),
                String.valueOf(super.currentPage),
                null);
    }

    @Override
    protected List<Company> getItems(List<Company> baseList, List<CompanyArea> optionList) {
        return baseList.stream()
                .map(company -> company.addFinancialGroupType(super.currentFinancialGroupType))
                .toList();
    }
}
