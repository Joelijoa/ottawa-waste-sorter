# Image du backend pour Google Cloud Run.
# À construire depuis la RACINE du projet (le backend dépend du module :rules) :
#   docker build -f backend/Dockerfile -t waste-sorter-backend .
FROM gradle:8.11.1-jdk17 AS build
WORKDIR /src
COPY . .
ENV SKIP_ANDROID=true
RUN chmod +x gradlew && ./gradlew --no-daemon :backend:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/backend/build/install/backend /app
ENV PORT=8080
EXPOSE 8080
USER 1000
CMD ["/app/bin/backend"]
