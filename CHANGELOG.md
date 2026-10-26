# Changelog — Zen TV Launcher

Toutes les modifications notables apportées à ce projet sont documentées dans ce fichier.
Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/), et ce projet adhère à [Semantic Versioning](https://semver.org/lang/fr/).

---

## [1.1.0] — 2026-10-26

### Ajouté
- **Bouton Veille Native Google TV** : Implémentation du démarrage direct de l'Activity vocale/veille Daydream (com.google.android.pano.action.SLEEP) avec fallbacks DreamX Backdrop, Somnambulator, ACTION_DREAM et broadcast.
- **Widget Sport Carré & Logos Vectoriels** : Nouveau composant iew_sports_card.xml (170x170dp, coins 20dp givrés) avec 7 écussons vectoriels haute fidélité légers (< 2.5 Ko) : Canadiens de Montréal, Toronto Maple Leafs, LA Lakers, Boston Celtics, Real Madrid, FC Barcelone et logo générique.
- **Coexistence Parfaite News & Sports** : Le ticker marquee Google News RSS et la carte carrée Sports s'affichent et sont actualisés en simultané sans aucun écrasement mutuel.
- **Nettoyage Configuration Widgets** : Retrait de l'option obsolète Wi-Fi du sélecteur d'accueil (centralisée dans le Centre de Contrôle).
- **Centre de Contrôle tvOS 2x2** : Tuile Veille native, Tuile Wi-Fi avec SSID + vitesse en Mbps réels, Tuile Batterie télécommande avec badge d'alerte rouge [!] si <= 20%.
- **Protection d'écran OLED** : Pixel Shift (1-2px / 5 min), HUD Dimming (-30% après 2 min) et Veille rapide (après 3 min).
- **Thèmes Sombre & OLED Pur** : Modes Sombre Standard LED (#0E1420), Pur OLED (#000000) et Bing 4K dynamique.
- **Documentation Complète** : Création du guide technique et utilisateur exhaustif DOCUMENTATION.md à la racine.
- **Batterie de Tests Défensifs** : 178 tests unitaires automatisés validant 100% des cas limites, l'intégrité D-Pad et la mémoire.

## [0.1.0] — 2026-10-25 (Version Initiale en cours)

### Ajouté
- Socle natif Android TV ultra-léger (< 30 Mo RAM cible).
- Modèle de données immuable `AppModel` avec persistance `PreferencesManager`.
- 40 tests unitaires de validation défensive et gestion de mémoire.
- Support du mode Apple TV sombre et du mode Contraste Élevé OLED.
- Fichier de directives Google Jules [`AGENTS.md`](./AGENTS.md).
- Skill de validation CI/CD Frontend & Backend [`.skills/tv-testing/SKILL.md`](./.skills/tv-testing/SKILL.md).
