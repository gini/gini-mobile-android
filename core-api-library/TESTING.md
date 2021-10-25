Testing the Gini Internal Core API Library
==========================================

### Instrumented tests

The instrumented tests also contain integration tests which require some test properties.

Before running the tests you will need to create a `core-api-library/library/local.properties` with the following:
```
testClientId=*******
testClientSecret=*******
testApiType=DEFAULT
testApiUri=https://pay-api.gini.net
testUserCenterUri=https://user.gini.net
```