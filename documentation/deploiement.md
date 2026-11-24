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
gcloud builds submit --config backend/cloudbuild.yaml .

gcloud run deploy waste-sorter-backend \
  --image gcr.io/$(gcloud config get-value project)/waste-sorter-backend \
  --region northamerica-northeast1 \
  --allow-unauthenticated \
  --set-secrets GEMINI_API_KEY=gemini-api-key:latest \
  --set-env-vars GEMINI_MODEL=gemini-2.5-flash,RATE_LIMIT_PER_MINUTE=20 \
  --memory 512Mi --max-instances 3
```
`northamerica-northeast1` correspond à Montréal, la région la plus proche d'Ottawa.
`--max-instances 3` plafonne le coût en cas d'abus.

## 4. Vérifier
```bash
curl https://VOTRE-SERVICE.run.app/api/v1/health
# {"status":"ok","rulesVersion":"ottawa-2026.1","aiConfigured":true}
```
Saisissez ensuite cette adresse dans **Réglages → Serveur d'identification** de l'application, puis
touchez « Tester la connexion ».

## Exécution locale
```bash
export GEMINI_API_KEY=...   # voir backend/.env.example
SKIP_ANDROID=true ./gradlew :backend:run
```

## Avant une diffusion publique
- Ajouter Firebase App Check (ou Play Integrity) pour que seule l'application puisse appeler l'API.
- Définir une alerte de budget dans Google Cloud.
- Signer l'APK de production avec votre propre clé (*Build → Generate Signed App Bundle / APK*).
