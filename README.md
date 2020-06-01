
# Azure Functions custom handler in Java

The sample in this folder demonstrates how to implement a [custom handler](https://docs.microsoft.com/azure/azure-functions/functions-custom-handlers) in Java. The code is adapted and simplified from a [Microsoft sample](https://github.com/Azure-Samples/functions-custom-handlers), with the addition of a `Dockerfile` (strangely missing in the original sample) and some configuration for building a GraalVM Native Image.

Example functions featured in this repo include:

| Name | Trigger | Input | Output |
|------|---------|-------|--------|
| [SimpleHttpTrigger](SimpleHttpTrigger) | HTTP | n/a   | n/a |
| [AnotherTrigger](SimpleHttpTriggerWithReturn) | HTTP | HTTP |HTTP |

We meaured response times to the app deployed in Azure:

```bash
time curl -v https://funapp-dsyer.azurewebsites.net/api/AnotherTrigger -H "Content-Type: application/json" -d '{"Data":{"Value":"Foo"}}'
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

- Hit the endpoints at `http://localhost:5001/SimpleHttpTrigger`

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

- Hit the endpoints at `http://localhost:7071/api/SimpleHttpTrigger`

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
      Mapped function route 'api/AnotherTrigger' [get,post] to 'AnotherTrigger'
      Mapped function route 'api/SimpleHttpTrigger' [get,post] to 'SimpleHttpTrigger'
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

Build a container as before and use thie `host.json`:

```json
{
	"version": "2.0",
	"httpWorker": {
		"description": {
			"defaultExecutablePath": "worker/target/worker"
		}
	},
	"extensionBundle": {
        "id": "Microsoft.Azure.Functions.ExtensionBundle",
        "version": "[1.*, 2.0.0)"
    }
}
```

### Run in Azure Function App

The container can be run in Azure by creating a "Function App" and then modifying the container image configuration (e.g. in the UI) to point to the image we just built.