# Ottawa Waste Sorter

Application Android native qui indique dans quel bac jeter un objet à Ottawa, à partir d'une photo
(identification par Gemini, via un backend) ou d'une sélection manuelle (hors ligne).

| Module | Rôle | Technologies |
|---|---|---|
| `rules/` | Moteur de règles et règles officielles d'Ottawa 2026, versionnées et sourcées | Kotlin pur, kotlinx.serialization |
| `app/` | Application Android | Kotlin, Jetpack Compose, Material 3, MVVM, CameraX, Photo Picker, Retrofit/OkHttp, Room, DataStore |
| `backend/` | API REST : reçoit la photo, interroge Gemini, valide la réponse, applique les règles | Kotlin, Ktor, Cloud Run, Secret Manager |

Principe clé : **l'IA identifie l'objet, le moteur de règles décide du bac.** Changer de modèle d'IA ne
touche ni l'application ni les règles.

## Obtenir l'APK

### Option A : GitHub (aucune installation)
1. Créez un dépôt GitHub privé et envoyez-y ce dossier (bouton *Add file → Upload files*, ou `git push`).
2. Le workflow `.github/workflows/build-apk.yml` compile, lance les tests et publie
   `OttawaWasteSorter.apk` dans l'onglet **Releases** (et dans **Actions → Artifacts**).
3. Facultatif : ajoutez la variable `BACKEND_URL` (*Settings → Secrets and variables → Actions → Variables*)
   pour intégrer l'adresse du serveur dans l'APK.

### Option B : Android Studio
1. *File → Open* → choisir le dossier `OttawaWasteSorter`.
2. Attendre la synchronisation Gradle, puis *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
3. L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.

En ligne de commande : `./gradlew :app:assembleDebug -PbackendUrl=https://votre-backend.run.app`

### Installer sur le téléphone
Copiez l'APK sur le téléphone, ouvrez-le et autorisez l'installation depuis cette source.
Android 8.0 minimum.

## Ce qui fonctionne sans serveur
Guide de tri, recherche, tri manuel, historique, réglages, français et anglais.
L'identification par photo nécessite le backend (voir `documentation/deploiement.md`).

## Tests
```
./gradlew :rules:test :backend:test :app:testDebugUnitTest   # tests unitaires
./gradlew :app:connectedDebugAndroidTest                       # tests d'interface (appareil connecté, en anglais)
```
Le jeu de référence des 15 emballages du cahier des charges se trouve dans
`rules/src/test/.../ClassificationEngineTest.kt`.

## Documentation
- [Architecture](documentation/architecture.md)
- [API](documentation/api.md)
- [Règles d'Ottawa](documentation/ottawa-rules.md)
- [Déploiement du backend](documentation/deploiement.md)
