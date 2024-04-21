package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class MovieRecommendationProperties {

    @JsonProperty(value = "movieName")
    StringField movieName = new StringField("The name of the movie required to search for recommendations.");
}
