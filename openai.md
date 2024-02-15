
# Azure OpenAI

This section covers:
- Why Azure's OpenAI
- How to provision
- How to get access


## The Why

If you've seen ChatGPT in action, then you'll know the power of using OpenAI's models. What Azure brings to the table is a number of benefits to you:

- Your own private instance of the models, so there's no chance of your private data leaking to other users.
- Safety [filters](https://learn.microsoft.com/en-us/azure/ai-services/openai/concepts/content-filter?tabs=warning%2Cpython) that protect not only the prompt going in but also the model's response back to the user.
- Additional protections like anti-Jailbreaking, anti-copyright material, and licensed code
- Block lists, quotas, and more!

## Provisioning Your First Model

In the Azure Portal ([portal.azure.com](portal.azure.com)), in the search box at the top, search for `Azure OpenAI` and select the service.

Next, hit the `+ Create` button towards the top left of the the screen.

Use the following values:
- *Resource Group* = `practical-ai`

