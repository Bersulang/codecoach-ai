package com.codecoach.common.storage;

public interface AliyunOssService {

    String upload(byte[] content, String originalFilename);

    String upload(byte[] content, String originalFilename, String directory);
}
