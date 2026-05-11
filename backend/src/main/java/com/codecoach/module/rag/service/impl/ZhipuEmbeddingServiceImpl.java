package com.codecoach.module.rag.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.enums.AiRequestType;
import com.codecoach.module.ai.service.AiCallLogService;
import com.codecoach.module.rag.config.RagProperties;
import com.codecoach.module.rag.exception.EmbeddingException;
import com.codecoach.module.rag.model.EmbeddingResult;
import com.codecoach.module.rag.service.EmbeddingService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(prefix = "rag.embedding", name = "provider", havingValue = "zhipu", matchIfMissing = true)
public class ZhipuEmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ZhipuEmbeddingServiceImpl.class);

    private static final int SUCCESS = 1;

    private static final int FAILURE = 0;

    private static final int EMBEDDING_FAILED_CODE = 3003;

    private static final String EMBEDDING_FAILED_MESSAGE = "Embedding 调用失败，请稍后重试";

    private static final String PROMPT_VERSION = "v1";

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final RagProperties ragProperties;

    private final AiCallLogService aiCallLogService;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    public ZhipuEmbeddingServiceImpl(
            RagProperties ragProperties,
            AiCallLogService aiCallLogService,
            ObjectMapper objectMapper
    ) {
        this.ragProperties = ragProperties;
        this.aiCallLogService = aiCallLogService;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds(ragProperties));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public EmbeddingResult embed(String text) {
        long startTime = System.currentTimeMillis();
        AiCallLog callLog = buildCallLog();
        int inputLength = text == null ? 0 : text.length();

        try {
            if (!StringUtils.hasText(text)) {
                throw new EmbeddingException("EMPTY_INPUT", "Embedding 输入文本不能为空");
            }

            RagProperties.EmbeddingProperties config = getConfig();
            String endpoint = resolveEmbeddingEndpoint(config.getBaseUrl());
            ZhipuEmbeddingRequest request = new ZhipuEmbeddingRequest(
                    config.getModel(),
                    text,
                    config.getDimensions()
            );

            ResponseEntity<String> responseEntity = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            ZhipuEmbeddingResponse response = readEmbeddingResponse(
                    responseEntity.getBody(),
                    responseEntity.getStatusCode().value(),
                    getRequestId(responseEntity.getHeaders(), null)
            );
            String requestId = getRequestId(responseEntity.getHeaders(), response);
            List<Float> vector = extractVector(
                    response,
                    responseEntity.getBody(),
                    responseEntity.getStatusCode().value(),
                    requestId
            );
            validateDimensions(vector, config.getDimensions(), responseEntity.getBody(), responseEntity.getStatusCode().value(), requestId);

            EmbeddingResult result = new EmbeddingResult(
                    vector,
                    StringUtils.hasText(response.getModel()) ? response.getModel() : config.getModel(),
                    vector.size(),
                    response.getUsage() == null ? null : response.getUsage().getPromptTokens(),
                    response.getUsage() == null ? null : response.getUsage().getTotalTokens()
            );
            recordSuccess(callLog, responseEntity.getStatusCode().value(), requestId, response.getUsage(), startTime);
            return result;
        } catch (RestClientResponseException exception) {
            EmbeddingException embeddingException = toEmbeddingException(exception);
            recordFailure(callLog, embeddingException, startTime);
            throw failEmbedding(embeddingException, inputLength);
        } catch (ResourceAccessException exception) {
            EmbeddingException embeddingException = isTimeoutException(exception)
                    ? new EmbeddingException("TIMEOUT", "TIMEOUT", exception)
                    : new EmbeddingException("NETWORK_ERROR", "NETWORK_ERROR", exception);
            recordFailure(callLog, embeddingException, startTime);
            throw failEmbedding(embeddingException, inputLength);
        } catch (RestClientException exception) {
            EmbeddingException embeddingException = isTimeoutException(exception)
                    ? new EmbeddingException("TIMEOUT", "TIMEOUT", exception)
                    : new EmbeddingException("HTTP_REQUEST_FAILED", "HTTP_REQUEST_FAILED", exception);
            recordFailure(callLog, embeddingException, startTime);
            throw failEmbedding(embeddingException, inputLength);
        } catch (EmbeddingException exception) {
            recordFailure(callLog, exception, startTime);
            throw failEmbedding(exception, inputLength);
        }
    }

    private RagProperties.EmbeddingProperties getConfig() {
        RagProperties.EmbeddingProperties config = ragProperties.getEmbedding();
        if (config == null) {
            throw missingConfig("embedding");
        }
        if (!StringUtils.hasText(config.getBaseUrl())) {
            throw missingConfig("baseUrl");
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new EmbeddingException("CONFIG_MISSING", "Embedding API Key 未配置");
        }
        if (!StringUtils.hasText(config.getModel())) {
            throw missingConfig("model");
        }
        if (config.getDimensions() == null || config.getDimensions() <= 0) {
            throw missingConfig("dimensions");
        }
        if (config.getTimeoutSeconds() == null || config.getTimeoutSeconds() <= 0) {
            throw missingConfig("timeoutSeconds");
        }
        return config;
    }

    private EmbeddingException missingConfig(String fieldName) {
        log.warn("RAG embedding config missing: {}", fieldName);
        return new EmbeddingException("CONFIG_MISSING", "CONFIG_MISSING: " + fieldName);
    }

    private ZhipuEmbeddingResponse readEmbeddingResponse(String rawResponse, Integer statusCode, String requestId) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new EmbeddingException("EMPTY_RESPONSE", "EMPTY_RESPONSE", statusCode, null, requestId, null);
        }
        try {
            return objectMapper.readValue(rawResponse, ZhipuEmbeddingResponse.class);
        } catch (JsonProcessingException exception) {
            throw new EmbeddingException(
                    "RESPONSE_PARSE_FAILED",
                    "RESPONSE_PARSE_FAILED",
                    statusCode,
                    rawResponse,
                    requestId,
                    exception
            );
        }
    }

    private List<Float> extractVector(
            ZhipuEmbeddingResponse response,
            String rawResponse,
            Integer statusCode,
            String requestId
    ) {
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new EmbeddingException("EMPTY_DATA", "EMPTY_DATA", statusCode, rawResponse, requestId, null);
        }
        ZhipuEmbeddingData first = response.getData().get(0);
        if (first == null || first.getEmbedding() == null || first.getEmbedding().isEmpty()) {
            throw new EmbeddingException("EMPTY_EMBEDDING", "EMPTY_EMBEDDING", statusCode, rawResponse, requestId, null);
        }
        return first.getEmbedding();
    }

    private void validateDimensions(
            List<Float> vector,
            Integer expectedDimensions,
            String rawResponse,
            Integer statusCode,
            String requestId
    ) {
        if (expectedDimensions == null || vector == null || vector.size() == expectedDimensions) {
            return;
        }
        throw new EmbeddingException(
                "DIMENSION_MISMATCH",
                "DIMENSION_MISMATCH: expected=" + expectedDimensions + ", actual=" + vector.size(),
                statusCode,
                rawResponse,
                requestId,
                null
        );
    }

    private EmbeddingException toEmbeddingException(RestClientResponseException exception) {
        ErrorInfo errorInfo = parseErrorInfo(exception.getResponseBodyAsString());
        return new EmbeddingException(
                errorInfo.getErrorCode(),
                errorInfo.getErrorMessage(),
                exception.getStatusCode().value(),
                exception.getResponseBodyAsString(),
                getRequestId(exception.getResponseHeaders(), null),
                exception
        );
    }

    private ErrorInfo parseErrorInfo(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return new ErrorInfo("HTTP_ERROR", "Zhipu embedding request failed");
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                return new ErrorInfo(
                        textOrDefault(errorNode.path("code"), "HTTP_ERROR"),
                        textOrDefault(errorNode.path("message"), "Zhipu embedding request failed")
                );
            }
            return new ErrorInfo(
                    textOrDefault(root.path("code"), "HTTP_ERROR"),
                    textOrDefault(root.path("message"), abbreviate(rawResponse))
            );
        } catch (JsonProcessingException exception) {
            return new ErrorInfo("HTTP_ERROR", abbreviate(rawResponse));
        }
    }

    private AiCallLog buildCallLog() {
        RagProperties.EmbeddingProperties config = ragProperties.getEmbedding();
        AiCallLog callLog = new AiCallLog();
        callLog.setProvider(config == null ? "" : textValue(config.getProvider()));
        callLog.setModelName(config == null ? "" : textValue(config.getModel()));
        callLog.setRequestType(AiRequestType.RAG_EMBEDDING.name());
        callLog.setPromptVersion(PROMPT_VERSION);
        callLog.setCreatedAt(LocalDateTime.now());
        return callLog;
    }

    private void recordSuccess(
            AiCallLog callLog,
            Integer statusCode,
            String requestId,
            ZhipuUsage usage,
            long startTime
    ) {
        callLog.setLatencyMs(System.currentTimeMillis() - startTime);
        callLog.setSuccess(SUCCESS);
        callLog.setStatusCode(statusCode);
        callLog.setRequestId(requestId);
        if (usage != null) {
            callLog.setPromptTokens(usage.getPromptTokens());
            callLog.setTotalTokens(usage.getTotalTokens());
        }
        aiCallLogService.record(callLog);
    }

    private void recordFailure(AiCallLog callLog, EmbeddingException exception, long startTime) {
        callLog.setLatencyMs(System.currentTimeMillis() - startTime);
        callLog.setSuccess(FAILURE);
        callLog.setStatusCode(exception.getStatusCode());
        callLog.setErrorCode(exception.getErrorCode());
        callLog.setErrorMessage(abbreviate(exception.getErrorMessage()));
        callLog.setRequestId(exception.getRequestId());
        aiCallLogService.record(callLog);
    }

    private BusinessException failEmbedding(EmbeddingException exception, int inputLength) {
        log.warn(
                "RAG embedding request failed, provider={}, model={}, dimensions={}, inputLength={}, errorCode={}, statusCode={}, errorMessage={}",
                getProvider(),
                getModelName(),
                getDimensions(),
                inputLength,
                exception.getErrorCode(),
                exception.getStatusCode(),
                abbreviate(exception.getErrorMessage()),
                exception
        );
        if ("CONFIG_MISSING".equals(exception.getErrorCode()) && "Embedding API Key 未配置".equals(exception.getErrorMessage())) {
            return new BusinessException(EMBEDDING_FAILED_CODE, "Embedding API Key 未配置");
        }
        return new BusinessException(EMBEDDING_FAILED_CODE, EMBEDDING_FAILED_MESSAGE);
    }

    private String getRequestId(HttpHeaders headers, ZhipuEmbeddingResponse response) {
        if (headers == null) {
            return response == null ? null : response.getId();
        }
        String requestId = headers.getFirst("x-request-id");
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        requestId = headers.getFirst("x-correlation-id");
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        return response == null ? null : response.getId();
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (StringUtils.hasText(message) && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String resolveEmbeddingEndpoint(String baseUrl) {
        String normalized = trimTrailingSlash(baseUrl);
        if (normalized.endsWith("/embeddings")) {
            return normalized;
        }
        return normalized + "/embeddings";
    }

    private int resolveTimeoutSeconds(RagProperties properties) {
        if (properties == null
                || properties.getEmbedding() == null
                || properties.getEmbedding().getTimeoutSeconds() == null
                || properties.getEmbedding().getTimeoutSeconds() <= 0) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        return properties.getEmbedding().getTimeoutSeconds();
    }

    private String getProvider() {
        RagProperties.EmbeddingProperties config = ragProperties.getEmbedding();
        return config == null ? "" : textValue(config.getProvider());
    }

    private String getModelName() {
        RagProperties.EmbeddingProperties config = ragProperties.getEmbedding();
        return config == null ? "" : textValue(config.getModel());
    }

    private Integer getDimensions() {
        RagProperties.EmbeddingProperties config = ragProperties.getEmbedding();
        return config == null ? null : config.getDimensions();
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String textValue(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private static class ErrorInfo {

        private final String errorCode;

        private final String errorMessage;

        private ErrorInfo(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class ZhipuEmbeddingRequest {

        private String model;

        private String input;

        private Integer dimensions;

        public ZhipuEmbeddingRequest(String model, String input, Integer dimensions) {
            this.model = model;
            this.input = input;
            this.dimensions = dimensions;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public Integer getDimensions() {
            return dimensions;
        }

        public void setDimensions(Integer dimensions) {
            this.dimensions = dimensions;
        }
    }

    public static class ZhipuEmbeddingResponse {

        private String id;

        private String model;

        private List<ZhipuEmbeddingData> data;

        private ZhipuUsage usage;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<ZhipuEmbeddingData> getData() {
            return data;
        }

        public void setData(List<ZhipuEmbeddingData> data) {
            this.data = data;
        }

        public ZhipuUsage getUsage() {
            return usage;
        }

        public void setUsage(ZhipuUsage usage) {
            this.usage = usage;
        }
    }

    public static class ZhipuEmbeddingData {

        private List<Float> embedding;

        private Integer index;

        private String object;

        public List<Float> getEmbedding() {
            return embedding;
        }

        public void setEmbedding(List<Float> embedding) {
            this.embedding = embedding;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }
    }

    public static class ZhipuUsage {

        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
