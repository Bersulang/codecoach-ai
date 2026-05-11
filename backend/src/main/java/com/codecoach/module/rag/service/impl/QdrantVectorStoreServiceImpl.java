package com.codecoach.module.rag.service.impl;

import com.codecoach.module.rag.config.RagProperties;
import com.codecoach.module.rag.exception.VectorStoreException;
import com.codecoach.module.rag.model.VectorSearchRequest;
import com.codecoach.module.rag.model.VectorSearchResult;
import com.codecoach.module.rag.model.VectorUpsertRequest;
import com.codecoach.module.rag.service.VectorStoreService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(prefix = "rag.vector-store", name = "provider", havingValue = "qdrant", matchIfMissing = true)
public class QdrantVectorStoreServiceImpl implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreServiceImpl.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private static final String DISTANCE_COSINE = "Cosine";

    private final RagProperties ragProperties;

    private final RestClient restClient;

    public QdrantVectorStoreServiceImpl(RagProperties ragProperties) {
        this.ragProperties = ragProperties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public void ensureCollection() {
        long startTime = System.currentTimeMillis();
        String operation = "ensureCollection";
        try {
            QdrantConfig config = getConfig();
            if (collectionExists(config)) {
                log.debug("Qdrant collection already exists, collection={}", config.collection());
                return;
            }

            restClient.put()
                    .uri(collectionEndpoint(config))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateCollectionRequest(new VectorParams(config.dimensions(), DISTANCE_COSINE)))
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Qdrant collection created, collection={}, dimensions={}, distance={}, latencyMs={}",
                    config.collection(),
                    config.dimensions(),
                    DISTANCE_COSINE,
                    System.currentTimeMillis() - startTime
            );
        } catch (VectorStoreException exception) {
            logFailure(operation, exception, startTime);
            throw exception;
        } catch (RestClientResponseException exception) {
            VectorStoreException vectorStoreException = toVectorStoreException(operation, exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (ResourceAccessException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_UNAVAILABLE", "QDRANT_UNAVAILABLE", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (RestClientException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_REQUEST_FAILED", "QDRANT_REQUEST_FAILED", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        }
    }

    @Override
    public void upsert(VectorUpsertRequest request) {
        long startTime = System.currentTimeMillis();
        String operation = "upsert";
        try {
            QdrantConfig config = getConfig();
            validateVectorId(request == null ? null : request.getVectorId());
            validateVector(request == null ? null : request.getVector(), config.dimensions());

            QdrantPoint point = new QdrantPoint(request.getVectorId(), request.getVector(), request.getPayload());
            restClient.put()
                    .uri(pointsEndpoint(config))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new UpsertPointsRequest(List.of(point)))
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Qdrant vector upserted, collection={}, vectorId={}, payloadKeys={}, latencyMs={}",
                    config.collection(),
                    request.getVectorId(),
                    request.getPayload() == null ? List.of() : request.getPayload().keySet(),
                    System.currentTimeMillis() - startTime
            );
        } catch (VectorStoreException exception) {
            logFailure(operation, exception, startTime);
            throw exception;
        } catch (RestClientResponseException exception) {
            VectorStoreException vectorStoreException = toVectorStoreException(operation, exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (ResourceAccessException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_UNAVAILABLE", "QDRANT_UNAVAILABLE", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (RestClientException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_REQUEST_FAILED", "QDRANT_REQUEST_FAILED", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        }
    }

    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) {
        long startTime = System.currentTimeMillis();
        String operation = "search";
        try {
            QdrantConfig config = getConfig();
            validateVector(request == null ? null : request.getVector(), config.dimensions());

            int topK = request.getTopK() == null || request.getTopK() <= 0
                    ? defaultTopK()
                    : request.getTopK();
            QdrantSearchRequest qdrantRequest = new QdrantSearchRequest(
                    request.getVector(),
                    topK,
                    true,
                    buildFilter(request.getFilter())
            );
            QdrantSearchResponse response = restClient.post()
                    .uri(searchEndpoint(config))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(qdrantRequest)
                    .retrieve()
                    .body(QdrantSearchResponse.class);
            List<VectorSearchResult> results = toSearchResults(response);
            log.info(
                    "Qdrant vector searched, collection={}, topK={}, resultCount={}, filterKeys={}, latencyMs={}",
                    config.collection(),
                    topK,
                    results.size(),
                    request.getFilter() == null ? List.of() : request.getFilter().keySet(),
                    System.currentTimeMillis() - startTime
            );
            return results;
        } catch (VectorStoreException exception) {
            logFailure(operation, exception, startTime);
            throw exception;
        } catch (RestClientResponseException exception) {
            VectorStoreException vectorStoreException = toVectorStoreException(operation, exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (ResourceAccessException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_UNAVAILABLE", "QDRANT_UNAVAILABLE", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (RestClientException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_REQUEST_FAILED", "QDRANT_REQUEST_FAILED", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        }
    }

    @Override
    public void delete(String vectorId) {
        long startTime = System.currentTimeMillis();
        String operation = "delete";
        try {
            QdrantConfig config = getConfig();
            validateVectorId(vectorId);
            restClient.post()
                    .uri(deleteEndpoint(config))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DeletePointsRequest(List.of(vectorId)))
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Qdrant vector deleted, collection={}, vectorId={}, latencyMs={}",
                    config.collection(),
                    vectorId,
                    System.currentTimeMillis() - startTime
            );
        } catch (VectorStoreException exception) {
            logFailure(operation, exception, startTime);
            throw exception;
        } catch (RestClientResponseException exception) {
            VectorStoreException vectorStoreException = toVectorStoreException(operation, exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (ResourceAccessException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_UNAVAILABLE", "QDRANT_UNAVAILABLE", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        } catch (RestClientException exception) {
            VectorStoreException vectorStoreException = new VectorStoreException("QDRANT_REQUEST_FAILED", "QDRANT_REQUEST_FAILED", exception);
            logFailure(operation, vectorStoreException, startTime);
            throw vectorStoreException;
        }
    }

    private boolean collectionExists(QdrantConfig config) {
        try {
            restClient.get()
                    .uri(collectionEndpoint(config))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return false;
            }
            throw toVectorStoreException("collectionExists", exception);
        }
    }

    private QdrantFilter buildFilter(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        List<QdrantMatchCondition> must = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            must.add(new QdrantMatchCondition(entry.getKey(), new QdrantMatch(entry.getValue())));
        }
        return must.isEmpty() ? null : new QdrantFilter(must);
    }

    private List<VectorSearchResult> toSearchResults(QdrantSearchResponse response) {
        if (response == null || response.getResult() == null) {
            return List.of();
        }
        return response.getResult().stream()
                .map(item -> {
                    VectorSearchResult result = new VectorSearchResult();
                    result.setVectorId(item.getId() == null ? null : String.valueOf(item.getId()));
                    result.setScore(item.getScore());
                    result.setPayload(item.getPayload());
                    return result;
                })
                .toList();
    }

    private QdrantConfig getConfig() {
        RagProperties.VectorStoreProperties vectorStore = ragProperties.getVectorStore();
        if (vectorStore == null) {
            throw new VectorStoreException("CONFIG_MISSING", "vectorStore config missing");
        }
        if (!StringUtils.hasText(vectorStore.getUrl())) {
            throw new VectorStoreException("CONFIG_MISSING", "qdrant url missing");
        }
        if (!StringUtils.hasText(vectorStore.getCollection())) {
            throw new VectorStoreException("CONFIG_MISSING", "qdrant collection missing");
        }
        Integer dimensions = ragProperties.getEmbedding() == null ? null : ragProperties.getEmbedding().getDimensions();
        if (dimensions == null || dimensions <= 0) {
            throw new VectorStoreException("CONFIG_MISSING", "embedding dimensions missing");
        }
        return new QdrantConfig(trimTrailingSlash(vectorStore.getUrl()), vectorStore.getCollection(), dimensions);
    }

    private void validateVectorId(String vectorId) {
        if (!StringUtils.hasText(vectorId)) {
            throw new VectorStoreException("INVALID_VECTOR_ID", "vectorId must not be blank");
        }
    }

    private void validateVector(List<Float> vector, Integer expectedDimensions) {
        if (vector == null || vector.isEmpty()) {
            throw new VectorStoreException("EMPTY_VECTOR", "vector must not be empty");
        }
        if (expectedDimensions != null && vector.size() != expectedDimensions) {
            throw new VectorStoreException(
                    "DIMENSION_MISMATCH",
                    "DIMENSION_MISMATCH: expected=" + expectedDimensions + ", actual=" + vector.size()
            );
        }
    }

    private VectorStoreException toVectorStoreException(String operation, RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        String message = parseQdrantErrorMessage(responseBody);
        return new VectorStoreException(
                "QDRANT_HTTP_ERROR",
                operation + ": " + message,
                exception.getStatusCode().value(),
                responseBody,
                exception
        );
    }

    private String parseQdrantErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "Qdrant request failed";
        }
        try {
            JsonNode root = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(responseBody);
            JsonNode status = root.path("status");
            if (!status.isMissingNode() && !status.isNull()) {
                return status.asText();
            }
            JsonNode result = root.path("result");
            if (!result.isMissingNode() && !result.isNull()) {
                return result.asText();
            }
        } catch (Exception ignored) {
            // Use abbreviated raw response below.
        }
        return abbreviate(responseBody);
    }

    private void logFailure(String operation, VectorStoreException exception, long startTime) {
        QdrantConfig safeConfig = safeConfig();
        log.warn(
                "Qdrant operation failed, operation={}, collection={}, errorCode={}, statusCode={}, latencyMs={}, errorMessage={}, rawResponse={}",
                operation,
                safeConfig == null ? null : safeConfig.collection(),
                exception.getErrorCode(),
                exception.getStatusCode(),
                System.currentTimeMillis() - startTime,
                abbreviate(exception.getErrorMessage()),
                abbreviate(exception.getRawResponse()),
                exception
        );
    }

    private QdrantConfig safeConfig() {
        try {
            return getConfig();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private int defaultTopK() {
        return ragProperties.getTopK() == null || ragProperties.getTopK() <= 0 ? 5 : ragProperties.getTopK();
    }

    private String collectionEndpoint(QdrantConfig config) {
        return config.url() + "/collections/" + config.collection();
    }

    private String pointsEndpoint(QdrantConfig config) {
        return collectionEndpoint(config) + "/points?wait=true";
    }

    private String searchEndpoint(QdrantConfig config) {
        return collectionEndpoint(config) + "/points/search";
    }

    private String deleteEndpoint(QdrantConfig config) {
        return collectionEndpoint(config) + "/points/delete?wait=true";
    }

    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500);
    }

    private record QdrantConfig(String url, String collection, Integer dimensions) {
    }

    private record CreateCollectionRequest(VectorParams vectors) {
    }

    private record VectorParams(Integer size, String distance) {
    }

    private record UpsertPointsRequest(List<QdrantPoint> points) {
    }

    private record QdrantPoint(String id, List<Float> vector, Map<String, Object> payload) {
    }

    private record QdrantSearchRequest(
            List<Float> vector,
            Integer limit,
            @JsonProperty("with_payload") Boolean withPayload,
            QdrantFilter filter
    ) {
    }

    private record QdrantFilter(List<QdrantMatchCondition> must) {
    }

    private record QdrantMatchCondition(String key, QdrantMatch match) {
    }

    private record QdrantMatch(Object value) {
    }

    private record DeletePointsRequest(List<String> points) {
    }

    public static class QdrantSearchResponse {

        private List<QdrantScoredPoint> result;

        public List<QdrantScoredPoint> getResult() {
            return result;
        }

        public void setResult(List<QdrantScoredPoint> result) {
            this.result = result;
        }
    }

    public static class QdrantScoredPoint {

        private Object id;

        private Double score;

        private Map<String, Object> payload;

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }
    }
}
