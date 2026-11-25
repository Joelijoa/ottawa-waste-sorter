# Image du backend pour Google Cloud Run.
# À la racine du projet car le backend dépend du module :rules.
# Utilisé automatiquement par : gcloud run deploy --source .
# Construction locale :         docker build -t waste-sorter-backend .
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
