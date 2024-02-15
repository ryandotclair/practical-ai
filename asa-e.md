
# Azure Spring Apps Enterprise (~10min)

Azure Spring Apps Enterprise is a fully managed App Platform, that's Spring aware. It comes with a number of nifty features such as...
- No need to learn DockerFiles, Kubernetes, or YAML (we do that for you)
- No need to patch the infrastructure/platform/base OS images (we do that for you)
- "Give me your source code, or jar file, and I'll deploy it into Azure for you" UX (we also support Python, .NET, Nodejs, Go, and Php)
- Long term extended support for Spring workloads deployed on it, across 50 Spring projects, including Spring Boot (for those still on 2.7, we [support](https://spring.io/projects/spring-boot#support) it until August 24th, 2025) in addition to OpenJDK/Tomcat, as well as phone a friend support... said friend is part of the team that maintains Spring.
- A fully managed API router for advance traffic shaping

TLDR; You focus on your code, Microsoft manages everything for you, and it's all powered (and supported) by Tanzu at Broadcom (artisit formally known as VMware).


For purpose of this guide, we will assume you've created a RG called `practical-ai`.

## AZ CLI Commands

First up, add the spring extension. This is where all the subcommands that are used to manage Azure Spring Apps Enterprise live.

```bash
az extension add --name spring
```
> Note: If it mentions it's already installed, you can ensure you're on the latest version with the command `az extension update -n spring`

Accept the legal terms and privacy statements for the Enterprise tier.

```bash
az provider register --namespace Microsoft.SaaS
az term accept --publisher vmware-inc --product azure-spring-cloud-vmware-tanzu-2 --plan asa-ent-hr-mtr
```

Create an instance of Azure Spring Apps Enterprise.
```bash
az spring create --name practical-[tmdbapi] \
    --resource-group practical-ai \
    --location eastus \
    --sku Enterprise
```
The above command usually takes about 8min. If you've got other infrastructure to deploy, feel free to move on to those in this guide. Otherwise grab a cup of cofee or tea.

