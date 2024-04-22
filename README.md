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

### Prepare Azure Spring Apps instance

1. Use the following commands to define variables for this quickstart with the names of your resources and desired settings:

   ```bash
   LOCATION="eastus"
   RESOURCE_GROUP="<resource-group-name>"
   MANAGED_ENVIRONMENT="<Azure-Container-Apps-environment-name>"
   SERVICE_NAME="<Azure-Spring-Apps-instance-name>"
   APP_NAME="<Spring-app-name>"
   AZURE_OPENAI_RESOURCE_NAME="<Azure-OpenAI-resource-name>"
   CACHE="<Azure-Redis-Cache-Name>"
   ```

1. Use the following command to create a resource group:

   ```bash
   az group create \
       --resource-group ${RESOURCE_GROUP} \
       --location ${LOCATION}
   ```

1. An Azure Container Apps environment creates a secure boundary around a group of applications. Apps deployed to the same environment are deployed in the same virtual network and write logs to the same log analytics workspace. For more information, see [Log Analytics workspace overview](../azure-monitor/logs/log-analytics-workspace-overview.md). Use the following command to create the environment:

   ```bash
   az containerapp env create \
       --resource-group ${RESOURCE_GROUP} \
       --name ${MANAGED_ENVIRONMENT} \
       --location ${LOCATION} \
       --enable-workload-profiles
   ```

1. Use the following command to create a variable to store the environment resource ID:

   ```bash
   MANAGED_ENV_RESOURCE_ID=$(az containerapp env show \
       --resource-group ${RESOURCE_GROUP} \
       --name ${MANAGED_ENVIRONMENT} \
       --query id \
       --output tsv)
   ```

1. Use the following command to create an Azure Spring Apps service instance. An instance of the Azure Spring Apps Standard consumption and dedicated plan is built on top of the Azure Container Apps environment. Create your Azure Spring Apps instance by specifying the resource ID of the environment you created.

   ```bash
   az spring create \
       --resource-group ${RESOURCE_GROUP} \
       --name ${SERVICE_NAME} \
       --managed-environment ${MANAGED_ENV_RESOURCE_ID} \
       --sku standardGen2 \
       --location ${LOCATION}
   ```

### Prepare Azure OpenAI Service

1. Run the following command to create an Azure OpenAI resource in the the resource group.

   ```bash
   az cognitiveservices account create \
      -n ${AZURE_OPENAI_RESOURCE_NAME} \
      -g ${RESOURCE_GROUP} \
      -l ${LOCATION} \
      --kind OpenAI \
      --sku s0 \
      --custom-domain ${AZURE_OPENAI_RESOURCE_NAME}   
   ```

1. Create the model deployments for `text-embedding-ada-002` and `gpt-35-turbo` in your Azure OpenAI service.
   ```bash
   az cognitiveservices account deployment create \
      -g ${RESOURCE_GROUP} \
      -n ${AZURE_OPENAI_RESOURCE_NAME} \
      --deployment-name text-embedding-ada-002 \
      --model-name text-embedding-ada-002 \
      --model-version "2"  \
      --model-format OpenAI

   az cognitiveservices account deployment create \
      -g ${RESOURCE_GROUP} \
      -n ${AZURE_OPENAI_RESOURCE_NAME} \
      --deployment-name gpt-35-turbo \
      --model-name gpt-35-turbo \
      --model-version "0301"  \
      --model-format OpenAI   
   ```

### Prepare Azure Cache for Redis
1. Run the following command to create an Azure Cache for Redis in the resource group.

   ```bash
    az redis create \
      -g ${RESOURCE_GROUP} \
      -n ${CACHE} \
      -l ${LOCATION} \
      --sku "basic"  \
      --vm-size "c0"   
   ```
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

### Run in Azure Spring Apps

1. Use the following command to specify the app name on Azure Spring Apps and to allocate required resources:

   ```bash
   az spring app create \
      --resource-group ${RESOURCE_GROUP} \
      --service ${SERVICE_NAME} \
      --name ${APP_NAME} \
      --cpu 2 \
      --memory 4Gi \
      --min-replicas 1 \
      --max-replicas 1 \
      --assign-endpoint true
   ```

1. Use the following command to deploy the *.jar* file for the app:

   ```bash
   az spring app deploy \
      --resource-group ${RESOURCE_GROUP} \
      --service ${SERVICE_NAME} \
      --name ${APP_NAME} \
      --artifact-path target/spring-ai-azure-movee-0.0.1-SNAPSHOT.jar \
      --runtime-version Java_17
      --env AZURE_OPENAI_ENDPOINT=<your_azure_openai_endpoint> AZURE_OPENAI_APIKEY=<your_api_key> AZURE_OPENAI_CHATDEPLOYMENTID=gpt-35-turbo AZURE_OPENAI_EMBEDDINGDEPLOYMENTID=text-embedding-ada-002 \
   ```


## Reporting Issues and Feedback
