# 📺 Zen TV Launcher — Documentation Technique & Guide d'Utilisation

> **Version** : 1.0.0  
> **Cible matérielle** : Chromecast with Google TV 4K (Sabrina) & Android TV 14+  
> **Contrainte critique** : Empreinte mémoire < 30 Mo de RAM, 60 FPS constants, 0 framework lourd.

---

## Sommaire

1. [Architecture Technique & Philosophie Minimaliste](#1-architecture-technique--philosophie-minimaliste)
   - [1.1 Cible matérielle & Contraintes physiques](#11-cible-matérielle--contraintes-physiques)
   - [1.2 Empreinte mémoire et budget RAM strict (< 30 Mo)](#12-empreinte-mémoire-et-budget-ram-strict--30-mo)
   - [1.3 Stack 100% Android Views natif](#13-stack-100-android-views-natif)
   - [1.4 Composants clés du système](#14-composants-clés-du-système)
2. [Guide Utilisateur de la Télécommande D-Pad](#2-guide-utilisateur-de-la-télécommande-d-pad)
   - [2.1 Anatomie de la télécommande et navigation](#21-anatomie-de-la-télécommande-et-navigation)
   - [2.2 Le Dock d'accueil (5 applications favorites)](#22-le-dock-daccueil-5-applications-favorites)
   - [2.3 Le Tiroir complet d'applications (All-Apps Drawer)](#23-le-tiroir-complet-dapplications-all-apps-drawer)
   - [2.4 Accès aux widgets d'en-tête et Centre de Contrôle](#24-accès-aux-widgets-den-tête-et-centre-de-contrôle)
   - [2.5 Gestion de la touche Retour (Back)](#25-gestion-de-la-touche-retour-back)
   - [2.6 Actions au Clic Long (Menu contextuel)](#26-actions-au-clic-long-menu-contextuel)
3. [Centre de Contrôle tvOS & Réglages Système](#3-centre-de-contrôle-tvos--réglages-système)
   - [3.1 Présentation et ouverture du panneau latéral](#31-présentation-et-ouverture-du-panneau-latéral)
   - [3.2 Tuile Veille Instantanée (Daydream Native SLEEP Intent)](#32-tuile-veille-instantanée-daydream-native-sleep-intent)
   - [3.3 Tuile Wi-Fi & Réseau (SSID & Débit réel en Mbps)](#33-tuile-wi-fi--réseau-ssid--débit-réel-en-mbps)
   - [3.4 Tuile Télécommande / Manette & Alerte Batterie Critique](#34-tuile-télécommande--manette--alerte-batterie-critique)
   - [3.5 Options de Thèmes (Sombre LED, OLED Pur, Bing 4K)](#35-options-de-thèmes-sombre-led-oled-pur-bing-4k)
   - [3.6 Protection d'écran OLED & Normes d'Accessibilité WCAG](#36-protection-décran-oled--normes-daccessibilité-wcag)
4. [Widgets de l'Écran d'Accueil](#4-widgets-de-lécran-daccueil)
   - [4.1 Coexistence Google News Ticker & Carte Sport Carrée](#41-coexistence-google-news-ticker--carte-sport-carrée)
   - [4.2 Retrait du widget Wi-Fi de l'accueil](#42-retrait-du-widget-wi-fi-de-laccueil)
   - [4.3 Widget RAM dynamique en temps réel](#43-widget-ram-dynamique-en-temps-réel)
   - [4.4 Widget Météo Locale (Open-Meteo)](#44-widget-météo-locale-open-meteo)
5. [Procédure de Déploiement ADB & Instructions pour Google Jules](#5-procédure-de-déploiement-adb--instructions-pour-google-jules)
   - [5.1 Préparation et connexion ADB](#51-préparation-et-connexion-adb)
   - [5.2 Compilation et installation directe](#52-compilation-et-installation-directe)
   - [5.3 Commandes de validation pour Google Jules](#53-commandes-de-validation-pour-google-jules)
   - [5.4 Protocoles de maintenance automatisée](#54-protocoles-de-maintenance-automatisée)

---

## 1. Architecture Technique & Philosophie Minimaliste

### 1.1 Cible matérielle & Contraintes physiques

Le projet **Zen TV Launcher** est conçu spécifiquement pour adresser les lacunes d'ergonomie et de performances des téléviseurs connectés sous Android TV et Google TV, avec une cible d'étalonnage stricte :
* **Appareil de référence** : Chromecast with Google TV 4K (Nom de code : `sabrina`, processeur Amlogic S905X4 quad-core ARM Cortex-A55, GPU Mali-G31 MP2).
* **Mémoire vive physique** : **2 Go de RAM physique seulement**, partagée avec le système Android 14+ et les tampons d'affichage vidéo 4K.
* **Système d'exploitation** : Android 14 (API 34) et versions supérieures (rétrocompatible Android 8.0 Oreo / API 26).

Sur ce type d'appareil, le launcher officiel Google TV est lourd, surchargé de recommandations commerciales et consomme régulièrement plus de 150 à 250 Mo de RAM, provoquant du ralentissement (stutter) et la fermeture intempestive en tâche de fond des lecteurs multimédias (Kodi, Plex, Jellyfin, IPTV).

### 1.2 Empreinte mémoire et budget RAM strict (< 30 Mo)

L'engagement fondamental de Zen TV Launcher est une **sobriété absolue** :
* **Empreinte mémoire maximale autorisée** : **≤ 30 Mo de RAM** en régime de croisière.
* **Taux de rafraîchissement** : **60 FPS constants** sans aucune saccade (jank), même lors des défilements rapides de listes ou de l'ouverture du Centre de Contrôle.
* **Zéro fuite mémoire** : Cycle de vie hermétique, aucune rétention d'instance d'`Activity` ou de `Context` dans les singletons ou coroutines/threads en tâche de fond, utilisation exclusive de `context.applicationContext`.
* **Décodage Bitmap optimisé** : Toutes les affiches et arrière-plans sont sous-échantillonnés (`inSampleSize`) et décodés en configuration `Bitmap.Config.RGB_565` (2 octets par pixel au lieu de 4 en ARGB_8888), réduisant la mémoire allouée aux images de 50%.
* **Plafond de cache LRU** : Le cache d'images en mémoire est strictement borné à 4 Mo (`TopShelfManager.MAX_POSTER_CACHE_BYTES`).

### 1.3 Stack 100% Android Views natif

Pour respecter ces contraintes draconiennes de 2 Go de RAM :
* **Interdiction formelle de bibliothèques lourdes** : Pas de Jetpack Compose, pas de Flutter, pas de React Native, pas d'injection de dépendances complexe (Dagger/Hilt), pas d'ORM lourd (Room).
* **Android Views classiques ultra-légères** :
  - `RecyclerView` avec recyclage granulaire des cartes d'applications.
  - `CardView` et `FrameLayout` optimisés.
  - Animations de transitions gérées par `ViewPropertyAnimator` avec interpolateurs matériels légers (`DecelerateInterpolator`, `OvershootInterpolator`).
  - View Binding Android officiel (`buildFeatures { viewBinding = true }`).
* **Dépendances de production réduites à l'essentiel** :
  - `androidx.core:core-ktx:1.12.0`
  - `androidx.appcompat:appcompat:1.6.1`
  - `androidx.recyclerview:recyclerview:1.3.2`
  - `androidx.cardview:cardview:1.0.0`

### 1.4 Composants clés du système

```
                          ┌────────────────────────┐
                          │      MainActivity      │
                          │   (Orchestrateur UI)   │
                          └───────────┬────────────┘
                                      │
       ┌──────────────┬───────────────┼───────────────┬──────────────┐
       │              │               │               │              │
┌──────▼──────┐┌──────▼──────┐┌───────▼───────┐┌──────▼──────┐┌──────▼──────┐
│TopShelfMgr  ││DisplayResMgr││ SportsManager ││ NewsManager ││BluetoothMgr │
│(Leanback EPG││(HDMI, 4K,   ││(Scores live & ││(Google News ││(Manette &   │
│ & Curated)  ││ Daydream)   ││  Écussons)    ││ RSS stream) ││  Batterie)  │
└─────────────┘└─────────────┘└───────────────┘└─────────────┘└─────────────┘
                                      │
                          ┌───────────▼────────────┐
                          │   PreferencesManager   │
                          │(SharedPreferences/JSON)│
                          └────────────────────────┘
```

1. **`MainActivity`** :
   - Cœur de l'application gérant la boucle d'affichage, le cycle de vie Android TV (`singleTask`, `stateNotNeeded="true"`), l'écoute des paquets installés/désinstallés (`PACKAGE_ADDED`, `PACKAGE_REMOVED`), et la synchronisation des données d'en-tête.
2. **`TopShelfManager`** :
   - Interroge les fournisseurs de contenus Android TV standard `TvContract` (`content://android.media.tv/watch_next_program` et `content://android.media.tv/preview_program`).
   - Fournit un mécanisme de repli immersif 4K avec des visuels de haute qualité pour les plateformes majeures en cas d'absence d'EPG local (Netflix, Amazon Prime Video, Crave, YouTube, Spotify, Twitch, VLC).
3. **`DisplayResolutionManager`** :
   - Détecte la résolution réelle de l'écran (4K UHD 3840×2160, 1080p, 720p) et la fréquence de rafraîchissement HDMI (24Hz Cinéma, 50Hz Europe, 60Hz NTSC, 120Hz HDMI 2.1).
   - Identifie la capacité matérielle 4K du téléviseur physique (`display.supportedModes`) même si le boîtier Android TV restitue temporairement son interface en 1080p.
   - Calcule et applique les marges de sécurité d'overscan (5% à 10%) pour éviter le rognage des widgets sur les téléviseurs récalcitrants.
   - Intègre la séquence d'extinction/veille ultra-robuste Google TV.
4. **`SportsManager`** :
   - Récupère en arrière-plan sans clé d'API (requêtes HTTPS strictes vers les points de terminaison publics ESPN) les scores sportifs en direct (LNH, NBA, NFL, Soccer Premier League, MLB).
   - Intègre les logos d'équipes vectoriels légers intégrés au paquet APK (`ic_team_canadiens`, `ic_team_leafs`, `ic_team_lakers`, `ic_team_celtics`, `ic_team_real_madrid`, `ic_team_barcelona`, `ic_team_generic`).
5. **`NewsManager`** :
   - Analyseur RSS Google News ultra-léger basé sur `XmlPullParser` (zéro allocation de modèle lourd DOM), filtrant les actualités par thématiques (Technologie, Sports, Divertissement, Sciences, Monde, Économie) et par sources journalistiques sélectionnées.
6. **`BluetoothDeviceManager`** :
   - Détecte en temps réel la télécommande vocale Google TV ou les manettes de jeu connectées via `InputDevice` (Android 12+) et les profils Bluetooth natifs.
   - Surveille le niveau de charge de batterie sans drainer la pile Bluetooth.
7. **`PreferencesManager`** :
   - Couche de persistance thread-safe basée sur `SharedPreferences`.
   - Écriture asynchrone non-bloquante (`apply()`) pour préserver les 60 FPS, et synchronisation défensive avec reprise sur corruption JSON pour la liste ordonnée des favoris et les applications masquées.

---

## 2. Guide Utilisateur de la Télécommande D-Pad

Zen TV Launcher est conçu pour une navigation 100% au D-Pad (flèches directionnelles), sans jamais nécessiter de pointeur de souris ou d'écran tactile.

```
                  ┌───────────────────────┐
                  │    [ HAUT ]           │ ───> Accès Direct : Centre de Contrôle
                  │                       │      & Réglages en-tête (depuis le Dock)
    Navigation    │ [GAUCHE] [OK] [DROITE]│ ───> Défilement des Apps & Lancement (OK)
     du D-Pad     │                       │      Clic Long OK : Menu contextuel
                  │    [ BAS ]            │ ───> Ouverture du Tiroir complet (All-Apps)
                  └───────────────────────┘
                               │
                       [ RETOUR / BACK ]  ───> Ferme le Tiroir / Centre de Contrôle
                                               Retour immédiat au Dock (anti-quitter)
```

### 2.1 Anatomie de la télécommande et navigation

* **Flèches Gauche / Droite** : Déplacement horizontal entre les vignettes d'applications sur le Dock ou dans les rangées du Tiroir.
* **Flèche Bas** :
  - Depuis le Dock d'accueil : Ouvre instantanément le **Tiroir complet d'applications** (All-Apps Drawer) avec une transition fluide de 220 ms.
  - Dans le Tiroir : Fait défiler les rangées suivantes d'applications.
* **Flèche Haut** :
  - Depuis le Dock d'accueil : Transfère le focus directement vers le bouton du **Centre de Contrôle & Réglages** en haut à droite de l'écran.
  - Dans le Tiroir d'applications : Lorsque vous êtes sur la première rangée (vignettes 0 à 4), presser Haut referme le tiroir et remet immédiatement le focus sur votre dernière application sélectionnée du Dock.
* **Bouton Sélection / Entrée (Centre du D-Pad)** :
  - **Pression brève** : Lance immédiatement l'application sélectionnée.
  - **Pression longue (> 500 ms)** : Ouvre le menu contextuel Leanback de l'application.

### 2.2 Le Dock d'accueil (5 applications favorites)

Au repos, l'écran d'accueil présente un Dock épuré inspiré de tvOS contenant vos **5 applications favorites** :
* Les vignettes adoptent des proportions cinématographiques 16:9 (`item_app_card`).
* **Effet de survol immersif** : Lorsqu'une application du Dock est sélectionnée, sa taille s'agrandit avec un effet de rebond subtil (échelle 1.08x, élévation 14dp, halo lumineux blanc) et l'arrière-plan de l'écran affiche en fondu la recommandation Top Shelf associée.
* **Prévention du focus trap** : Le focus circule de gauche à droite sans blocage.

### 2.3 Le Tiroir complet d'applications (All-Apps Drawer)

Lorsque vous souhaitez accéder à l'ensemble de votre catalogue :
1. Depuis le Dock, appuyez sur la flèche **Bas**.
2. Le Dock s'efface vers le bas et la grille complète de vos applications apparaît dans une disposition organisée en 5 colonnes (ajustable de 3 à 6 colonnes).
3. Le défilement est optimisé par recyclage de vues et conserve une fluidité parfaite à 60 Hz / 120 Hz.
4. Pour quitter le tiroir, appuyez sur **Retour** ou remontez sur la première rangée et faites **Haut**.

### 2.4 Accès aux widgets d'en-tête et Centre de Contrôle

Depuis le Dock, une pression sur la flèche **Haut** sélectionne directement la capsule d'accès rapide aux réglages (`btn_settings`) matérialisée par une roue crantée discrète :
* L'icône grossit (échelle 1.25x) et s'illumine.
* Une pression sur **OK** déploie immédiatement le **Centre de Contrôle tvOS**.
* Si vous souhaitez revenir au Dock sans ouvrir le panneau, appuyez simplement sur la flèche **Bas**.

### 2.5 Gestion de la touche Retour (Back)

La touche **Retour** (`KEYCODE_BACK`) de la télécommande est protégée par un gestionnaire d'état défensif :
* **Si le Centre de Contrôle est ouvert** : La touche Retour le referme immédiatement et restitue le focus sur la roue crantée.
* **Si le Tiroir d'applications est ouvert** : La touche Retour le referme en douceur et restitue le focus sur le Dock.
* **Si vous êtes déjà sur le Dock d'accueil** : La touche Retour est consommée (`return true`) sans quitter l'application. Le téléviseur ne bascule jamais sur un écran noir et ne quitte pas le launcher inopinément.

### 2.6 Actions au Clic Long (Menu contextuel)

En maintenant enfoncé le bouton central **OK** sur n'importe quelle vignette (Dock ou Tiroir), une boîte de dialogue contextuelle Leanback s'affiche :
1. **Ajouter aux favoris / Retirer des favoris** : Place l'application sur le Dock (ou l'en retire).
2. **Masquer l'application** : Cache l'application de la grille (idéal pour dissimuler les services système inutiles ou bloatwares préinstallés).
3. **Détails de l'application** : Ouvre directement les paramètres système d'Android TV pour gérer le stockage, forcer l'arrêt ou désinstaller l'application.

---

## 3. Centre de Contrôle tvOS & Réglages Système

Le **Centre de Contrôle** reproduit fidèlement le panneau latéral translucide introduit sur Apple TV, adapté aux spécificités techniques d'Android TV.

```
┌─────────────────────────────────────────────────────────────┐
│                      Écran d'Accueil                        │
│                                           ┌───────────────┐ │
│                                           │Centre Contrôle│ │
│                                           ├───────────────┤ │
│                                           │ [Veille] [WiFi│ │
│                                           │  Direct] 390M │ │
│                                           │ [Manette/Batt]│ │
│                                           │   85%  [!]    │ │
│                                           │  (O)  (♿) (X) │ │
│                                           └───────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 3.1 Présentation et ouverture du panneau latéral

* **Déploiement** : Le panneau s'anime latéralement depuis la droite de l'écran avec une translation sur l'axe X (420px -> 0px) et une atténuation d'arrière-plan en 240 ms.
* **Focus Trap Modal de Sécurité** : Dès l'ouverture, les `RecyclerView` de l'accueil sont verrouillées (`FOCUS_BLOCK_DESCENDANTS`). Le focus de la télécommande est strictement capturé dans le Centre de Contrôle pour éliminer tout risque d'interaction fantôme en arrière-plan.
* Le focus initial est automatiquement positionné sur le bouton de **Veille**.

### 3.2 Tuile Veille Instantanée (Daydream Native SLEEP Intent)

Le bouton carré **Veille** permet d'éteindre ou de mettre en veille prolongée le téléviseur instantanément.  
Afin de garantir un fonctionnement sans faille sur toutes les versions d'Android TV et Google TV, `DisplayResolutionManager` orchestre une chaîne de repli défensive à 6 niveaux :

```mermaid
flowchart TD
    A["Clic sur Tuile Veille"] --> B{"1. Pano SLEEP Google TV<br>com.google.android.pano.action.SLEEP"}
    B -- Succès --> End["Écran en veille"]
    B -- Échec --> C{"2. DreamX Backdrop Service<br>com.google.android.apps.tv.dreamx"}
    C -- Succès --> End
    C -- Échec --> D{"3. Somnambulator SystemUI<br>com.android.systemui"}
    D -- Succès --> End
    D -- Échec --> E{"4. Intent générique DREAM<br>android.intent.action.DREAM"}
    E -- Succès --> End
    E -- Échec --> F{"5. Broadcast Pano SLEEP"}
    F -- Succès --> End
    F -- Échec --> G{"6. Commande Shell Shell<br>input keyevent 223"}
    G --> End
```

Cette architecture élimine les blocages constatés sur d'autres launchers tiers qui échouent à déclencher la mise en sommeil sur les processeurs Amlogic.

### 3.3 Tuile Wi-Fi & Réseau (SSID & Débit réel en Mbps)

La tuile réseau affiche l'état physique exact de votre connexion sans fil :
* **Format dynamique** : `NomDuRéseau • Débit Mbps` (ex : `Maison • 390 Mbps`).
* **Détection Ethernet** : Affiche automatiquement `Ethernet` en cas de connexion par câble RJ45.
* **Clic direct** : Ouvre instantanément les paramètres sans fil natifs de Google TV (`Settings.ACTION_WIFI_SETTINGS`).

### 3.4 Tuile Télécommande / Manette & Alerte Batterie Critique

Zen TV Launcher interroge les périphériques d'entrée pour vous tenir informé de l'état de vos accessoires :
* Affiche le nom de l'accessoire connecté (`Télécommande` Google TV, Manette Bluetooth Xbox/PS5/Nvidia Shield).
* Affiche le pourcentage de batterie restant (`Batterie : 85%`).
* **Alerte Rouge Critique `[!]`** : Si le niveau de charge de la télécommande ou de la manette est **inférieur ou égal à 20%**, une pastille rouge circulaire contenant un point d'exclamation s'affiche immédiatement pour vous prévenir avant la panne sèche.
* Un clic sur la tuile ouvre les paramètres Bluetooth d'Android TV pour appairer ou gérer les accessoires.

### 3.5 Options de Thèmes (Sombre LED, OLED Pur, Bing 4K)

Depuis le menu des Réglages (`btn_cc_all_settings`), plusieurs ambiances visuelles sont configurables :
1. **Sombre Standard LED (`#0E1420`)** : Teinte bleu nuit profonde et apaisante avec un léger dégradé adapté aux dalles LCD/LED.
2. **Sombre Pur OLED (`#000000`)** : Désactive totalement l'émission lumineuse des pixels noirs sur dalles OLED, maximisant le contraste et réduisant la consommation électrique.
3. **Fond d'écran Bing 4K Quotidien** : Télécharge automatiquement chaque jour la photographie 4K officielle de Bing avec mise en cache locale.
4. **Fonds d'écran personnalisés** : Sélection possible d'images stockées localement sur la mémoire du Chromecast ou depuis un album Google Photos.

### 3.6 Protection d'écran OLED & Normes d'Accessibilité WCAG

Zen TV Launcher intègre les directives d'accessibilité grand écran :
* **Mode Contraste Élevé 7:1** : Active un arrière-plan noir profond absolu (`#000000`) et entoure chaque élément sélectionné d'une bordure jaune or éclatante (`#FFD600` de 3dp) garantissant un ratio de contraste supérieur à 7:1 (dépasse les recommandations WCAG 2.1 AAA).
* **Rétroaction sonore D-Pad** : Bip sonore système subtil confirmant chaque déplacement et validation pour les utilisateurs malvoyants.
* **Compatibilité TalkBack** : Toutes les icônes interactives possèdent des `contentDescription` explicites et localisées en français.
* **Protection d'écran OLED anti-marquage** :
  - **Dimming automatique** : Diminution progressive de la luminosité lors de périodes d'inactivité prolongées.
  - **Pixel Shift** : Micro-décalages imperceptibles des conteneurs statiques pour éviter la brûlure des sous-pixels des dalles LG et Sony OLED.

---

## 4. Widgets de l'Écran d'Accueil

### 4.1 Coexistence Google News Ticker & Carte Sport Carrée

Zen TV Launcher propose une disposition équilibrée qui ne surcharge jamais l'écran :

```
┌─────────────────────────────────────────────────────────────┐
│ 14:30  Lundi 26 Octobre                [RAM: 1.2G] [☀️ 21°C] │
│                                           ┌───────────────┐ │
│                                           │  🏒 LNH       │ │
│                                           │  MTL 3  2 TOR │ │
│                                           │    3e Pér.    │ │
│                                           └───────────────┘ │
│                                                             │
│       [ 📰 Google News : Découverte spatiale majeure... ]    │
│                                                             │
│      ┌────────────────────────────────────────────────┐     │
│      │ [App 1]   [App 2]   [App 3]   [App 4]   [App 5]│     │
│      └────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

* **Ticker Google News (Bandeau défilant)** :
  - Centré horizontalement au-dessus du Dock d'accueil (`widget_news_container`).
  - Défilement continu (Marquee infini) sur une seule ligne nette.
  - Non-focusable pour ne jamais capturer accidentellement le focus du D-Pad.
  - Personnalisable par thématiques multiples (Sciences, Technologie, Sports, Monde) et sources québécoises et internationales réputées (Radio-Canada, La Presse, Le Devoir, TVA, Le Monde, RDS).
* **Carte Sport Carrée (Square Sports Card)** :
  - Format compact carré de **170dp × 170dp** positionné en haut à droite sous la barre d'en-tête.
  - Affiche le badge de la ligue sportive (`🏒 LNH`, `🏀 NBA`, `🏈 NFL`, `⚽ Foot`, `⚾ MLB`).
  - Affiche les logos vectoriels nets des deux équipes qui s'affrontent, leurs noms abrégés, le score actuel en gros caractères gras (18sp) et l'état de la rencontre (ex: `3e Pér.`, `Mi-temps`, `Terminé`).
  - Définition d'équipe favorite pour prioriser les matchs de votre franchise préférée (ex: *Canadiens de Montréal*, *Lakers*, *Real Madrid*).

### 4.2 Retrait du widget Wi-Fi de l'accueil

Dans un souci de pureté visuelle et d'efficacité ergonomique :
* Le statut Wi-Fi a été **retiré du sélecteur de widgets de l'écran d'accueil**.
* La surveillance réseau est désormais **centralisée exclusivement dans le Centre de Contrôle**, évitant de polluer la barre d'accueil tout en offrant un diagnostic plus complet (affichage du débit réel en Mbps lors de l'ouverture du volet).

### 4.3 Widget RAM dynamique en temps réel

* Capsule d'en-tête affichant la mémoire vive disponible en temps réel (ex: `RAM: 1.2G libre`).
* Calcul basé sur `ActivityManager.MemoryInfo` natif avec rafraîchissement non-bloquant en tâche de fond.

### 4.4 Widget Météo Locale (Open-Meteo)

* Capsule d'en-tête affichant la température et les conditions actuelles (ex: `Montréal • 21°C ⛅`).
* Données issues de l'API publique Open-Meteo strictly over HTTPS (zéro clé d'API requise, respect de la vie privée).
* Géolocalisation automatique par adresse IP ou configuration manuelle de votre ville de résidence.

---

## 5. Procédure de Déploiement ADB & Instructions pour Google Jules

### 5.1 Préparation et connexion ADB

1. **Activer les options pour les développeurs sur Google TV** :
   - Rendez-vous dans `Paramètres` > `Système` > `À propos`.
   - Cliquez 7 fois consécutives sur `Build du système d'exploitation Android TV` jusqu'à voir le message *« Vous êtes désormais un développeur »*.
2. **Activer le débogage USB / Réseau** :
   - Allez dans `Paramètres` > `Système` > `Options pour les développeurs`.
   - Activez `Débogage USB` et `Débogage sans fil` (ou notez l'adresse IP du Chromecast).
3. **Se connecter via ADB depuis votre poste de travail** :
   ```bash
   # Remplacez par l'adresse IP et le port de votre Chromecast
   adb connect 192.168.2.100:5555
   
   # Vérifiez la connexion effective
   adb devices
   ```

### 5.2 Compilation et installation directe

```bash
# 1. Compilation de l'APK en mode Release optimisé
./gradlew assembleRelease

# 2. Installation directe sur le boîtier Chromecast
adb install -r app/build/outputs/apk/release/app-release.apk

# 3. Lancement immédiat de l'activité principale
adb shell am start -n com.minimal.tvlauncher/.MainActivity

# 4. (Optionnel) Définir comme Launcher d'accueil par défaut
adb shell cmd package set-home-activity com.minimal.tvlauncher/.MainActivity
```

### 5.3 Commandes de validation pour Google Jules

Conformément à `AGENTS.md`, chaque tâche automatisée par Google Jules ou tout agent d'intégration CI/CD doit obligatoirement valider les deux commandes suivantes dans l'environnement de build :

```bash
# 1. Exécution de la suite complète de tests unitaires et défensifs Robolectric
./gradlew testDebugUnitTest --continue

# 2. Compilation de vérification de l'APK de Release (ProGuard / R8 shrinker)
./gradlew assembleRelease
```

### 5.4 Protocoles de maintenance automatisée

Pour garantir la pérennité du launcher, 4 tâches automatisées sont programmées pour les agents de maintenance :

| Tâche | Fréquence | Périmètre d'audit |
|---|---|---|
| **🔄 1. Dépendances** | Mensuelle | Mise à niveau d'`androidx.core`, `appcompat`, `recyclerview` sans injection de dépendances transitives lourdes. |
| **🛡️ 2. Sécurité** | Trimestrielle | Vérification stricte des permissions (`AndroidManifest.xml`), TLS 1.3 dans `network_security_config.xml`, intents non exportés. |
| **📲 3. Compatibilité OS** | Annuelle | Adaptation du `targetSdk` aux directives Android TV 15+, respect des politiques d'arrière-plan du noyau Linux. |
| **📐 4. Accessibilité WCAG** | Continue | Contrôle de contraste (4.5:1 standard, 7:1 OLED), intégrité des chaînes de focus D-Pad (0 focus trap), TalkBack descriptions. |

---

## Conclusion & Synthèse

Zen TV Launcher concilie le raffinement visuel de **tvOS** avec la liberté et l'ouverture d'**Android TV**, tout en maintenant une rigueur technique sans compromis pour les appareils à 2 Go de RAM.

Pour toute question ou contribution, veuillez vous référer aux règles de développement multi-agents détaillées dans [AGENTS.md](file:///c:/Users/jdgen/OneDrive/Documents/android%20tv%20apple%20tv/AGENTS.md).
