# TMSOS – iOS-Style Android Launcher (Samsung Galaxy Fold Edition)

Ein maßgeschneiderter, werbefreier und kostenloser **iOS-Style Homescreen-Launcher** für Android, speziell optimiert für das **Samsung Galaxy Fold** (Cover Screen & aufgeklapptes Innendisplay).

## Direkt-Download (APK)
- **Im Repository:** [`TMS-OS-Launcher.apk`](./TMS-OS-Launcher.apk)
- **Oder über GitHub Releases:** [Neuestes Release herunterladen](../../releases/latest)

---

## Hauptfunktionen

1. **Samsung Galaxy Fold Dual-Display Engine:**
   - **Cover Screen (Zugeklappt):** Klassisches iPhone-Raster (`4x6`, anpassbar) + Frosted-Glass-Dock.
   - **Main Screen (Aufgeklappt):** Wählbar zwischen **iPadOS Breitbild-Raster (`6x5` bis `8x5`)** inkl. *Zuletzt genutzte Apps*-Sektion im Dock oder **Dual-Page Buch-Modus** (zwei iPhone-Seiten nebeneinander).
2. **Echte iOS-18-System-Symbole:**
   - Native, von Hand nachgezeichnete Apple-Icons für Telefon, Nachrichten, Kamera, Uhr, Kalender (mit Live-Datum), Einstellungen, Dateien, Kontakte, Rechner, Fotos, Notizen, Mail, Musik und Wetter – inkl. echter **Superellipse (Squircle)** statt Android-`RoundedCornerShape`.
   - **Inter** als SF-Pro-Ersatz (Inter Display für Large Titles), damit Typografie und Gewichte Apple's entsprechen.
3. **Intelligente Auto-Kategorisierung & Smart App-Mediathek:**
   - Sortiert alle installierten Apps automatisch in sinnvolle Kategorien (*Soziale Netze, Finanzen & Banking, Produktivität & Büro, Unterhaltung & Medien, Shopping & Essen, Reisen & Navigation, Kreativität & Foto, Spiele, Dienstprogramme*).
   - **1-Klick Smart-Ordner Sortierung:** Räumt den gesamten Homescreen auf Wunsch mit einem Klick automatisch in iOS-Kategorie-Ordner auf.
   - **iOS App-Mediathek** auf der letzten Seite rechts (inkl. Direktstart der Top-3-Apps je Box, 2x2-Ordner-Erweiterung und A–Z Schnellliste).
4. **Blitzschnelle Spotlight-Suche (Swipe Down):**
   - Überall auf dem Homescreen nach unten wischen öffnet sofort Spotlight mit aktiver Tastatur.
   - **Fuzzy-Search & Kürzel-Erkennung:** Findet z. B. **W**hats**A**pp über `wa` oder **DB** Navigator über `db`, inklusive Tippfehler-Toleranz.
   - **Integrierter Sofort-Rechner:** Direkte Berechnung von Ausdrücken wie `45*12` in der Suchzeile.
   - **Optionaler Direkt-Start:** Startet die App automatisch, sobald nur noch 1 eindeutiger Treffer übrig ist.
5. **iOS-native Einstellungen (v4.1):**
   - Komplett neu gebaut im iOS-Settings-Stil: **gruppierte Inset-Listen**, **Large Title mit Collapse**, 17-pt-Zeilen, echte **iOS-Switches**, echte **iOS-Slider**, echtes **Segmented Control** und System-Icon-Kacheln.
   - Vollständig lokalisiert in **Deutsch, Englisch und Türkisch**.
6. **iOS-Feeling im Detail:**
   - Eigenes, gezeichnetes **Statusleiste**-Overlay (Uhr, Empfang, WLAN, Akku) – immer scharf, auch über Spotlight, Ordnern und Menüs.
   - **Hardware-Blur** hinter allen Overlays, echte **Zoom-from-Icon**-App-Animation, **Jiggle-Modus** mit „Bearbeiten"/„Fertig"-Leiste wie auf iOS.

---

## Build aus dem Quellcode
```bash
gradle assembleRelease
```
Die signierte und R8-optimierte APK wird unter `app/build/outputs/apk/release/app-release.apk` sowie im Projekt-Root als `TMS-OS-Launcher.apk` abgelegt.
