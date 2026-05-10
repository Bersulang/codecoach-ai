package com.codecoach.module.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider;

    private OpenAiCompatible openAiCompatible = new OpenAiCompatible();

    private Log log = new Log();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OpenAiCompatible getOpenAiCompatible() {
        return openAiCompatible;
    }

    public void setOpenAiCompatible(OpenAiCompatible openAiCompatible) {
        this.openAiCompatible = openAiCompatible;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public static class OpenAiCompatible {

        private String baseUrl;

        private String apiKey;

        private String model;

        private Integer timeoutSeconds = 30;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Log {

        private Boolean saveRawResponse = false;

        public Boolean getSaveRawResponse() {
            return saveRawResponse;
        }

        public void setSaveRawResponse(Boolean saveRawResponse) {
            this.saveRawResponse = saveRawResponse;
        }
    }
}
