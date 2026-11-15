# Cascara  Common

## Building

### Without Tests or Javadoc

```
./gradlew -x javadoc -x testWithoutJPMS -x testJarJPMS -x testJarWithoutJPMS -x test
```

## Testing

### All Tests

```
./gradlew -x javadoc :cascara-common:check :integration-test:test
```

### All Unit Tests

```
./gradlew -x javadoc :cascara-common:check
```

### Modular Unit Tests

#### Class Files in JPMS Environment

```
./gradlew -x javadoc :cascara-common:test --tests "*EnvironmentAwareTests*" --rerun-tasks
```

#### JAR File in JPMS Environment

```
./gradlew -x javadoc -x test :cascara-common:testJarJPMS --tests "*EnvironmentAwareTests*" --rerun-tasks
```

### Non-Modular Unit Tests

#### Class Files in non-JPMS Environment

```
./gradlew -x javadoc -x test :cascara-common:testWithoutJPMS --tests "*EnvironmentAwareTests*" --rerun-tasks
```

#### JAR File in non-JPMS Environment

```
 ./gradlew -x javadoc -x test :cascara-common:testJarWithoutJPMS --tests "*EnvironmentAwareTests*" --rerun-tasks
 ```

### All Integration Tests

```
 ./gradlew :integration-test:test
```

Specific test

```
./gradlew :integration-test:test --tests "*test_subclassDeserializationSameFields"
```

---

Open Source Licenses
====================

This product may include a number of subcomponents with separate
copyright notices and license terms. Your use of the source code for
these subcomponents is subject to the terms and conditions of the
subcomponent's license, as noted in the LICENSE-<subcomponent>.md
files.