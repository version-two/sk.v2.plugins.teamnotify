# Builds the TeamNotify plugin with the Java 21 toolchain required by TeamCity 2026.1.
# The image ships JDK 21, so the Gradle toolchain uses it directly (no foojay download).
#
#   Build and export the plugin zip to ./out :
#     docker build --target artifact --output type=local,dest=out .
#
#   Or just run the build (artifact stays inside the image):
#     docker build --target build -t team-notify-build .
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Cache the Gradle distribution download in its own layer (invalidated only when the
# wrapper changes). `--version` does not configure the project, so sources aren't needed yet.
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon --version

# Run the tests and build the plugin distribution zip, so the exported artifact is verified
COPY . .
RUN ./gradlew --no-daemon clean test serverPlugin

# Export-only stage: `--output type=local,dest=out` writes the zip(s) to the host.
FROM scratch AS artifact
COPY --from=build /workspace/build/distributions/ /
