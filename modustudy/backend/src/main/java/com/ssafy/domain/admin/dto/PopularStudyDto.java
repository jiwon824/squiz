package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopularStudyDto {
    private Long id;
    private String name;
    private String topicName;
    private int memberCount;
    private String status;
}
