package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.client.FssClient;
import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.client.dto.CompanyResult;
import com.example.springbatchinvestment.client.dto.FssResponse;
import com.example.springbatchinvestment.client.error.FssClientError;
import com.example.springbatchinvestment.domain.FinancialGroupType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

import java.util.List;
import java.util.Optional;

@Slf4j
public class FinancialCompanyItemReader implements ItemStreamReader<Company> {

    public FinancialCompanyItemReader(final String baseUrl, final String authKey) {
        this.fssClient = new FssClient(baseUrl, authKey);
    }

    private final FssClient fssClient;
    private int currentPage = 1;
    private Optional<Long> currentMaxPage = Optional.empty();
    private FinancialGroupType currentFinancialGroupType = FinancialGroupType.BANK;
    private List<Company> currentPageItems = null;
    private int nextIndexItem = 0;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.currentPage = executionContext.getInt("currentPage", 1);
        this.currentFinancialGroupType = FinancialGroupType.fromCode(executionContext.getString("currentFinancialGroupCode", FinancialGroupType.BANK.getFinancialGroupCode()));
        this.nextIndexItem = executionContext.getInt("nextIndexItem", 0);

    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt("currentPage", this.currentPage);
        executionContext.putString("currentFinancialGroupCode", this.currentFinancialGroupType.getFinancialGroupCode());
        executionContext.putInt("nextIndexItem", this.nextIndexItem);
    }


    @Override
    public Company read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (this.currentPageItems == null || this.nextIndexItem >= this.currentPageItems.size()) {
            if (this.shouldSwitchToNextFinancialGroup()) {
                this.switchToNextFinancialGroup();
                this.fetchData();
            } else if (this.shouldEndFinancialSync()) {
                return null;
            } else {
                this.fetchData();
            }
            if (this.shouldNextPage()) {
                this.currentPage++;
            }
        }

        if (this.nextIndexItem < this.currentPageItems.size()) {
            Company company = this.currentPageItems.get(this.nextIndexItem++);

            return new Company(
                    company.dclsMonth(),
                    company.finCoNo(),
                    company.korCoNm(),
                    company.dclsChrgMan(),
                    company.hompUrl(),
                    company.calTel(),
                    this.currentFinancialGroupType
            );
        }

        return null;
    }

    private void switchToNextFinancialGroup() {
        log.info("Switching to next financial group: {}", FinancialGroupType.SAVING_BANK);
        this.currentFinancialGroupType = FinancialGroupType.SAVING_BANK;
        this.currentPage = 1;  // Start from page 1 for the new financial group type
        this.currentMaxPage = Optional.empty();
        this.currentPageItems = null;  // Clear current page items
        this.nextIndexItem = 0;  // Reset index for new financial group
    }

    private boolean shouldNextPage() {
        return this.currentMaxPage.isPresent() && this.currentPage <= this.currentMaxPage.get().intValue();
    }

    private boolean shouldSwitchToNextFinancialGroup() {
        return this.currentMaxPage.isPresent() && this.currentPage > this.currentMaxPage.get() && this.currentFinancialGroupType.equals(FinancialGroupType.BANK);
    }

    private boolean shouldEndFinancialSync() {
        return this.currentMaxPage.isPresent() && this.currentFinancialGroupType.equals(FinancialGroupType.SAVING_BANK) && this.currentPage > this.currentMaxPage.get();
    }

    private void fetchData() {
        log.info("Fetching data for page {} of financial group {}", this.currentPage, this.currentFinancialGroupType);
        FssResponse<CompanyResult> resultFssResponse = this.fssClient.getCompanies(this.currentFinancialGroupType.getFinancialGroupCode(), String.valueOf(this.currentPage), Optional.empty());
        if (resultFssResponse.isSuccess()) {
            log.info("Fetched resultFssResponse: {} items from page {} of financial group {}", resultFssResponse, this.currentPage, this.currentFinancialGroupType);
            this.currentPageItems = resultFssResponse.result().getBaseList();
            this.currentMaxPage = resultFssResponse.result().getMaxPageNo();
        } else {
            throw new FssClientError();
        }
    }
}
