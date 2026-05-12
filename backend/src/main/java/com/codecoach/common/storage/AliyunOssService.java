package com.codecoach.common.storage;

public interface AliyunOssService {

    String upload(byte[] content, String originalFilename);

    String upload(byte[] content, String originalFilename, String directory);

    OssUploadResult uploadWithKey(byte[] content, String objectKey);

    byte[] download(String objectKey);

    void delete(String objectKey);
}
