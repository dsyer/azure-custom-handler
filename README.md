
# Azure Functions custom handler in Java

The sample in this folder demonstrates how to implement a [custom handler](https://docs.microsoft.com/azure/azure-functions/functions-custom-handlers) in Java. The code is adapted and simplified from a [Microsoft sample](https://github.com/Azure-Samples/functions-custom-handlers), with the addition of a `Dockerfile` (strangely missing in the original sample) and some configuration for building a GraalVM Native Image.

Example functions featured in this repo include:

| Name | Trigger | Input | Output |
|------|---------|-------|--------|
| [trigger](trigger) |  HTTP | HTTP |HTTP |

We meaured response times to the app deployed in Azure:

```bash
time curl -v https://funapp-dsyer.azurewebsites.net/api/trigger -H "Content-Type: application/json" -d '{"Value":"Foo"}'
```

Results (response times in milliseconds):

|Platform | Runtime | Cold | Warm |
|---------|---------|------|------|
| Azure   | Function + Java | 1423 | 887 |
| Azure   | Function + Native | 1100 | 717 |
| Azure   | Java | 2474(*) | 181 |
| Azure   | Native | 400 | 231 |

(*) I only ever saw this once. It doesn't appear that the app container usually scales down to zero instances, even after a long timeout. I couldn't find any documentation on this.

The app in this repo uses the `Function+` runtime. The "bare" runtime without the Azure Function layer is the sample app from [Spring GraalVM Native](https://github.com/spring-projects-experimental/spring-graalvm-native/tree/master/spring-graalvm-native-samples/function-netty) also running in Azure as a conatiner web app (UI looks very similar). The difference is that with the Azure Function layer there is a .NET Core app that sits in the same container and proxies requests down to the Spring Boot app. I haven't really figured out why that's a good idea yet (maybe something to do with the function triggers?). It's clearly expensive (about 500ms slower than the "bare" Azure container runtime).

## Configuration

The *local.settings-example.json* is provided to show what values the app is expecting to read from environment variables. Make a copy of *local.settings-example.json* and rename it *local.settings.json* and replace any values that begin with "**YOUR_**" with your values.

## Pre-reqs

- Java 8
- Maven (for packaging)

## Build and Run

### Build + Run the Java API standalone

- Use the following configuration in your `launch.json` to run the Java API:

```json
 "configurations": [
    {
      "type": "java",
      "name": "Debug (Launch)",
      "request": "launch",
      "mainClass": "",
      "env": {
        "FUNCTIONS_HTTPWORKER_PORT": 5001
      }
    },
```

- Hit the endpoints at `http://localhost:5001/trigger`

### Run Locally with Functions

- Package the Java api into a jar file:

```bash
(cd worker; mvn package)
```

- In a terminal run:

```bash
func start
```

> All being well you should see the Spring ascii logo, where the functions runtime has started the process.

- Hit the endpoints at `http://localhost:7071/api/trigger`

### Build a Container

To run the app in a JVM use this `host.json`:

```json
{
  "version": "2.0",
	"httpWorker": {
		"description": {
			"arguments": ["-jar"],
			"defaultExecutablePath": "/usr/local/openjdk-8/bin/java",
			"defaultWorkerPath": "worker/target/worker-1.0.jar"
		}
	},
	"extensionBundle": {
        "id": "Microsoft.Azure.Functions.ExtensionBundle",
        "version": "[1.*, 2.0.0)"
    }
}
```

and build an image:

```bash
(cd worker; ./mvnw clean install) && docker build -t dsyer/simple-function .
```

Run it locally:

```bash
docker run --rm -p 8080:80 dsyer/simple-function
```

You will see a .NET Core app start upo and listen on port 80:

```
info: Host.Triggers.Warmup[0]
      Initializing Warmup Extension.
info: Host.Startup[503]
      Initializing Host. OperationId: 'b3357bf2-27fa-4c5b-ad7f-99fe3affadf7'.
...
info: Host.Startup[0]
      Found the following functions:
      Host.Functions.AnotherTrigger
      Host.Functions.SimpleHttpTrigger
      
info: Microsoft.Azure.WebJobs.Script.WebHost.WebScriptHostHttpRoutesManager[0]
      Initializing function HTTP routes
      Mapped function route 'api/trigger' [get,post] to 'trigger'
...
Hosting environment: Production
Content root path: /
Now listening on: http://[::]:80
Application started. Press Ctrl+C to shut down.
...
```

Also embedded in the logs you will see the Spring Boot app starting up:

```
info: Host.Function.Console[0]
        .   ____          _            __ _ _
info: Host.Function.Console[0]
       /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
info: Host.Function.Console[0]
      ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
info: Host.Function.Console[0]
       \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
info: Host.Function.Console[0]
        '  |____| .__|_| |_|_| |_\__, | / / / /
info: Host.Function.Console[0]
       =========|_|==============|___/=/_/_/_/
info: Host.Function.Console[0]
       :: Spring Boot ::                        
info: Host.Function.Console[0]
      2020-06-01 11:16:59.566  INFO 156 --- [           main] com.example.App                          : Starting App on 1259c61f5e68 with PID 156 (/home/site/wwwroot/worker/target/worker started by root in /home/site/wwwroot)
...
```

### Build a Native Image

Ensure you have the GraalVM `native-image` command on your path and then build the image:

```bash
(cd worker && ./mvnw clean install && compile.sh)
```

Build a container as before and use this `host.json`:

```json
{
	"version": "2.0",
	"httpWorker": {
		"description": {
			"defaultExecutablePath": "./worker"
		}
	},
	"extensionBundle": {
        "id": "Microsoft.Azure.Functions.ExtensionBundle",
        "version": "[1.*, 2.0.0)"
    }
}
```

Be sure to set `WEBSITE_RUN_FROM_PACKAGE=1` in the function app settings so that it doesn't squash the executable bit when it deploys. Deploy like this (once the native image is built):

```
./mvnw package -P native && \
(cd target/azure-functions/funappnative; func azure functionapp publish funappnative --java --force)
```


### Run Custom Container in Azure Function App

The container can be run in Azure by creating a ["Function App"](https://portal.azure.com/#blade/HubsExtension/BrowseResource/resourceType/Microsoft.Web%2Fsites/kind/functionapp) and then modifying the container image configuration (e.g. in the UI) to point to the image built above. There doesn't seem to be a way to specify the container image until after the app is deployed - it is created with a default container image to start with. The UI for "Function Apps" looks identical to the ["App Services"](https://portal.azure.com/#blade/HubsExtension/BrowseResource/resourceType/Microsoft.Web%2Fsites) UI. That's the one we used to run a Spring Boot container without the "Function App" wrapper.

## Wash Up

Whilst the Azure platform documentation is extensive, there are some inconsistencies, or maybe missing pieces. It was hard work getting anything to run at all, nevermind persuading it to run in Azure.

* These [sample apps](https://docs.microsoft.com/azure/azure-functions/functions-custom-handlers) are referred to in the Azure platform docs. They don't use a docker container though, and they don't themselves contain any information about how to deploy the apps to Azure.

* Several references are made to the `func init --docker` option, but it doesn't know about the Java runtime. [Dockerhub](https://hub.docker.com/_/microsoft-azure-functions-base) definitely has references to Java though, and that's kind of how we got the samples in this repo missing. The Java base container has a JDK, and `JAVA_HOME` is set, but `java` is not on the `PATH` (`<shrug/>`) hence the slightly weird `host.json`.

* There is another section with a [Maven archetype](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-function-linux-custom-image?tabs=bash%2Cportal&pivots=programming-language-java) that generates a `Dockerfile`, but it doesn't use the .NET proxy. Maybe that's the difference between a "custom container" and a "custom handler"? Not clear.

* The role of the .NET proxy in the docker base images is unclear, and it seems to be getting in the way more than anything really. The contract between it and the backend app is not explained anywhere, but you can grope your way to something that works by following the example here, which in turn is copied from a Microsoft sample that doesn't explain how to run itself on the platform.

* Is there a scale to zero? Am I being charged for all the resources behind a custom handler, even while it isn't being triggered. Hard to tell. It's certainly quite hard to observe a true cold start - there are some "slower" starts, and some "slow" starts, but none is as slow as starting a Spring Boot app in a constrained environment, so probably the app was already running, and passivated in some way that isn't explained. And none is as fast as a locally running JVM even (nevermind the native images). The .NET proxy adds about 500ms of latency to every request, which puts it well behind. Without the poxy the latency is about the same as equivalent features in AWS and GCP (but they also display true "cold starts").

* The Maven plugin seems to destroy the app when it deploys a native image. Probably it resets the executable bit in the zip file it uploads or something. To work around that we had to use the `func` CLI directly.

* Running a custom handler with the default Function Host from Azure seems to be no better than creating a custom container, so I expect it's the same code path (the .NET proxy adds a lot of latency).