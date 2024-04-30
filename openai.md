
# Azure OpenAI

The [Azure OpenAI Service](https://learn.microsoft.com/en-us/azure/ai-services/openai/) provides access to OpenAI's models including the GPT-4, GPT-4 Turbo with Vision, GPT-3.5-Turbo, DALLE-3 and Embeddings model series with the security and enterprise capabilities of Azure.

This is the "brains" of Vee.

# Deploying OpenAI
1. Run the following command to create an Azure OpenAI resource in the practical-[tmdbapi] resource group.

   ```bash
   az cognitiveservices account create \
      -n practical-[tmdbapi] \
      -g practical-ai \
      -l eastus \
      --kind OpenAI \
      --sku s0 \
      --custom-domain practical-[tmdbapi]
   ```

1. Create the model deployments for `text-embedding-ada-002` and `gpt-35-turbo-16k` in your Azure OpenAI service.
   ```bash
   az cognitiveservices account deployment create \
      -g practical-ai \
      -n practical-[tmdbapi] \
      --deployment-name text-embedding-ada-002 \
      --model-name text-embedding-ada-002 \
      --model-version "2"  \
      --model-format OpenAI

   az cognitiveservices account deployment create \
      -g practical-ai \
      -n practical-[tmdbapi] \
      --deployment-name gpt-35-turbo-16k \
      --model-name gpt-35-turbo-16k \
      --model-version "0613"  \
      --model-format OpenAI
   ```

   