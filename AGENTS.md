# AGENTS.md — Directives pour Google Jules

Ce fichier définit les consignes, l'architecture et les protocoles de maintenance automatisée pour **Google Jules** sur ce dépôt.

---

## 1. Contexte du Projet & Contraintes Matérielles Absolues

* **Cible** : Chromecast with Google TV 4K (Nom de code : `sabrina`, SoC Amlogic, Android 14+).
* **Contrainte Critique** : **2 Go de RAM physique seulement**.
* **Empreinte Mémoire Maximale** : Le launcher **ne doit jamais dépasser 30 Mo de RAM**.
* **Architecture** : 100% Kotlin natif avec Android Views classiques (`RecyclerView`, `CardView`). **Interdiction d'ajouter des frameworks lourds (Jetpack Compose, Flutter, React Native)** pour préserver la mémoire et la réactivité à 60 fps.

---

## 2. Commandes de Validation & Build pour Jules

Chaque tâche exécutée par Jules dans sa VM Google Cloud doit être validée par les commandes suivantes :

```bash
# Exécution des suites de tests unitaires et défensifs
./gradlew testDebugUnitTest --continue

# Compilation de l'APK de release
./gradlew assembleRelease
```

---

## 3. Définition des 4 Tâches Automatisées de Google Jules

### 🔄 Tâche 1 : Mise à jour du Launcher & Dépendances
* **Objectif** : Mettre à jour les bibliothèques AndroidX (`core-ktx`, `appcompat`, `recyclerview`, etc.) et le plugin Gradle Android.
* **Règles de Jules** :
  1. Vérifier qu'aucune nouvelle dépendance transitive lourde n'est injectée.
  2. Exécuter `./gradlew testDebugUnitTest` pour s'assurer de l'absence de régression.
  3. Ouvrir une Pull Request avec le récapitulatif des versions mises à niveau.

---

### 🛡️ Tâche 2 : Audit de Sécurité & Intégrité
* **Objectif** : Scanner et sécuriser les surfaces d'attaque du launcher.
* **Règles de Jules** :
  1. **Permissions** : Vérifier que le `AndroidManifest.xml` ne déclare aucun droit excessif non justifié.
  2. **Network Security Config** : S'assurer que les flux distants (Bing Wallpaper API, Météo, Google News RSS) utilisent exclusivement `https://` avec certificats stricts.
  3. **Intents non exportés** : Toutes les activités internes ou composants sensibles doivent avoir `android:exported="false"`.

---

### 📲 Tâche 3 : Suivi des Mises à jour Système Android TV
* **Objectif** : Adapter le launcher aux nouvelles versions majeures du firmware Google TV (Android 14 -> 15+).
* **Règles de Jules** :
  1. Adapter le `compileSdk` et `targetSdk` aux dernières directives Google Play.
  2. Vérifier que la déclaration d'activité d'accueil (`HOME`, `LEANBACK_LAUNCHER`) reste conforme aux modifications de sécurité de Google TV.
  3. Tester la résilience face aux changements de politique de gestion de l'énergie en tâche de fond.

---

### 📐 Tâche 4 : Audit de Mise aux Normes & Accessibilité (WCAG TV)
* **Objectif** : Garantir la conformité ergonomique et l'accessibilité sur grand écran.
* **Règles de Jules** :
  1. **Contraste & Lisibilité** : Contrôler les ratios de contraste (minimum 4.5:1 en mode standard, et 7:1 en mode Contraste Élevé).
  2. **D-Pad Focus Integrity** : Vérifier que chaque vue interactive possède un `nextFocusDown`, `nextFocusUp`, etc. cohérent et ne bloque jamais la télécommande.
  3. **Lecteur d'écran** : Valider la présence de `contentDescription` explicites pour l'accessibilité TalkBack.
  4. **Zéro fuite mémoire** : Vérifier l'absence de références statiques vers le `Context` ou des Bitmaps non recyclés.

---

## 4. Protocole de Développement Multi-Agents (Branches & Intégration)

Pour tout agent travaillant sur le dépôt :

1. **Isolation par branche de fonctionnalité (Feature Branching)** :
   - Chaque sous-agent de développement **DOIT** créer et travailler sur sa propre branche dédiée (ex: `feature/dpad-focus`, `feature/bing-wallpaper`, `feature/widgets-status`, `feature/accessibility`).
   - Interdiction formelle de commiter directement sur la branche `main`.

2. **Phase Pré-PR obligatoire (Économie de charge & Détection amont)** :
   - Avant même de soumettre une Pull Request ou demander un merge :
     1. L'agent développeur rédige et valide localement ses tests unitaires.
     2. L'agent développeur délègue à un **sous-agent de test dédié (QA Tester Sub-Agent)** pour exécuter une passe défensive autonome (cas limites, stress test, vérification mémoire).
     3. La PR n'est émise que si le sous-agent QA donne son feu vert, évitant ainsi les rejets d'intégration et les cycles de reprise inutiles.

3. **Rôle du Sub-Agent Intégrateur ("Integrator / CI Gatekeeper")** :
   - Seul le sub-agent intégrateur est habilité à merger du code vers `main`.
   - **Procédure d'intégration obligatoire** (suivant le skill `.skills/tv-testing/SKILL.md`) :
     1. Rebase de la branche de fonctionnalité sur `main`.
     2. Résolution défensive d'éventuels conflits de fusion (XML, Gradle, Manifest).
     3. **Audit Backend** : Exécution stricte de `./gradlew testDebugUnitTest` (persistance, modèles, résilience réseau).
     4. **Audit Frontend** : Vérification D-Pad (absence de focus trap, 60 fps, accessibilité WCAG TalkBack & contraste).
     5. En cas d'échec : rejet immédiat avec rapport d'erreurs transmis à l'agent émetteur.
     6. En cas de succès : Merge vers `main`.

4. **Numérotation des Versions & Tagging Automatisé** :

   - Dès qu'une version fonctionnelle est prête pour release :
     - Validation 100% en VM isolée.
     - **Test Pré-production sur Google TV physique** obligatoire avant toute version majeure (`vX.0.0`) via `adb -s 192.168.2.100:38667 install -r ...`.
     - Incrémenter `versionCode` et `versionName` (SemVer `vX.Y.Z`) dans `app/build.gradle.kts`.
     - Créer un tag Git annoté (`git tag -a vX.Y.Z -m "..."`).
     - Mettre à jour `CHANGELOG.md` et pousser les tags sur `origin`.

5. **Clôture de Version & Documentation Complète (`DOCUMENTATION.md`)** :
   - Dès que l'implémentation et les tests sont terminés avec succès :
     - Un **sous-agent Documentation dédié** rédige un fichier complet `DOCUMENTATION.md` à la racine.
     - Contenu obligatoire : architecture technique, guide utilisateur de la télécommande, réglages d'accessibilité, gestion des widgets/Bing, procédure de déploiement ADB et instructions pour Google Jules.




