package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.client.FssClient;
import com.example.springbatchinvestment.client.dto.FssResponse;
import com.example.springbatchinvestment.client.dto.Result;
import com.example.springbatchinvestment.client.error.FssClientError;
import com.example.springbatchinvestment.domain.FinancialGroupType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

@Slf4j
public abstract class AbstractFinancialItemReader<T, A, U extends Result<T, A>>
        implements ItemStreamReader<T> {

    protected final FssClient fssClient;
    protected int currentPage = 1;
    protected Long currentMaxPage = null;
    protected FinancialGroupType currentFinancialGroupType = FinancialGroupType.BANK;
    protected List<T> currentPageItems = null;
    protected int nextIndexItem = 0;

    public AbstractFinancialItemReader(final FssClient fssClient) {
        this.fssClient = fssClient;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.currentPage = executionContext.getInt("currentPage", 1);
        this.currentFinancialGroupType =
                FinancialGroupType.fromCode(
                        executionContext.getString(
                                "currentFinancialGroupCode", FinancialGroupType.BANK.getFinancialGroupCode()));
        this.nextIndexItem = executionContext.getInt("nextIndexItem", 0);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt("currentPage", this.currentPage);
        executionContext.putString(
                "currentFinancialGroupCode", this.currentFinancialGroupType.getFinancialGroupCode());
        executionContext.putInt("nextIndexItem", this.nextIndexItem);
    }

    @Override
    public T read()
            throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
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
            return this.addFinancialGroupType(this.currentPageItems.get(this.nextIndexItem++));
        }

        return null;
    }

    private void switchToNextFinancialGroup() {
        log.info("Switching to next financial group: {}", FinancialGroupType.SAVING_BANK);
        this.currentFinancialGroupType = FinancialGroupType.SAVING_BANK;
        this.currentPage = 1; // Start from page 1 for the new financial group type
        this.currentMaxPage = null;
        this.currentPageItems = null; // Clear current page items
        this.nextIndexItem = 0; // Reset index for new financial group
    }

    private boolean shouldNextPage() {
        return this.currentMaxPage != null && this.currentPage <= this.currentMaxPage;
    }

    private boolean shouldSwitchToNextFinancialGroup() {
        return this.currentMaxPage != null
                && this.currentPage > this.currentMaxPage
                && this.currentFinancialGroupType.equals(FinancialGroupType.BANK);
    }

    private boolean shouldEndFinancialSync() {
        return this.currentMaxPage != null
                && this.currentFinancialGroupType.equals(FinancialGroupType.SAVING_BANK)
                && this.currentPage > this.currentMaxPage;
    }

    private void fetchData() {
        log.info(
                "Fetching data for page {} of financial group {}",
                this.currentPage,
                this.currentFinancialGroupType);
        FssResponse<U> resultFssResponse = this.fetchDataFromClient();
        if (resultFssResponse.isSuccess()) {
            log.info(
                    "Fetched resultFssResponse: {} items from page {} of financial group {}",
                    resultFssResponse,
                    this.currentPage,
                    this.currentFinancialGroupType);
            this.currentPageItems = resultFssResponse.result().getBaseList();
            this.currentMaxPage = resultFssResponse.result().getMaxPageNo();
        } else {
            throw new FssClientError();
        }
    }

    protected abstract FssResponse<U> fetchDataFromClient();

    protected abstract T addFinancialGroupType(T item);
}
