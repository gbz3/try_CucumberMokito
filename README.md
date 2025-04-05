# try_CucumberMokito

## Cucumber

- [10-minute tutorial](https://cucumber.io/docs/guides/10-minute-tutorial/?lang=java)

```shell
$ mvn --version
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /opt/apache-maven-3.9.9
Java version: 23.0.2, vendor: GraalVM Community, runtime: /opt/graalvm-community-openjdk-23.0.2+7.1
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "5.15.167.4-microsoft-standard-wsl2", arch: "amd64", family: "unix"
$ mvn archetype:generate                     \
"-DarchetypeGroupId=io.cucumber"           \
"-DarchetypeArtifactId=cucumber-archetype" \
"-DarchetypeVersion=7.21.1"                \
"-DgroupId=hellocucumber"                  \
"-DartifactId=hellocucumber"               \
"-Dpackage=hellocucumber"                  \
"-Dversion=1.0.0-SNAPSHOT"                 \
"-DinteractiveMode=false"
```

