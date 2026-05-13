package com.codecoach.module.memory.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMemoryItemVO {

    private Long id;

    private String type;

    private String key;

    private String value;

    private String confidence;

    private Integer weight;

    private LocalDateTime lastReinforcedAt;
}
