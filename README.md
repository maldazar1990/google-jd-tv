# 📺 Zen TV Launcher — Apple TV Experience pour Google TV 4K

Un launcher Android TV ultra-léger, rapide et épuré inspiré de l'ergonomie d'**Apple TV (tvOS)**, conçu spécifiquement pour le **Chromecast avec Google TV 4K** (2 Go de RAM physique, SoC Amlogic).

---

## ✨ Points Forts & Caractéristiques

* **🚀 Empreinte Mémoire Ultra-Réduite** : Consommation inférieure à **30 Mo de RAM** (comparé aux ~400+ Mo du launcher officiel Google TV).
* **🍎 Interface Inspirée de tvOS** : 
  - Horloge et date épurées en en-tête.
  - Dock des applications favorites en rangée supérieure.
  - Grille d'applications avec formes de cartes personnalisables (*Squircles* Apple TV, cercles, rectangles doux).
  - Animation de focus télécommande fluide à 60 fps (zoom doux 1.08x, élévation 3D et halo lumineux).
* **🖼️ Fond d'Écran Dynamique Bing** :
  - Intégration de la photo du jour haute définition de Bing.
  - Filtre sombre adaptatif (*dark scrim*) pour garantir un contraste parfait des icônes.
* **📊 Widgets d'État Modulaires (100% Désactivables)** :
  - **Moniteur de RAM** en direct.
  - **Statut Réseau** (Wi-Fi 6, Ethernet).
  - **Météo locale**.
  - **Fil d'actualité Google News (RSS)** discret.
* **♿ Accessibilité Avancée** :
  - Mode **Contraste Élevé** (fond noir pur OLED `#000000` + bordures de focus jaune vif `#FFD600`).
  - Grille adaptative en **6, 4 ou 3 colonnes** pour une lisibilité optimale de loin.
  - Compatible lecteurs d'écran (TalkBack) et retour sonore télécommande.
* **🤖 Prêt pour Google Jules** : Fichier [`AGENTS.md`](./AGENTS.md) configuré pour les tâches de maintenance, d'audit de sécurité et de mise aux normes automatisées.

---

## 🛠️ Stack Technique

* **Langage** : Kotlin 100% natif.
* **Interface** : Android Views classiques (`RecyclerView`, `CardView`).
* **Zéro Bloatware** : Aucun framework lourd (pas de Jetpack Compose, Flutter ou React Native) pour garantir 60 fps constants sur GPU Mali-G31.
* **Cible** : Android 10+ (API 26 à 34+), testé sous Android 14 (`sabrina`).

---

## 🚀 Installation & Déploiement via ADB sans fil

1. Activez les **Options pour les développeurs** et le **Débogage sans fil** sur votre Google TV.
2. Associez et connectez votre appareil via ADB :
   ```bash
   adb pair 192.168.x.x:<port_association> <code_6_chiffres>
   adb connect 192.168.x.x:<port_connexion>
   ```
3. Compilez et installez directement l'application :
   ```bash
   ./gradlew installDebug
   ```

---

## 🤖 Maintenance Automatisée avec Google Jules

Ce dépôt est configuré pour être géré de manière autonome par **Google Jules** via [`AGENTS.md`](./AGENTS.md) :
1. **Mise à jour du Launcher & Dépendances**
2. **Audit de Sécurité & Intégrité**
3. **Suivi des Mises à jour Système Android TV**
4. **Audit de Mise aux Normes & Accessibilité (WCAG TV)**
