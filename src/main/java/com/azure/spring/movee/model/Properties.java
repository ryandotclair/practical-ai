package com.azure.spring.movee.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Properties {

    String type;
    String description;
    List<String> enumString;
}
