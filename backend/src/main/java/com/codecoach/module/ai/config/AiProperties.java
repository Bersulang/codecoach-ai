package com.codecoach.module.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider;

    private OpenAiCompatible openAiCompatible = new OpenAiCompatible();

    private Gateway gateway = new Gateway();

    private SpringAi springAi = new SpringAi();

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

    public Gateway getGateway() {
        return gateway;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public SpringAi getSpringAi() {
        return springAi;
    }

    public void setSpringAi(SpringAi springAi) {
        this.springAi = springAi;
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

        private Integer timeoutSeconds = 120;

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

    public static class Gateway {

        private String primary = "openai-compatible";

        private Boolean shadowSpringAi = false;

        public String getPrimary() {
            return primary;
        }

        public void setPrimary(String primary) {
            this.primary = primary;
        }

        public Boolean getShadowSpringAi() {
            return shadowSpringAi;
        }

        public void setShadowSpringAi(Boolean shadowSpringAi) {
            this.shadowSpringAi = shadowSpringAi;
        }
    }

    public static class SpringAi {

        private Boolean enabled = false;

        private String provider = "spring-ai";

        private String model;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
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
