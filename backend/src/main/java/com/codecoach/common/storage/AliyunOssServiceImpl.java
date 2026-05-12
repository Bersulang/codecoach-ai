package com.codecoach.common.storage;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AliyunOssServiceImpl implements AliyunOssService {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssServiceImpl.class);

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private static final String DEFAULT_DIRECTORY = "uploads";

    private final AliyunOssProperties properties;

    public AliyunOssServiceImpl(AliyunOssProperties properties) {
        this.properties = properties;
    }

    @Override
    public String upload(byte[] content, String originalFilename) {
        return upload(content, originalFilename, DEFAULT_DIRECTORY);
    }

    @Override
    public String upload(byte[] content, String originalFilename, String directory) {
        validateConfig();
        if (content == null || content.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        String objectName = buildObjectName(directory, originalFilename);
        OSS ossClient = null;
        try {
            CredentialsProvider credentialsProvider = buildCredentialsProvider();
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
            ossClient = OSSClientBuilder.create()
                    .endpoint(normalizedEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(properties.getRegion())
                    .build();
            ossClient.putObject(properties.getBucketName(), objectName, new ByteArrayInputStream(content));
            return buildPublicUrl(objectName);
        } catch (Exception exception) {
            log.warn("Aliyun OSS upload failed, objectName={}, contentLength={}, error={}",
                    objectName,
                    content.length,
                    abbreviate(exception.getMessage()));
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getBucketName())
                || !StringUtils.hasText(properties.getRegion())) {
            log.warn("Aliyun OSS config missing: endpoint/bucketName/region");
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
    }

    private CredentialsProvider buildCredentialsProvider() throws Exception {
        if (StringUtils.hasText(properties.getAccessKeyId())
                && StringUtils.hasText(properties.getAccessKeySecret())) {
            return CredentialsProviderFactory.newDefaultCredentialProvider(
                    properties.getAccessKeyId().trim(),
                    properties.getAccessKeySecret().trim()
            );
        }
        EnvironmentVariableCredentialsProvider credentialsProvider =
                CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        return credentialsProvider;
    }

    private String buildObjectName(String directory, String originalFilename) {
        String normalizedDirectory = normalizeDirectory(directory);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        return normalizedDirectory + "/" + datePath + "/" + UUID.randomUUID() + getFileExtension(originalFilename);
    }

    private String normalizeDirectory(String directory) {
        if (!StringUtils.hasText(directory)) {
            return DEFAULT_DIRECTORY;
        }
        String normalized = directory.trim()
                .replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        if (!StringUtils.hasText(normalized) || normalized.contains("..")) {
            return DEFAULT_DIRECTORY;
        }
        return normalized;
    }

    private String getFileExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        String filename = originalFilename.trim();
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index);
    }

    private String buildPublicUrl(String objectName) {
        URI endpointUri = URI.create(normalizedEndpoint());
        String scheme = endpointUri.getScheme();
        String host = endpointUri.getHost();
        if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
            return normalizedEndpoint() + "/" + objectName;
        }
        return scheme + "://" + properties.getBucketName() + "." + host + "/" + objectName;
    }

    private String normalizedEndpoint() {
        String endpoint = properties.getEndpoint().trim();
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return "https://" + endpoint;
    }

    private String abbreviate(String text) {
        if (!StringUtils.hasText(text)) {
            return "unknown";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }
}
