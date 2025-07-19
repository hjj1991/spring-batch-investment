package com.example.springbatchinvestment.service.embedding;

import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GoogleEmbeddingService implements EmbeddingService {

    private final PredictionServiceClient predictionServiceClient;
    private final EndpointName endpointName;

    public GoogleEmbeddingService(
            @org.springframework.beans.factory.annotation.Value("${gcp.project-id}") String gcpProjectId)
            throws IOException {
        PredictionServiceSettings predictionServiceSettings =
                PredictionServiceSettings.newBuilder()
                        .setEndpoint(String.format("%s-aiplatform.googleapis.com:443", "us-central1"))
                        .build();
        this.predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings);
        this.endpointName =
                EndpointName.of(gcpProjectId, "us-central1", "publishers/google/models/text-embedding-004");
    }

    @Override
    public List<Float> embed(String text) {
        List<Value> instances = new ArrayList<>();
        instances.add(
                Value.newBuilder()
                        .setStructValue(
                                com.google.protobuf.Struct.newBuilder()
                                        .putFields("text", Value.newBuilder().setStringValue(text).build())
                                        .build())
                        .build());

        List<Value> predictions =
                this.predictionServiceClient
                        .predict(this.endpointName, instances, Value.newBuilder().build())
                        .getPredictionsList();

        ListValue embeddings =
                predictions
                        .getFirst()
                        .getStructValue()
                        .getFieldsOrThrow("embeddings")
                        .getStructValue()
                        .getFieldsOrThrow("values")
                        .getListValue();
        return embeddings.getValuesList().stream()
                .map(value -> (float) value.getNumberValue())
                .toList();
    }
}
