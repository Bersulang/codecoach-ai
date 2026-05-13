package com.codecoach.module.mockinterview.dto;

import lombok.Data;

@Data
public class MockInterviewPageRequest {

    private Long pageNum;

    private Long pageSize;

    private String status;
}
