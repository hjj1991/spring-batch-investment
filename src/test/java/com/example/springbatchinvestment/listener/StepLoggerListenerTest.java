package com.example.springbatchinvestment.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

class StepLoggerListenerTest {

    private final StepExecutionLoggerListener stepExecutionLoggerListener =
            new StepExecutionLoggerListener();
    private final ChunkLoggerListener chunkLoggerListener = new ChunkLoggerListener();
    private final ItemReadLoggerListener itemReadLoggerListener = new ItemReadLoggerListener();
    private final ItemWriteLoggerListener itemWriteLoggerListener = new ItemWriteLoggerListener();
    private final SkipLoggerListener skipLoggerListener = new SkipLoggerListener();

    @Test
    void afterStep은_전달받은_exitStatus를_그대로_반환한다() {
        StepExecution stepExecution = mock(StepExecution.class);

        when(stepExecution.getStepName()).thenReturn("TEST_STEP");
        when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getReadCount()).thenReturn(10L);
        when(stepExecution.getWriteCount()).thenReturn(10L);
        when(stepExecution.getReadSkipCount()).thenReturn(0L);
        when(stepExecution.getWriteSkipCount()).thenReturn(0L);
        when(stepExecution.getRollbackCount()).thenReturn(0L);
        when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        ExitStatus result = this.stepExecutionLoggerListener.afterStep(stepExecution);

        assertThat(result).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void 분리된_리스너_콜백들은_예외없이_동작한다() {
        StepExecution stepExecution = mock(StepExecution.class);
        Chunk<Object> outputs = new Chunk<>(List.of("a", "b"));
        Chunk<Object> writeItems = new Chunk<>(List.of("x"));

        when(stepExecution.getStepName()).thenReturn("TEST_STEP");

        assertDoesNotThrow(() -> this.stepExecutionLoggerListener.beforeStep(stepExecution));
        assertDoesNotThrow(
                () -> this.chunkLoggerListener.onChunkError(new RuntimeException("chunk"), outputs));
        assertDoesNotThrow(() -> this.itemReadLoggerListener.onReadError(new RuntimeException("read")));
        assertDoesNotThrow(
                () -> this.itemWriteLoggerListener.onWriteError(new RuntimeException("write"), writeItems));
        assertDoesNotThrow(() -> this.skipLoggerListener.onSkipInRead(new RuntimeException("skip-read")));
        assertDoesNotThrow(
                () -> this.skipLoggerListener.onSkipInWrite("item", new RuntimeException("skip-write")));
        assertDoesNotThrow(
                () -> this.skipLoggerListener.onSkipInProcess("item", new RuntimeException("skip-process")));
    }
}
