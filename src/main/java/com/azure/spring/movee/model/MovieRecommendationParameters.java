package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class MovieRecommendationParameters {

    @JsonProperty(value = "type")
    private String type = "object";

    @JsonProperty(value = "properties")
    private MovieRecommendationProperties properties = new MovieRecommendationProperties();

}
