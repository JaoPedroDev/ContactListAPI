FROM eclipse-temurin:21

# Set the working directory in the container
WORKDIR /app

# Copy the jar file from your build folder to the container
COPY target/contactapi-1.0.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
