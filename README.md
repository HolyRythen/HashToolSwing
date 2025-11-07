# 🔐 HashToolSwing

Ein leichtgewichtiges **Hash‑Tool in purem Java (Swing)** — ideal zum Prüfen, Vergleichen und Exportieren von Datei‑Hashes (MD5, SHA‑1, SHA‑256, SHA‑512, CRC32).  
Perfekt für Entwickler, Admins oder für GitHub‑Projekte als nützliches Utility.

---

## 🚀 Features

- ✅ Unterstützt **MD5**, **SHA‑1**, **SHA‑256**, **SHA‑512**, **CRC32**
- 🖱️ **Drag & Drop** oder **Dateiauswahl per Dialog**
- 📋 **Kopier‑Buttons** neben jedem Hashfeld
- 🔍 **Hash‑Vergleich** (füge erwarteten Hash ein → sofortiger Abgleich)
- 📦 **Batch‑Modus** mit Mehrfach‑Dateien und CSV‑Export
- ⚡ Fortschrittsbalken & Multi‑Threaded Verarbeitung
- 🔒 Reines Java, kein Framework, keine externen Abhängigkeiten

---

## 🧩 Voraussetzungen

- **Java 17 oder neuer**
- Kein JavaFX, kein Maven oder Gradle erforderlich — reine `.java`‑Datei

---

## ⚙️ Installation & Start

1. Lege die Datei **`HashToolSwing.java`** in einen Ordner, z. B.:  
   `C:\Users\RobertMartin\Desktop\java-programms\hash-tool`

2. Öffne **PowerShell oder CMD** in diesem Ordner

3. Kompiliere das Programm:

   ```powershell
   javac HashToolSwing.java
   ```

4. Starte das Programm:

   ```powershell
   java HashToolSwing
   ```

---

## 🖥️ Nutzung

### 🔹 Einzeldatei‑Modus

1. Wähle eine Datei per **Button**, **Drag & Drop** oder gib den Pfad manuell ein  
2. Klicke **„Hash berechnen“**  
3. Alle Hashwerte werden angezeigt  
4. Mit **„Copy“** kannst du jeden Hash in die Zwischenablage kopieren  
5. Zum Vergleich einen erwarteten Hash einfügen → **„Prüfen“**

### 🔹 Batch‑Modus

1. Mit **„Batch: Dateien hinzufügen“** mehrere Dateien auswählen  
2. Auf **„Batch: Berechnen“** klicken — MD5 & SHA‑256 werden automatisch erzeugt  
3. Optional: **„Export CSV“** speichert alle Ergebnisse in einer CSV‑Datei

---

## 📊 Beispiel‑Ausgabe

| Datei | MD5 | SHA‑256 |
|:------|:----|:--------|
| `setup.exe` | `c3fcd3d76192e4007dfb496cca67e13b` | `9b74c9897bac770ffc029102a200c5de` |
| `data.zip` | `a87ff679a2f3e71d9181a67b7542122c` | `4e07408562bedb8b60ce05c1decfe3ad` |

---

## 🧠 Tipps

- Funktioniert auch mit **großen Dateien** (>1 GB)  
- „CRC32“ ist nützlich für ZIP‑/Archiv‑Integritätsprüfungen  
- Hash‑Vergleich ignoriert Groß-/Kleinschreibung  
- **Dateien mit Anführungszeichen oder Leerzeichen** werden automatisch erkannt

---

## 💡 Erweiterungsideen

- Speicherung der letzten Pfade / History  
- Option für **Hash‑Format (Großbuchstaben, mit Leerzeichen)**  
- Unterstützung für **Ordner‑Hashes (rekursiv)**  
- Integriertes CLI‑Interface (`--md5 file.txt`)

---

## 📁 Lizenz

MIT License — frei nutzbar & veränderbar.

---

© 2025 Robert Martin
