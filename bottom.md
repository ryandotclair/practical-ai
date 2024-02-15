
# Deploying Vee

Assuming you have created the below resources into US East:
- Resource Group called `practical-ai`
    - CosmosDB called `practical-[tmdbapi]`
    - OpenAI called `practical-[tmdbapi]`
    - Redis called `practical-[tmdbapi]`
    - Azure Spring Apps Enterprise instances called `practical-[tmdbapi]`

And you've set the below defaults into your trusty az CLI tool:
```bash
az configure --defaults \
    group=practical-ai \
    location=eastus \
    spring=pracitcal-[tmdbapi]
```

Let's deploy our app [NAME]!

First we create the app construct on Azure Spring Apps Enterprise. This construct is where we can scale the app (up/down/out/in), set ingress-to-app TLS encryption, remote debug, manage deployments, and even console directly into the app.
```bash
az spring app create -n vee --assign-endpoint true --query properties.url
```

This usually takes less than 2 minutes. Breaking this command down:
- `-n vee` is giving the app the name `vee`
- `--assign-endpoint true` exposes the app behind an HTTPS endpoint.
- `--query properties.url` returns the URL of the app

> Tip: Every command you can pass in `-o json` to see what is queryable with --query command.

Go to your app now (if you've followed along with this guide, your url should be: https://practical-[tmdbapi]-vee.azuremicroservices.io).

You'll see the "default" deployment that comes out of the box when you create a new app. The benefit of this is having a quick smoke test on whether the infra is up and running or not (read: networking issues). Nifty!

Now all the commands you've ran so far are typically done once. What you're likely to do over and over again is actually deploying your application (vee). While we could just override the default deployment with a `az spring app deploy` command, we'll use the `deployment create` command to walk you through the promotion feature. Ensure you are in the same folder/directory as this `lab.md` file is and run...

```bash
az spring app deployment create -n v1 --app vee --source-path . --build-env BP_JVM_VERSION=17 -e x=y
```
Now the magic happens. What's happening now is the code in this folder is being sent to a robot called Tanzu Build Service. It's scaning the source code, figuring out that it's a Spring app, building it into a jar file for you (optional), along with all the app's dependencies, and creating a highly secured container--using the latest fully patched image that's auto-updated for you--and will deploy it for you into Azure in a rolling fashion.

This command typically takes ~4min (depends on the type of app). Breaking this command down:
- `deployment create`, Azure Spring Apps supports up to two deployments. The deployment named `default` is the out of the box one that's automatically created for you. This is how we create a new one, and by default this new deployment is put into staging. Worth noting that this command is idepotent.
- `-n v1`, we're naming the deployment's name `v1`
- `--app vee`, this is telling it to create the new deployment named v1 under the vee app.
- `--source-path .`, here we are asking it to take all the source code in the current working directory (aka `.` aka folder) and send it to Azure Spring Apps. An alternative option is `--artifact-path <jar, war, netcore zip file>`
- `--build-env BP_JVM_VERSION=17`, here we're giving an example of how you can tell the Tanzu Build Service that we want to specifically use Java 17. 
- `-e `, Lastly, we're passing in the various environment variables that the app expects to exist at runtime.

Once it's done, if you go back to the url (https://practical-[tmdbapi]-vee.azuremicroservices.io) you'll note that nothing has changed. And that's because our app has been deployed into staging (`v1`). You could validate it by either going into the UI and clicking on the vee app / Deployments / and selecting the staging link (it's not public, hidden by a login), but let's just YOLO it and promote it to production!

```bash
az spring app set-deployment -n vee -d v1
```
Go ahead and hit refresh in the browser, and you should see it eventually change from the default app screen to vee. The beauty of this is it's effectively just a network change to promote it into production (zero downtime).

If you wanted to use some more advance traffic shaping patterns (A/B testing, canary deployments, experimental deployments using % based traffic, etc), Azure Spring Apps Enterprise comes with a fully managed API router called Spring Cloud Gateway that allows you to do some more advance promotions, in addition to adding SSO for your end users, and (among other things) header manipulations.


# Cleaning Up

Once you're done, don't forget to delete the `practical-ai` resource group so you don't get charged!

```bash
az group delete -n practical-ai
```

We hope you enjoyed this lab. If you have any additional questions, don't hesitate to reach out to us at tanzu-azure.pdl@broadcom.com. We also have a more robust lab guide for Azure Spring Apps Enterprise found [here](https://github.com/Azure-Samples/acme-fitness-store/tree/Azure)

Thanks!
