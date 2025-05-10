# Contributing to IBM i Pipeline Steps

## Building
### Prerequisites
- Java 64 bits JDK 17 or 21 ([Adoptium's JDK](https://adoptium.net/fr/temurin/releases/?arch=x64&package=jdk&version=21) are a good option)
- [Apache Maven](https://maven.apache.org/install.html)
- Clone the repository

### Building
Run the following command from the project's root to compile and build the plugin
````shell
mvn clean package
````
Find the packaged plugin under `target/ibmi-steps.hpi`

### Running
Run the following command from the project's root to run Jenkins on port 8080 with the current plugin project.
````shell
mvn hpi:run
````

To specify the HTTP port, run:
````shell
mvn hpi:run -Djetty.port=PORT
````

### Debugging
Run the following command from the project's root to run Jenkins in debug mode.
````shell
mvnDebug hpi:run
````
Maven will wait for a debugger to connect on port 8000 before carrying on with the execution.

## Contributing
Everyone is welcome to contribute. Code, documentation, ideas...there is no small contribution!

Create a Pull Request to make your contribution:
1. Fork this repository
2. Commit and push changes to your fork
3. Create a Pull Request (PR)

Check out [Contributing to projects](https://docs.github.com/en/get-started/quickstart/contributing-to-projects) on the GitHub documentation for more information about this process.

## Contributors

Many thanks to everyone [who has contributed so far](https://github.com/jenkinsci/ibmi-steps-plugin/graphs/contributors) 🙏🏻

* [@sebjulliand](https://github.com/sebjulliand)
* [@strangelookingnerd](https://github.com/strangelookingnerd)
* [@AbhinavJha1023](https://github.com/https://github.com/AbhinavJha1023)