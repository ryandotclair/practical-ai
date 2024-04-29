# Spring AI Azure Movee App


## How it works


### Application Architecture

This application utilizes the following Azure resources:

- [**Azure Spring Apps**](https://docs.microsoft.com/azure/spring-apps/) to host the application
- [**Azure OpenAI**](https://docs.microsoft.com/azure/cognitive-services/openai/) for ChatGPT
- [**Azure Redis**](https://learn.microsoft.com/en-us/azure/azure-cache-for-redis/) to save the conversation history for each user.

Here's a high level architecture diagram that illustrates these components. Notice that these are all contained within a single [resource group](https://docs.microsoft.com/azure/azure-resource-manager/management/manage-resource-groups-portal), that will be created for you when you create the resources.

!["Application architecture diagram"](TBD)

## Run with Azure CLI
You can run this sample app using the **Azure CLI** by following these steps.

### Prerequisites

- JDK 17
- Maven
- Azure CLI
- An Azure subscription with access granted to Azure OpenAI (see more [here](https://aka.ms/oai/access))

### Clone and Build the repo

1. Run `git clone https://github.com/rohanmukesh/spring-ai-azure-movee.git`
2. Run `cd spring-ai-azure-movee`.
4. Build with `./mvnw clean package -PProduction`.


### Preprocess the movies

Before running the web app, you need to preprocess the movies and load them into the vector store:
```bash
source env.sh
java -jar spring-ai-azure-movee/target/spring-ai-azure-movee-0.0.1-SNAPSHOT.jar --from=/<path>/<to>/<your>/<documents>
```

Or [dowload](https://asawikigpt.blob.core.windows.net/demo/doc_store.json) the pre-built vector store of the [public documents](https://github.com/MicrosoftDocs/azure-docs/tree/main/articles/spring-apps) of the Azure Spring Apps.

### Run in local

To run the demo in the local machine, please follow these steps:

1. Launch the web app
   ```bash
   ./mvnw clean package
   source env.sh
   java -jar spring-ai-azure-movee/target/spring-ai-azure-movee-0.0.1-SNAPSHOT.jar
   ```

1. Open `http://localhost:8080` in your browser.


## Reporting Issues and Feedback
