# Rename the package

We support renaming the libraries' package now, 
so our es-androidx-media3 libraries will not conflict with google's original one.

These package renamed libraries have a `-renamed` suffix on their version name. 
Such as: `com.endeavorstreaming.androidx-media:exoplayer:1.4.1-dr1-renamed`

## How to publish the package renamed version
run gradle command simply:
```shell
./gradlew publish -PrenamePackage=true -x test -x lintDebug
```
