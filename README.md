# Cascara  Common

## Building

### Without Tests or Javadoc

```
./gradlew clean build -x javadoc -x test -x testClasspath -x testJarClasspath
```

## Testing

### Modular: Class Files in JPMS Environment

```
./gradlew -x javadoc :cascara-common:test --tests "*test_commonBehaviorWorkOnBoth" --rerun-tasks
```

### Non-Modular: Class Files in non-JPMS Environment

```
./gradlew -x javadoc -x :cascara-common:test :cascara-common:testClasspath --tests "*test_commonBehaviorWorkOnBoth" --rerun-tasks
```

### Non-Modular: JAR file in non-JPMS environment

```
 ./gradlew -x javadoc -x :cascara-common:test :cascara-common:testJarClasspath --tests "*test_commonBehaviorWorkOnBoth" --rerun-tasks
 ```
