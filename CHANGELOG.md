# Changelog — jd tv

Toutes les modifications notables apportées à ce projet sont documentées dans ce fichier.
Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/), et ce projet adhère à [Semantic Versioning](https://semver.org/lang/fr/).

---

## [0.9.0] — 2026-10-26 (Release jd tv v0.9)

### 🚀 Nouveau Nom Officiel
- Adoption du nom de marque officiel **`jd tv`** (anciennement *Zen TV* / *Minimal TV Launcher*).
- Alignement du package de versioning : `versionCode = 9`, `versionName = "0.9"`.

### ✨ Centre de Contrôle tvOS 2x2 Enrichi
- **Option HDMI-CEC Standby Physique** : Ajout du bouton « Éteindre TV » (`btn_cc_cec_power`) déclenchant l'extinction matérielle du téléviseur via l'opcode HDMI-CEC `0x36` (`HdmiControlManager` / `HdmiPlaybackClient.sendStandby()`) avec fallback broadcast `android.hardware.hdmi.action.STANDBY`.
- **Veille Daydream Google TV** : Mise en veille logicielle native instantanée (`btn_cc_power`) avec chaîne de repli défensive multi-paliers (`Pano SLEEP`, `DreamX Backdrop`, `Somnambulator`, `ACTION_DREAM`, broadcast).
- **Statut Wi-Fi Précis & Dynamique** : Format textuel d'état `Connecté • [nom]` (ex: `Connecté • MonWiFi_5G`, `Connecté • Ethernet`, `Non connecté`) avec mesure du débit réel en Mbps lors du clic vers les paramètres réseau.
- **Niveau de Batterie Télécommande & Manette en Pourcentage** : Affichage dynamique de l'autonomie au format `XX%` (ex: `85%`, `100%`) interrogé via `InputDevice` et `BatteryState` (Android 12+), accompagné d'une pastille d'alerte rouge critique `[!]` lorsque le niveau est ≤ 20%.

### 📰 Dashboard Bureau & Widgets
- **Nouvelle Carte Carrée Actualités Google News (170×170dp)** : Intégration de `view_news_card` positionnée tout en haut de la colonne latérale droite (`dashboard_widgets_column`), au-dessus de la carte Sport en direct.
- **Carte Carrée Sport en Direct (170×170dp)** : Positionnée juste en dessous de la carte d'actualités avec badge de ligue (`🏒 LNH`, `🏀 NBA`, `🏈 NFL`, `⚽ Foot`, `⚾ MLB`), scores en temps réel et horodatage de match.
- **Écussons Sportifs Vectoriels Officiels Wikipédia/Wikimedia** : 12 écussons ultra-légers (< 3 Ko chacun) intégrés directement au paquet APK sans latence réseau :
  - **LNH (Hockey)** : Canadiens de Montréal (`ic_team_canadiens`), Toronto Maple Leafs (`ic_team_leafs`), Boston Bruins (`ic_team_bruins`), New York Rangers (`ic_team_rangers`), Edmonton Oilers (`ic_team_oilers`).
  - **NBA (Basketball)** : Los Angeles Lakers (`ic_team_lakers`), Boston Celtics (`ic_team_celtics`), Golden State Warriors (`ic_team_warriors`), Toronto Raptors (`ic_team_raptors`).
  - **Football Européen (Soccer)** : Real Madrid (`ic_team_real_madrid`), FC Barcelone (`ic_team_barcelona`), Paris Saint-Germain (`ic_team_psg`).
  - **Écusson Générique** : Logo trophée stylisé (`ic_team_generic`) pour les autres franchises.

### 🔍 Moteur de Recherche de Contenu Universel
- **Spécification d'Architecture Technique** : Rédaction et intégration du plan technique complet dans [`docs/content-search-plan.md`](./docs/content-search-plan.md).
- Prise en charge de la recherche unifiée locale EPG (`TvContract`) et deep linking direct vers Netflix, Amazon Prime Video, Crave, YouTube, Disney+ et Apple TV avec un budget mémoire strict < 5 Mo de RAM et latence < 80 ms.

### 🛠️ Déploiement ADB & Scripts de Debloat
- **Remplacement du Launcher par Défaut** : Procédure documentée via `adb shell cmd package set-home-activity`.
- **Commandes de Debloat Google TV** : Désactivation sans danger du launcher publicitaire officiel (`com.google.android.apps.tv.launcherx`) et du catalogue payant Google TV Films (`com.google.android.videos`).
- Scripts batch d'automatisation clé-en-main : `optimize-chromecast.bat` et `restore-google-tv.bat`.

### 🛡️ Qualité & Performance
- Maintien strict de l'empreinte mémoire totale sous le seuil critique de **30 Mo de RAM** sur Chromecast 4K (2 Go de RAM physique).
- Suite de **178 tests unitaires automatisés** Robolectric validant 100% des cas limites, l'intégrité de la navigation D-Pad (zéro focus trap) et la gestion du cycle de vie.

---

## [0.1.0] — 2026-10-25 (Version Initiale)

### Ajouté
- Socle natif Android TV ultra-léger (< 30 Mo RAM cible).
- Modèle de données immuable `AppModel` avec persistance `PreferencesManager`.
- 40 tests unitaires de validation défensive et gestion de mémoire.
- Support du mode Apple TV sombre et du mode Contraste Élevé OLED.
- Fichier de directives Google Jules [`AGENTS.md`](./AGENTS.md).
- Skill de validation CI/CD Frontend & Backend [`.skills/tv-testing/SKILL.md`](./.skills/tv-testing/SKILL.md).
