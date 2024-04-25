package com.azure.spring.movee.functions;

import com.azure.ai.openai.models.FunctionCall;
import com.azure.ai.openai.models.FunctionDefinition;

import java.util.List;

public interface MovieFunction {

    public FunctionDefinition getFunctionDefinition();
    public Object execute(FunctionCall functionCall, String question, List<String> logs);
}
