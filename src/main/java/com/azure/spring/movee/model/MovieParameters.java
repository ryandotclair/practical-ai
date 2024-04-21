package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;


public class MovieParameters {

    @JsonProperty(value = "type")
    private String type = "object";

    @JsonProperty(value = "required")
    List<String> required = Arrays.asList("region");

    @JsonProperty(value = "properties")
    private MovieProperties properties = new MovieProperties();

}
