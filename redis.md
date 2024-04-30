
# Azure Cache for Redis
The [Azure Cache for Redis service](https://learn.microsoft.com/en-us/azure/azure-cache-for-redis/) a secure data cache and messaging broker that provides high throughput and low-latency access to data for applications. 

This service is used to hold the conversation history and the "behind the scenes" information for each user.

# Deploy Redis
1. Run the following command to create an Azure Cache for Redis in the resource group.

   ```bash
    az redis create \
      -g practical-ai \
      -n practical-[tmdbapi] \
      -l eastus \
      --sku "basic"  \
      --vm-size "c0"
   ```

Note: This takes about 14 minutes so feel free to go grab a cup of coffee.

   