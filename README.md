# Scala Application

## Tasks
1. Run the application (see `#### Using Acceptance Test` section below) and check swagger docs
2. Try to use EP that allow return rates for given currency code
3. Find what is wrong with implementation and fix it
4. Next tasks will be provided by interviewer

## Project Setup

This project uses Scala. The current version is declared in the build.sbt file and you will want that installed. SDKMAN makes that easy:

- install sdkman: https://sdkman.io/install
- find a java sdk version via sdk list java and install it, for example: sdk install java 11.0.4.hs-adpt.
- install sbt sdk install sbt
- install scala sdk install scala 2.13.3
  For compatibility between different scala and jdk versions go to: https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html

Import the project to intellij IDEA (Import, not just open, you'll save yourself a lot of trouble).
We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) as your IDE.
When importing and setting up the JDK and Scala SDK, pick the distributions installed previously.
By default, sdkman puts them in: /Users/<user_name>/.sdkman/candidates/java/11.0.4.hs-adpt and /Users/<user_name>/.sdkman/candidates/scala/2.13.3

If you run into issues while resolving dependencies via sbt, go to sbt settings and check the `use sbt shell for imports` setting.

### Language and tools
- code is written in Scala, version 2.12.x (see `scalaVersion` in `build.sbt`)
- [sbt](https://www.scala-sbt.org/) is used for dependency management and packaging

#### Useful commands
SBT commands documentation: https://www.scala-sbt.org/1.x/docs/
- compile project `sbt compile`
- run tests
  - UTs only `sbt test`
  - ITs only `sbt it:testOnly`
  - ATs only `sbt it:testOnly`
  - all tests `sbt at:test`
  - run single test `sbt 'IntegrationTest / testOnly *TestHarnessConnectionsSpec'`
- run code formatting `sbt scalafmtAll`
- generate assembly JAR file with application `sbt clean compile assembly`
- to each command you can add `clean` after `sbt` to make sure it will clean all previous build artifacts before running given command

### Libraries and frameworks
Main libraries we use in project
- [Akka HTTP](https://doc.akka.io/docs/akka-http/current/introduction.html) to create HTTP web server backend and HTTP client backend
- [Tapir](https://tapir.softwaremill.com/en/latest/) for API description and specification
- [STTP](https://sttp.softwaremill.com/en/stable/) for HTTP client interface
- [Slick](https://scala-slick.org/docs/) for database access and generating in-code DB model representation
- [Circe](https://circe.github.io/circe/) for JSON serialization/deserialization
- [Flyway](https://flywaydb.org/) for database migrations

### Docker
We use docker to containerize application.
- `Dockerfile` contain recipe how to build docker image for application from `.jar` file produced by sbt assembly process
- `docker-compose.yml` contain definition of docker-compose service that is used to run application dependencies locally

To run application dependencies locally using docker-compose you can run following command:
```
docker-compose up -d
```

### Conventions and structure

- prefer immutable data structures
- for DB capabilities/queries we try to avoid writing raw SQL queries and use Slick representation wherever it is possible
- for JSON serialization/deserialization we use Circe and provide in case class companion object `implicit val` for `Encoder` and `Decoder`, or `Codec` if both are needed (semi-auto derivation) 
- for validation use [cats](https://typelevel.org/cats/) `Validated` data type
- for making changes in DB schema use Flyway migrations that are stored in `src/main/resources/db/migration` folder,
  together with flyway migration you need update Slick representation that you can find in `com/evidentid/database/model/Tables.scala`
- typically e.g. for HTTP API implementation we distinguish following components
  - `*Capability` - contain implementation of DB actions we want to perform
  - `*ApiProxy` - contain implementation of protocol that is used to communicate with upstream service
  - `*Manager` - contain business logic, it can use `*Capability` to perform DB interactions or `*ApiProxy` to perform required interaction with upstream service 
  - `*Route`- definition of HTTP endpoints, trigger execution of `*Manager` business logic

#### Project structure
- `application` package contain application entrypoint and application logic divided into domain oriented sub-packages
- `database` package contain all code related to database access and setup without actual application DB capabilities
  - `model` contain `Tables.scala` where you can find slick representation of declared in flyway migration DB tables
- `http` package contain all code related to HTTP server and client setup
- `logging` package contain all code related to logging setup

## Development

### Relational Database Management

Schema migrations are handled via [Flyway](https://flywaydb.org/) and applied
automatically by the service when it starts. For Integration Testing, there is
a Spec file that checks the connectivity and migration capability. Migrations
are located in `src/main/resources/com/evidentid/db/migrations` for SQL-based
migrations. See the Flyway docs if you want to add programmatic migrations.

To check query generated by Slick (e.g. to run EXPLAIN) you can use `statements` definition:
```
    import com.evidentid.database.DatabaseProfile.api._

    val simpleQuery = Tables.Entities.filter(_.displayName === "testname")
    val simpleQueryStatements = simpleQuery.result.statements
    print(simpleQueryStatements)

    val compiledQuery = databaseManager.compiledQueryThatWeWantAnalyse(("arg1", "arg2"))
    val compiledQueryStatements = compiledQuery.result.statements
    print(compiledQueryStatements)
```

### Data model visualisation
As DB schema is changing, and we're reaching >100 Flyway migration it is good to
have a way to visualise current state. You can use SchemaSpy for this purpose,
I've described how to use it [here](https://evidentid.atlassian.net/wiki/spaces/DOCUMENTAT/pages/2523214/Setup+of+your+computer+local+env+setup#Setupofyourcomputer%2Flocalenvsetup-Other) -
look for `SchemaSpy` phrase.


### Code formatting
This project use [Scalafmt](https://scalameta.org/scalafmt/) to keep consistent code formatting style.
Code formatting rules configuration is available in `scalafmt.conf` file
- to run code style check run: `sbt styleCheck`
- to reformat code according to `.scalafmt.conf` specification, run: `sbt styleFix`
- to configure intellij to use scalafmt formatter go to: Preferences > Editor > Code Style > Scala and
  select scalafmt as formatter (https://scalameta.org/scalafmt/docs/installation.html)

### Testing

Commands that can be used to run tests are listed in `Useful commands` section above.

We can distinguish three kind of tests:
- acceptance tests (`AT`)
  - tests that are testing application end to end and are interacting with it using exposed API
  - those tests should be placed in `com/evidentid/application/test/acceptance` package inside `src/it` directory
  - all acceptance tests should extend `AcceptanceFeatureSpec` that make sure that application is started before test and stopped after test and provide a bunch of useful helpers
- integration tests (`IT`)
  - tests that are testing services or bigger application parts and frequently rely on some runtime dependencies that are not mocked but provided e.g. as docker containers,
    such tests shouldn't use stubs/mocks or use them only if no other options are possible
  - those tests should be placed in `com/evidentid/application/test/integration` package inside `src/it` directory
  - all acceptance tests should extend `IntegrationSpec` that make sure that required dependencies are available and provide a bunch of useful helpers
- unit tests (`UT`)
  - quick tests that are testing exactly one thing in isolation, dependencies are stubbed/mocked to focus only for core assertions
  - those tests should be placed inside `src/test` directory in package corresponding to tested component
  - all acceptance tests should extend `UnitSpec` that provide a bunch of useful helpers and initialise all common instances e.g. ExecutionContext or ActorSystem


The integration harness is controlled via `docker-compose`. The services
it exposes are connectible from the host to avoid having
to bounce through a docker service to run integration tests. However, since
Jenkins has multiple executors per node, the services bind to ephemeral ports
on the host. Additionally, docker secrets control the user/passwd pair for
each service so that integration test code can programmatically access those
values. These credentials are used throughout the build to avoid hard-coded
copy pasta.  Use `docker-compose up -d` to start the PostgresQL, VerifyApi duplicate
and SchedulerAPI duplicate daemons.

We completely fake out the external interaction with both VerifyAPI and SchedulerAPI
for component
integration testing using [Mountebank](http://www.mbtest.org/). This beast runs
on Node.js and can be configured almost arbitrarily for that. We use it for a
simple Test Dummy that mindlessly accepts requests and responds as configured.

### Running application locally

#### Using Acceptance Test

In our acceptance tests, we're spawning fully functional application instance. You need to get the
webserver port (it is dynamically assigned) and ensure that the test will not finish e.g. by adding a long sleep in test implementation.

To accomplish this, add the following snippet in any acceptance test and run it:
```scala
// get application webserver address to be able interact with API
println(appBinding)
// long sleep so test won't finish - in this time we're able to use application
Thread.sleep(100000000)
```

------------------------------------------------------------------------------------------------------------------------
## API
All application API should be included in swagger specification.
Swagger specification is generated automatically based on Tapir endpoints definition (see `DocsRoute.scala` for more details).
Application expose swagger-ui under `/docs` path, in order to see currently exposed API, please use `SwaggerDocsFeature`,
where you find instructions how retrieve swagger yaml.
