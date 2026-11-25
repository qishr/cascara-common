# Cascara  Common

## Building

### Without Tests or Javadoc

```
./gradlew -x javadoc -x testWithoutJPMS -x test
```

## Testing

### All Tests

```
./gradlew -x javadoc :cascara-common-lib:check :integration-test:check
```

### Unit Tests

Unit tests run using class files on exploded class path or moule path.

#### All Unit Tests

```
./gradlew -x javadoc :cascara-common-lib:check
```

#### Unit Tests in JPMS Environment

```
./gradlew -x javadoc :cascara-common-lib:test
```

#### Unit Tests in non-JPMS Environment

```
./gradlew -x javadoc -x test :cascara-common-lib:testWithoutJPMS
```

### Integration Tests

Integration tests run using JAR files on the class path or module path.

#### All Integration Tests

```
 ./gradlew :integration-test:check
```

#### Integration Tests in JPMS Environment

```
./gradlew -x javadoc :integration-test:test
```

#### Integration Tests in non-JPMS Environment

```
 ./gradlew -x javadoc -x test :integration-test:testWithoutJPMS
```

### Specific test

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