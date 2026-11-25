# Déploiement du backend sur Google Cloud Run

Prérequis : un projet Google Cloud avec facturation activée, l'outil `gcloud`, et une clé API Gemini
(https://aistudio.google.com/apikey).

## 1. Configurer le projet
```bash
gcloud config set project VOTRE_PROJET
gcloud services enable run.googleapis.com secretmanager.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
```

## 2. Stocker la clé Gemini dans Secret Manager
```bash
printf "%s" "VOTRE_CLE_GEMINI" | gcloud secrets create gemini-api-key --data-file=-
PROJECT_NUMBER=$(gcloud projects describe $(gcloud config get-value project) --format='value(projectNumber)')
gcloud secrets add-iam-policy-binding gemini-api-key \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

## 3. Construire et déployer (depuis la racine du projet)
```bash
gcloud run deploy waste-sorter-backend \
  --source . \
  --region northamerica-northeast1 \
  --allow-unauthenticated \
  --set-secrets GEMINI_API_KEY=gemini-api-key:latest \
  --set-env-vars GEMINI_MODEL=gemini-2.5-flash,RATE_LIMIT_PER_MINUTE=20 \
  --memory 512Mi --max-instances 3
```
`--source .` envoie le projet à Cloud Build, qui construit l'image à partir du `Dockerfile` situé à la racine
(premier déploiement : environ 5 minutes ; acceptez la création du dépôt Artifact Registry si `gcloud` le propose).
`northamerica-northeast1` correspond à Montréal, la région la plus proche d'Ottawa.
`--max-instances 3` plafonne le coût en cas d'abus.

La limite de débit utilise la **dernière** adresse de l'en-tête `X-Forwarded-For`, celle qu'ajoute Cloud Run
et que le client ne peut pas falsifier. Si vous placez un équilibreur de charge devant le service, adaptez
`clientKey()` dans `backend/.../Application.kt`.

## 4. Vérifier
```bash
curl https://VOTRE-SERVICE.run.app/api/v1/health
# {"status":"ok","rulesVersion":"ottawa-2026.1","aiConfigured":true}
```
Saisissez ensuite cette adresse dans **Réglages → Serveur d'identification** de l'application, puis
touchez « Tester la connexion ».

En cas de message « service d'identification indisponible » alors que le test indique « IA prête » :
la clé Gemini est probablement invalide ou son quota gratuit est épuisé. Consultez les journaux
(`gcloud run services logs read waste-sorter-backend --region northamerica-northeast1`) : la ligne
`Gemini request failed with HTTP 400/403` indique une clé invalide, `HTTP 429` un quota dépassé.

## Exécution locale
```bash
export GEMINI_API_KEY=...   # voir backend/.env.example
SKIP_ANDROID=true ./gradlew :backend:run
```

## Avant une diffusion publique
- Ajouter Firebase App Check (ou Play Integrity) pour que seule l'application puisse appeler l'API.
- Définir une alerte de budget dans Google Cloud.
- Signer l'APK de production avec votre propre clé (*Build → Generate Signed App Bundle / APK*).
