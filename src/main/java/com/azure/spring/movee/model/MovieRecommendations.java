package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MovieRecommendations {
    @JsonProperty(value = "name") String region;
}
