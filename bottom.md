
# Deploying Vee

Assuming you have created the below resources into US East:
```
Resource Group: practical-ai
 |_OpenAI: practical-[tmdbapi]
 |_Redis: practical-[tmdbapi]
 |_ASA_E: practical-[tmdbapi]
```

And you've set the below defaults into your trusty az CLI tool:
```bash
az configure --defaults \
    group=practical-ai \
    location=eastus \
    spring=practical-[tmdbapi]
```

The last pre-req is grabbing the keys to your OpenAI model and Redis instance so we can pass them into the App. We'll do this by storing them into some environment variables.
```bash

AZURE_OPENAI_APIKEY=$(az cognitiveservices account keys list -n practical-[tmdbapi] --query key1 -o tsv)
AZURE_REDIS_KEY=$(az redis list-keys -n practical-[tmdbapi] --query primaryKey -o tsv)

```

Alright [NAME], let's deploy Vee!

First we create the app construct on Azure Spring Apps Enterprise. This construct is where we can scale the app (up/down/out/in), set ingress-to-app TLS encryption, remote debug, manage deployments, and even console directly into the app.
```bash
az spring app create -n movee --assign-endpoint true --query properties.url --cpu 2 --memory 4Gi
```

This usually takes less than 2 minutes. Breaking this command down:
- `-n movee` is giving the app the name `movee`
- `--assign-endpoint true` exposes the app behind an HTTPS endpoint thats internet accessible.
- `--query properties.url` returns the URL of the app after creation

> Pro Tip: Every command you can pass in `-o json` to see what is queryable with the `--query` command.

Go to your app now (if you've followed along with this guide, your url should be: https://practical-[tmdbapi]-movee.azuremicroservices.io).

You'll see the "default" deployment that comes out of the box when you create a new app. The benefit of this is having a quick smoke test on whether the infra is up and running or not (read: networking issues). Nifty!

Next, let's build the app into a jar file.

```bash
./mvnw clean package -Pproduction
```

Now all the az commands you've ran so far are typically done once. What you're likely to do over and over again is actually deploying your application (movee). While we could just override the default deployment with a `az spring app deploy` command, we'll use the `deployment create` command to walk you through the promotion feature. Ensure you are in the same folder/directory as this `lab.md` file is and run...

```bash
az spring app deployment create \
    -n version1 \
    --app movee \
    --build-env BP_JVM_VERSION=17 \
    --artifact-path target/spring-ai-azure-movee-0.0.1-SNAPSHOT.jar \
    --env AZURE_OPENAI_ENDPOINT="https://pracitcal-eyJhbGciOi.openai.azure.com/" \
    AZURE_OPENAI_APIKEY=$AZURE_OPENAI_APIKEY \
    AZURE_OPENAI_CHATDEPLOYMENTID=gpt-35-turbo \
    AZURE_OPENAI_EMBEDDINGDEPLOYMENTID=text-embedding-ada-002 \
    AZURE_REDIS_URL="pracitcal-eyJhbGciOi.redis.cache.windows.net" \
    AZURE_REDIS_KEY=$AZURE_REDIS_KEY \
    TMDB_API_AUTH_TOKEN="[tmdbapitoken]"
```
Now the magic happens! What's you're seeing is the code in this folder is being sent to a robot called Tanzu Build Service. It's scaning the source code, figuring out that it's a Spring app, building it into a jar file for you (optionally you can just send it the jar file), along with all the app's dependencies, and creating a highly secured container--using all the best practices and the latest fully patched image--and will deploy it for you into Azure in a rolling fashion.

This command typically takes ~4min (depends on the type of app). Breaking this command down:
- `deployment create`, Azure Spring Apps supports up to two deployments. The deployment named `default` is the out of the box one that's automatically created for you. This is how we create a new one, and by default this new deployment is put into staging. Worth noting that this command is idepotent.
- `-n version1`, we're naming the deployment's name `v1`
- `--app movee`, this is telling it to create the new deployment named v1 under the vee app.
- `--source-path .`, here we are asking it to take all the source code in the current working directory (aka `.` aka folder) and send it to Azure Spring Apps. An alternative option is `--artifact-path <jar, war, netcore zip file>`
- `--build-env BP_JVM_VERSION=17`, here we're giving an example of how you can tell the Tanzu Build Service that we want to specifically use Java 17.
- `-e `, Lastly, we're passing in the various environment variables that the Vee expects to exist at runtime.

Once it's done, if you go back to the url (https://practical-[tmdbapi]-vee.azuremicroservices.io) you'll note that nothing has changed. And that's because our app has been deployed into staging (`v1`). You could validate it by either going into the Azure Spring Apps Enteprise Azure portal UI and clicking on the movee app / Deployments / and selecting the staging link (it's not public, hidden by a login), but let's just YOLO it and promote it to production!

```bash
az spring app set-deployment -n movee -d version1
```
Go ahead and hit refresh in the browser, and you should see it eventually change from the default app screen to Vee. The beauty of this is it's effectively just a network change to promote it into production (zero downtime).

If you wanted to use some more advance traffic shaping patterns (A/B testing, canary deployments, experimental deployments using % based traffic, etc), Azure Spring Apps Enterprise comes with a fully managed API router called Spring Cloud Gateway that allows you to do some more advance promotions, in addition to adding SSO for your end users, and (among other things) header manipulations.

The last step is to initialize the embeddings in the Spring AI in-memory vectorDB store with a simple curl statement. This should take less than 60 seconds to finish.

```bash
curl --location --request POST 'https://practical-[tmdbapi].azuremicroservices.io/actuator/store-embeddings'
```

# Cleaning Up

Once you're done, don't forget to delete the `practical-ai` resource group to keep your costs minimumal.

```bash
az group delete -n practical-ai
```

We hope you enjoyed this lab. If you have any additional questions, don't hesitate to reach out to us at tanzu-azure.pdl@broadcom.com. We also have a more robust lab guide for Azure Spring Apps Enterprise found [here](https://github.com/Azure-Samples/acme-fitness-store/tree/Azure).

Thanks!

