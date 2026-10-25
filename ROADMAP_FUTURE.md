# 🚀 Feuille de Route des Versions Ultérieures (Roadmap v2.0+)

Ce document recense les évolutions stratégiques et idées innovantes planifiées pour les versions futures de **Zen TV Launcher**, une fois la version 1.0.0 pour Google TV 4K finalisée.

---

## 1. 📺 Déclinaison Amazon Fire TV (Fire OS)

* **Contexte & Opportunité** :
  - Les appareils Amazon Fire TV (Fire TV Stick 4K, 4K Max, Cube) tournent sous Fire OS (dérivé d'Android 9/11) et souffrent exactement du même problème d'encombrement publicitaire et de manque de RAM (1.5 à 2 Go).
  - Notre launcher (< 30 Mo de RAM, zéro Google Play Services obligatoire) est l'alternative idéale pour redonner une seconde jeunesse aux clés Fire TV.
* **Adaptations techniques prévues** :
  - Détection des applications installées depuis l'**Amazon Appstore** en plus du sideloading.
  - Gestion du bouton d'accueil Fire TV (contournement des restrictions Amazon via Launcher Manager ou script ADB).
  - Profil énergétique adapté aux clés HDMI compactes soumises à la surchauffe.

---

## 2. 💡 Propositions de Fonctionnalités Innovantes (Idées à Forte Valeur Ajoutée)

### A. Mode "Veilleuse & Horloge Ambiante" (Ambient Screen Saver)
* Après un délai d'inactivité configurable, l'interface bascule sur une **horloge géante minimaliste**, la météo et la photo Bing en fondu noir OLED.
* Luminosité ultra-faible pour préserver les dalles OLED des brûlures d'écran (*anti-burn-in pixel shifting*) et ne pas éblouir le salon la nuit.

### B. Dossiers & Catégories d'Applications (tvOS Folders)
* Création de dossiers personnalisés : *Streaming* (Netflix, Prime, Disney+), *Musique* (Spotify, Deezer), *Médias Locaux* (Plex, VLC, Kodi), *Jeux / Émulateurs*.
* Dépliage d'un dossier sous forme d'une rangée fluide avec la télécommande.

### C. Verrouillage Parental par Combinaison Télécommande (D-Pad PIN)
* Protection des applications sensibles ou du menu réglages par une combinaison simple sur la télécommande (ex: `Haut - Haut - Bas - Bas - OK`).
* Aucune invite de saisie complexe au clavier virtuel, déverrouillage en 1 seconde.

### D. Widget Domotique Léger (Home Assistant / MQTT)
* Intégration d'un badge d'état discret dans l'en-tête :
  - Température de la maison / capteurs extérieurs.
  - Statut des lumières ou notification discrète lorsqu'une sonnette vidéo est déclenchée.
  - Consommation réseau et mémoire strictement isolée (< 2 Mo supplémentaires).

### E. Sauvegarde & Clonage Multi-Appareils (Backup / Sync)
* Exportation de la configuration (favoris, ordre de la grille, widgets actifs) dans un petit fichier JSON chiffré.
* Permet de cloner la même disposition sur un second Chromecast, une Fire TV ou une Shield TV en 5 secondes.

### F. Support Manettes de Jeux (Bluetooth Gamepads)
* Navigation complète dans le launcher avec les manettes Bluetooth (Xbox, PlayStation, 8BitDo) pour les utilisateurs qui jouent en cloud gaming (GeForce NOW, Xbox Cloud Gaming, RetroArch).
