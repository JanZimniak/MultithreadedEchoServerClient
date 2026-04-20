## Multithreaded Echo Server & Client
Server allows for many client connections with blocking after defined amount. Clients have a simple data corruption check.

## Requirements:
- Java 17+
- Maven 3.6+

## Build:
```
mvn package
```
## Run:
### Client:
- Arguments: <IP_ADDRESS> <PORT>
```
java -cp target/programowaniesieciowe-1.0.jar projekt.Client localhost 5555

```
or
```
mvn exec:java -dexec.mainclass=projekt.Client -dexec.args="localhost 5555"
```
### Server:
- Arguments: <PORT>
```
java -cp target/programowaniesieciowe-1.0.jar projekt.Server 5555

```
or
```
mvn exec:java -dexec.mainclass=projekt.Server -dexec.args="5555"
```
