FROM appsvc/azure-functions-java:3.0-java11-appservice

ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/jvm/zre-11-azure-amd64/bin

COPY target/azure-functions/funapp3ds /home/site/wwwroot
