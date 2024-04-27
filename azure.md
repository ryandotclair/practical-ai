
# Azure Explainer

This section covers:
- How to create an Azure account and a subscription
- What a subscription and Resource Group (RG) is
- How to create an RG

## Create Azure Account and Subscription

Head on over to [portal.azure.com](https://portal.azure.com). Under the login screen, there's an option to create an account. Some companies have single-sign-on and provide their employees an account by default. Assuming your company hasn't done this, creating an account should take about 3min.

> Please note, we will need a valid credit card on file, although you won't be charged so long as you delete this instance before the free credit time is up (~3 hours).

Once logged in, you'll need to sign up for an Azure subscription. The steps are right when you log in after creating an Azure Account titled "Start with an Azure free trial". As a bonus, you'll get a $200 credit!

After providing your information and getting your account set up, click on "Go to Azure Portal" (skip the quick start).

## Subscription and Resource Groups Explainer
Your account (aka Directory aka Tenant ID) now has a subscription tied to you. Most companies _typically_ only have one Azure Account (tied to Active Directory) and usually there are many subscriptions tied to it (for billing and management purposes). Within a subscription you can create one or more Resource Groups (RGs). As the name implies this is a logical construct that you can deploy resources into, as well as assign RBAC controls. The nice part about RGs is it's a single object you can delete, and everything inside of it gets deleted for you (like after this guide is done).

```
Account
 |_Subscription_A
 | |_ResourceGroup_1
 |   |_Resource_C
 |   |_Resouce_D
 |_Subscription_B
   |_ResourceGroup_2
     |_Resource_E
     |_Resource_F
```
Click on the search bar at the top, search for Resource Groups. Hit the `+ Create` button towards the top. In this screen you'll see you can have access to multiple subscriptions (use-case: Production vs. Dev subscription). Name your RG `practical-ai`, and leave the default region (East US). Hit `Review + create` button.

> Pro Tip: You could also do this using the Azure CLI command: `az group create -n practical-ai -l eastus`

You're ready to deploy all the services you need for Vee to run!

