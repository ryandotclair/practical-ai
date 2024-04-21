package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class MovieProperties {

    @JsonProperty(value = "region")
    StringField region = new StringField("The region or location where the movie is playing.");
    @JsonProperty(value = "date")
    StringField date = new StringField("The date to movie list for. The format is YYYY-MM-DD.");
}
