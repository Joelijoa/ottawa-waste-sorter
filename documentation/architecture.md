# Architecture

```
Android (Compose, MVVM)  ──HTTPS multipart──▶  Backend Ktor (Cloud Run)  ──REST──▶  Gemini API
        │                                              │
        └──────── module partagé :rules ◀──────────────┘
                  (moteur déterministe + règles Ottawa 2026)
```

## Pourquoi un module de règles partagé
Le cahier des charges exige à la fois que **le backend applique les règles** et que **le guide et le tri
manuel fonctionnent hors ligne**. Le même module Kotlin pur est donc compilé dans le backend et dans
l'application : une seule source de vérité, aucune divergence possible. Le contrat d'API
(`rules/.../api/ApiModels.kt`) y vit aussi.

## Flux d'une identification par photo
1. Photo (CameraX) ou galerie (Photo Picker) → aperçu. **Aucun appel à l'IA avant confirmation.**
2. `ImageProcessor` : correction d'orientation EXIF, redimensionnement (1024 px), JPEG 80 %.
3. `POST /api/v1/identify` → le backend vérifie la taille, le type réel (signature binaire) et le débit.
4. Gemini reçoit l'image avec un **schéma JSON imposé** ; `AiResponseValidator` rejette toute réponse hors schéma.
5. `ClassificationEngine` détermine la destination de chaque composant.
6. Confiance < 0,6, information manquante ou aucune règle vérifiée ⇒ `needsUserInput` : l'application
   propose de préciser le matériau (nouvel appel avec `material`) ou de trier manuellement.
7. Le résultat est enregistré localement (Room), avec une miniature facultative.

## Application Android (`app/`)
| Paquet | Contenu |
|---|---|
| `core/network` | Retrofit + OkHttp, client recréé si l'URL du serveur change |
| `core/database` | Room : table `history` |
| `core/common` | `ImageProcessor` (compression, miniatures, suppression des photos temporaires) |
| `data/local` | DataStore (URL du serveur, miniatures) |
| `data/repository` | Implémentations des dépôts, conversion des erreurs HTTP en erreurs métier |
| `domain` | Modèles, interfaces de dépôts, cas d'utilisation (`ClassifyManuallyUseCase`, `IdentifyPhotoUseCase`) |
| `feature/*` | Écrans Compose : accueil, caméra, identification (aperçu, tri manuel, résultat), historique, guide, réglages |
| `navigation` | Navigation Compose + barre de navigation inférieure |

Injection de dépendances manuelle (`AppContainer`) : suffisante pour le MVP, sans bibliothèque supplémentaire.
La langue (anglais / français / langue de l'appareil) passe par le sélecteur de langue par application
d'AppCompat.

## Risques connus
- **Règles** : certaines consignes sont déduites des sources et marquées « À vérifier » (voir `ottawa-rules.md`).
- **Coût de l'IA** : limité par la compression, la confirmation obligatoire, l'absence de doublons et le
  plafond de débit par adresse IP. Ajouter Firebase App Check avant une diffusion publique.
- **API publique** : sans compte utilisateur, le backend est accessible à qui connaît son URL.
