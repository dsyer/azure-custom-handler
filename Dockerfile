FROM mcr.microsoft.com/azure-functions/java:3.0

ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

COPY target/azure-functions/funapp3ds /home/site/wwwroot
COPY target/worker /home/site/wwwroot/
