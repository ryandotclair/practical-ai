package com.azure.spring.movee;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Theme(value = "spring-azure-ai-stream")
public class SpringAiAzureMoveeApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiAzureMoveeApplication.class, args);
	}

}
