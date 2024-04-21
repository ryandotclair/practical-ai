package com.azure.spring.movee.model;

import lombok.Data;

@Data
public class ChatFunctionDetails {
    private String toolID;
    private String functionName;
    private StringBuilder functionArguments = new StringBuilder();
}
