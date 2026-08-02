# A13 Signing Configuration

This document records the repository-external signing configuration for `tomthenpc/customiuizer-a13`.

## Properties

```text
SigningDiscoveryMode: EXACT_CONFIG_ONLY
SigningGradleProperty: customiuizerA13KeystoreProperties
SigningEnvironmentVariable: CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES
ExpectedLocalProperties: C:\Users\tv\Documents\buildkey\r13\keystore.properties
RecursiveSigningSearch: forbidden
CrossProductKeyUse: forbidden
```

## Source of truth

The only supported ways to supply signing material are:

1. `customiuizerA13KeystoreProperties` Gradle property.
2. `CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES` environment variable.

The value must be a path to a `keystore.properties` file outside the repository.

## Required fields

The properties file must contain:

```properties
storeFile=<path to the keystore file>
storePassword=<keystore password>
keyAlias=<key alias>
keyPassword=<key password>
```

The actual keystore file is referenced by `storeFile`. No other source is valid.

## Behavior

- When a valid configuration is present, `release` and `develop` builds are signed.
- When no configuration is present, `debug` builds and unit tests still run.
- When no configuration is present, `release` and `develop` packaging tasks fail with an explicit message.

## Restrictions

- Do not commit `keystore.properties`, keystore files, or any signing secret to the repository.
- Do not search the filesystem for `*.jks`, `*.p12`, or other keystore files.
- Do not reuse A14 or any other product's signing material.
- Do not recursively scan `C:\Users\tv` or the `Documents` folder for keys.
