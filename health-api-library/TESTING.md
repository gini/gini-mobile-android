Testing the Gini Health API Library
===================================

### Instrumented tests

The instrumented tests also contain integration tests which require some test properties.

Before running the tests you will need to create a `health-api-library/library/local.properties` with the following:
```
testClientId=*******
testClientSecret=*******
testApiUri=https://pay-api.gini.net
testUserCenterUri=https://user.gini.net
```