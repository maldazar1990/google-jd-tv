---
name: tv-testing
description: Protocole et suite d'audits de validation backend (moteur/données) et frontend (UI/D-Pad TV) avant tout merge sur la branche main.
---

# Skill de Test Complet : Backend & Frontend Android TV

Ce skill est exécuté obligatoirement par le sous-agent **Integrator & CI Gatekeeper** avant toute validation et fusion d'une branche vers `main`.

---

## 1. Tests Backend (Moteur, Données, Réseau & Performance)

### A. Persistance & Données
- [ ] **Tests Unitaires Unit/Stress** : Valider `PreferencesManager` et la corruption éventuelle du stockage avec `./gradlew testDebugUnitTest`.
- [ ] **Modèle Immuable** : Garantir que `AppModel` ne conserve aucune référence directe au `Context` Android (prévention des fuites mémoire).

### B. Moteur de Découverte d'Applications
- [ ] **Double Scan** : Vérifier que le scanner capture à la fois `CATEGORY_LEANBACK_LAUNCHER` et `CATEGORY_LAUNCHER` (applications sideloadées).
- [ ] **Fallback Bannière** : Valider la génération d'icônes par défaut en cas d'absence de bannière TV native sans crash ni blocage de thread.

### C. Réseau & Flux Externes (Bing, Météo, News)
- [ ] **Thread Séparé** : Aucune requête réseau ne doit s'exécuter sur le thread principal (UI thread).
- [ ] **Mode Déconnecté (Hors-Ligne)** : Si le Wi-Fi est coupé, le launcher doit basculer instantanément sur le dégradé sombre sans planter.

---

## 2. Tests Frontend (Interface, Focus D-Pad & Accessibilité TV)

### A. Intégrité de la Navigation Télécommande (D-Pad)
- [ ] **Navigation Bidirectionnelle** : Chaque carte d'application et bouton possède un focus cohérent (`nextFocusUp`, `nextFocusDown`, `nextFocusLeft`, `nextFocusRight`).
- [ ] **Piège à Focus (Focus Trap)** : Vérifier que l'utilisateur ne reste jamais bloqué dans un sous-menu ou dans l'en-tête.
- [ ] **Animation 60 fps** : Zoom doux `1.08x` avec élévation d'ombre fluide sur GPU modeste (Mali-G31).

### B. Accessibilité & Ergonomie TV (WCAG)
- [ ] **Mode Contraste Élevé** : Vérifier le basculement en noir pur OLED `#000000` et bordures jaune vif `#FFD600`.
- [ ] **Labels Vocaux (TalkBack)** : Vérifier la présence de `android:contentDescription` explicites sur toutes les vues interactives.
- [ ] **Grilles Modulaires** : Valider le rendu visuel sans chevauchement en 6 colonnes (Standard), 4 colonnes (Grand) et 3 colonnes (Très Grand).

---

## 3. Règle de Versioning Automatique (Release Tagging)

Dès qu'une version est déclarée prête et que l'ensemble des tests Backend & Frontend sont au vert :
1. **Incrémenter la version** dans `app/build.gradle.kts` :
   - `versionCode += 1`
   - `versionName = "X.Y.Z"` (selon SemVer)
2. **Poser un tag Git annoté** :
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z: [Résumé des changements]"
   git push origin main --tags
   ```
3. Mettre à jour `CHANGELOG.md` avec la liste des fonctionnalités validées.
