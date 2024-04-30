# Welcome

Welcome [NAME]!

This guide has been custom built to you, using Azure Spring App Enterprise's App Accelerator feature. Think of this feature as a templating engine that can not only templatize all of your organization's best practices into ready-made "get off the ground and github co-pilot from there" app templates, but also a method in sharing knowledge within your organization (like how we're using it here). Have a service that you want other teams in your company to use? Why not give instructions, customized to them, with actual code examples, published to a dev portal! Everything is backed by git, so people can also contribute to it's content (hint, hint, PR's are welcomed!)

For our purposes we used your name (as a greeting) and the first 10 characters of your TMDB API token ([tmdbapi]) to create unique names to the various resources within Azure that require globally unique names (for DNS reasons).

Vee utilizes the following Azure resources:
- [**Azure Spring Apps Enterprise**](https://docs.microsoft.com/azure/spring-apps/) to host the application
- [**Azure OpenAI**](https://docs.microsoft.com/azure/cognitive-services/openai/) for the AI model.
- [**Azure Cache for Redis**](https://learn.microsoft.com/en-us/azure/azure-cache-for-redis/) to save the conversation history for each user and the embeddings.

Here's a high level architecture diagram that illustrates these components. Notice that these are all contained within a single [resource group](https://docs.microsoft.com/azure/azure-resource-manager/management/manage-resource-groups-portal), that will be created for you when you create the resources.


