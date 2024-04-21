package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MoviesLocation {
    @JsonProperty(value = "region") String region;
    @JsonProperty(value = "date") String date;
}
