plugins {
  application
  id("org.gradlex.reproducible-builds") version "1.0"
  id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
  version = "23.0.1";
  modules("javafx.controls", "javafx.fxml")
}

dependencies {
  implementation("org.xerial:sqlite-jdbc:3.47.0.0")
  implementation("se.michaelthelin.spotify:spotify-web-api-java:8.4.1")
  implementation("com.google.apis:google-api-services-youtube:v3-rev20241117-2.0.0")
  implementation("com.google.auth:google-auth-library-oauth2-http:1.30.0")
  implementation("com.google.guava:guava:33.3.1-jre")
}

application {
  mainClass = "isrcTools.Main"
}

tasks.compileJava {
    options.release = 23
}

configurations.all {
  resolutionStrategy {
      failOnNonReproducibleResolution()
  }
}

