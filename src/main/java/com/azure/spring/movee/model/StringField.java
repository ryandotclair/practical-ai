package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StringField {
    @JsonProperty(value = "type")
    private final String type = "string";

    @JsonProperty(value = "description")
    private String description;

    @JsonCreator
    StringField(@JsonProperty(value = "description") String description) {
        this.description = description;
    }
}