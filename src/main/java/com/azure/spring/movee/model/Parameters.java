package com.azure.spring.movee.model;

import lombok.*;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Builder
@Setter
@Getter
@AllArgsConstructor
public class Parameters {

    Object type;
    List<String> required;
    Map<String, Properties> properties;

}
