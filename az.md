
# AZ CLI
While we could do everything in the UI, the CLI is faster and easier to show in this guide.

If you are not familiar with CLI (aka command line, aka terminal, aka console), I HIGHLY recommend using Azure's [Cloud Console](https://shell.azure.com) (the `>_` icon next to the search bar in Azure's portal). It does cost a small nominal fee ($0.0255/hr per GB), which would be covered completely by the $200 credit if you're a first time Azure user. The first time you use it it will ask Bash or Powershell, pick Bash (the instructions in this guide assume Bash), it will also provision storage so it saves all your work across login sessions.

If you are comfortable with CLI tools and know how to navidate around on your local machine, here's the installation instructions for: [Windows](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli-windows?tabs=azure-cli), [Mac](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli-macos), and [Linux](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli) users.

## Login
Head on over to your trusty terminal window and confirm you are logged in. For Cloud Console users, thats the `>_` icon next to the search bar in Azure's console (or click [here](https://portal.azure.com/#cloudshell/)).

Run:
```
    az login
```
> Tip: If you have multiple subscriptions, you can change via `az account set --subscription <subscriptionID>` command.

## Install the Spring extension
Next, add in the Azure Spring Apps extension by running:
```
    az extension add --name spring
```

If it says you already have it, update it by running:
```
    az extension update --name spring
```

## Copy the Code
A zip file should have been generated from the App Accelerator. You'll want to unpack it where the CLI tool is installed and make sure it's in the right directory (aka folder). To get it into Azure's [console](https://portal.azure.com/#cloudshell/), click on the icon that looks like a page with bidirectional arrows and upload the `practical-ai.zip`. By default it will land in your home directory (/home/<username>). Run the following commands:

```bash
unzip practical-ai.zip # tip, hit tab after the 'p' and it should auto complete
cd practical-ai
```


Now your CLI is ready!

