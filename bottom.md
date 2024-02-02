# Deploying

Assuming you have created the below resources into US East 2:
- Resource Group called `practical-ai`
- CosmosDB called `practical-[PHONE]`
- Azure Spring Apps Enterprise instances called `practical-[PHONE]`

And you've set the below defaults into your trusty az CLI tool:
```bash
az configure --defaults \
    group=practical-ai \
    location=eastus2 \
    spring=pracitcal-[PHONE]
```

Let's deploy our app [NAME]!

First we create the app construct on Azure Spring Apps Enterprise using the following syntax:
```bash
az spring app create -n mobot --assign-endpoint true --query properties.url
```
This usually takes less than 2 minutes. Breakign this command down:
- `-n mobot` is giving the app the name `mobot`
- `--assign-endpoint true` is 