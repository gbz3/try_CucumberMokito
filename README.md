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

```shell
$ mvn test
...
#### actual:expected = IBM1047:xIBM1047
####         0x0   0x1   0x2   0x3   0x4   0x5   0x6   0x7   0x8   0x9   0xA   0xB   0xC   0xD   0xE   0xF
#### 0x00> 00:00 01:01 02:02 03:03 37:37 2D:2D 2E:2E 2F:2F 16:16 05:05 15:15 0B:0B 0C:0C 0D:0D 0E:0E 0F:0F
#### 0x10> 10:10 11:11 12:12 13:13 3C:3C 3D:3D 32:32 26:26 18:18 19:19 3F:3F 27:27 1C:1C 1D:1D 1E:1E 1F:1F
#### 0x20> 40:40 5A:5A 7F:7F 7B:7B 5B:5B 6C:6C 50:50 7D:7D 4D:4D 5D:5D 5C:5C 4E:4E 6B:6B 60:60 4B:4B 61:61
#### 0x30> F0:F0 F1:F1 F2:F2 F3:F3 F4:F4 F5:F5 F6:F6 F7:F7 F8:F8 F9:F9 7A:7A 5E:5E 4C:4C 7E:7E 6E:6E 6F:6F
#### 0x40> 7C:7C C1:C1 C2:C2 C3:C3 C4:C4 C5:C5 C6:C6 C7:C7 C8:C8 C9:C9 D1:D1 D2:D2 D3:D3 D4:D4 D5:D5 D6:D6
#### 0x50> D7:D7 D8:D8 D9:D9 E2:E2 E3:E3 E4:E4 E5:E5 E6:E6 E7:E7 E8:E8 E9:E9 AD:AD E0:E0 BD:BD 5F:5F 6D:6D
#### 0x60> 79:79 81:81 82:82 83:83 84:84 85:85 86:86 87:87 88:88 89:89 91:91 92:92 93:93 94:94 95:95 96:96
#### 0x70> 97:97 98:98 99:99 A2:A2 A3:A3 A4:A4 A5:A5 A6:A6 A7:A7 A8:A8 A9:A9 C0:C0 4F:4F D0:D0 A1:A1 07:07
...
```

## Log4j2

- [Apache Log4j](https://logging.apache.org/log4j/2.x/)
- [Apache Log4j 2](https://logging.apache.org/log4j/2.12.x/)
- [Log4j 2ログシステムの基礎となる概念の理解](https://qiita.com/KentOhwada_AlibabaCloudJapan/items/5fb11b26513bfe7e0eed)
- 
