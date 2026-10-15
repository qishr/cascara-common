# Cascara  Common

## Building

### Without Tests or Javadoc

```
./gradlew clean build -x javadoc -x test -x testClasspath -x testJarClasspath
```

## Testing

### All Unit Tests

```
./gradlew -x javadoc test -x testClasspath -x testJarClasspath
```

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

### All Integration Tests

```
 ./gradlew :integration-test:test
```

Specific test

```
./gradlew :integration-test:test --tests "*test_subclassDeserializationSameFields"
```

