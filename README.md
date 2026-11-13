# TMSOS – iOS-Style Android Launcher (Samsung Galaxy Fold Edition)

Ein maßgeschneiderter, werbefreier und kostenloser **iOS-Style Homescreen-Launcher** für Android, speziell optimiert für das **Samsung Galaxy Fold** (Cover Screen & aufgeklapptes Innendisplay).

## Direkt-Download (APK)
- **Im Repository:** [`TMS-OS-Launcher.apk`](./TMS-OS-Launcher.apk)
- **Oder über GitHub Releases:** [Neuestes Release herunterladen](../../releases/latest)

---

## Neu in v5.8.5
- Solvado-Widgets ohne Beschriftung „Solvado Shop“ / „Solvado Admin“ darunter.

## Neu in v5.8.4 – Widget-Einstellungen nur beim Bearbeiten
- Ein Tipp auf ein Widget öffnet die Einstellungen nur noch im Bearbeiten-Modus; sonst macht der Tipp, was das Widget tut (Wetter → Wetter-App, Solvado → nichts).
- Solvado-Widget: schlichterer, dünner Aktualisieren-Pfeil.

## Neu in v5.8.3 – eine Zeile mehr
- Die Zeilen werden etwas niedriger, wenn dadurch eine weitere über das Dock passt: Apps lassen sich jetzt bis direkt über die Suchleiste setzen. Bisherige Anordnungen bleiben erhalten.

## Neu in v5.8.2 – Platzieren ohne Zurückspringen
- Ein Icon landet genau in der Zelle, in der du es loslässt – auch wenn du schnell loslässt.
- Die Vorschau beim Ziehen geht immer von der Anordnung vor dem Ziehen aus: weiterziehen macht das Ausweichen anderer Icons rückgängig, statt es aufzustapeln.
- Das Raster reicht jetzt bis zum Dock; der freie Streifen unter der letzten Reihe ist nutzbar. Bisherige Anordnungen bleiben erhalten.

## Neu in v5.8.1 – frei positionierbar, Einstellungen wiederhergestellt
- **Apps und Widgets frei platzieren** wie bei iOS 18: dort ablegen, wo du willst – oben, unten, in der Mitte, Lücken bleiben. Liegt dort schon ein Icon, rückt es eine Zelle weiter. Gilt für den normalen und den minimalistischen Home-Bildschirm; Deckel- und Innendisplay merken sich ihre Anordnung getrennt.
- **Wiederhergestellt**: Die in v5.8.0 verlorene App-Auswahl des iOS-Minimal-Bildschirms wird einmalig zurückgeholt.
- **Solvado-Widgets**: Orange statt Lila als Akzent; Shop-Logos werden neu geladen, wenn sich das Logo ändert.

## Neu in v5.8.0 – Solvado-Widgets im Solvado-Stil, „Widget bearbeiten“ für mehr Widgets, freie iOS-Minimal-Layouts
- **Solvado-Widgets neu gestaltet**: Farben des Solvado-Dashboards, das echte Solvado-Zeichen, das Logo des jeweiligen Shops. Klein zeigt bis zu 3 Werte, Mittel 6, Groß 10 – ausgewählte Werte erscheinen jetzt auch wirklich.
- **Aktualisieren-Pfeil im Widget**; automatische Aktualisierung nur beim Blick auf den Home-Bildschirm (höchstens alle 15 Minuten) und abschaltbar. Im Hintergrund wird nie abgefragt.
- **„Widget bearbeiten“ überarbeitet**: Shop-Auswahl als eigene Seite mit Suche und Logos, Größe direkt umstellbar, Werte mit Reihenfolge.
- **Admin-Widget** wird nicht mehr automatisch gesetzt.
- **Wetter-Widget bearbeiten**: Standort auf Knopfdruck einmalig ermitteln oder Ort eintragen, °C/°F. **Kalender-Widget**: Tag oder Monat.
- **Minimalistisch, iOS-Vorlagen**: jetzt ein eigener, frei bearbeitbarer Home-Bildschirm – Widgets aus der Galerie, beliebig viele Apps, Ordner, Verschieben. Die drei schlichten Vorlagen bleiben fest.
- Neues Widget **Leerraum** (unsichtbar), damit Apps weiter unten sitzen können.

## Neu in v5.7.0 – TMS-Solvado-Widgets
- **Solvado Shop**: Umsatz, Gewinn, Conversion, Werbe- und Produktkosten, Bestellungen, Besucher, Ø Bestellwert, ROAS und Marge eines Shops – klein, mittel oder groß; mehrere Widgets für mehrere Shops.
- **Solvado Admin** (nur für Admin-Konten): Abonnenten, Credit-Umsatz in Euro, neue Accounts, Accounts gesamt, MRR, Credit-Käufe, Testphasen. Erscheint nach der Anmeldung als Admin automatisch einmal in der Heute-Ansicht.
- **Widget bearbeiten**: Widget antippen → Shop, Zeitraum (Heute / 7 Tage / 30 Tage) und die angezeigten Werte samt Reihenfolge wählen.
- Anmeldung mit dem Solvado-Konto direkt im Widget. Das Widget liest nur; nicht gemessene Werte erscheinen als „–“, nie als 0.
- Braucht die Solvado-Schnittstelle `/api/widgets` auf dem Server.

## Neu in v5.6.1 – Minimal-Modus bearbeiten
- **Reihenfolge per Ziehen ändern** im minimalistischen Home-Bildschirm: App gedrückt halten und ziehen – auf der Seite, im Dock und zwischen beiden; die anderen rücken animiert zur Seite. Funktioniert in allen sechs Vorlagen.
- **Bearbeiten-Modus wie gewohnt**: Hintergrund gedrückt halten lässt die Apps wackeln, „−“ entfernt eine App aus der Auswahl, „Fertig“ beendet. Vorlage und App-Auswahl stehen unter „Bearbeiten“ → „Minimalistisch“.

## Neu in v5.6.0 – natürlicher Dunkel-Stil, schneller, sparsamer
- **Icon-Stil „Dunkel“ neu**: helle Platten werden dunkel und Logos bleiben, wie sie sind; ein schwarzes Symbol wird hell (Revolut, ChatGPT); einfarbige Icons tauschen die Rollen (WhatsApp: dunkler Grund, grünes Symbol); bunte Icons mit Logo bekommen einen tieferen Farbton statt hell zu bleiben; bereits dunkle Icons bleiben unverändert. Kanten ohne hellen Saum.
- **Sofort da nach einem Neustart des Launchers**: fertige Icons liegen jetzt auf dem Speicher statt jedes Mal neu berechnet zu werden; Apps werden parallel geladen; die Icons fliegen mit derselben Animation ein wie bei der Rückkehr aus einer App.
- **Weniger Energie**: Uhr-Icon, Uhr-Widgets und Minimal-Uhr ticken nur noch, solange der Home-Bildschirm sichtbar ist (vorher rechnete das Uhr-Icon auch im Hintergrund jede Sekunde); das Hintergrundbild wird bei der Rückkehr nicht mehr neu geladen; Systemabfragen laufen nicht mehr auf dem Haupt-Thread.

## Neu in v5.5.1 – Minimal-Vorlagen im iOS-Stil
- Drei weitere Vorlagen für den minimalistischen Home-Bildschirm, gebaut aus den normalen iPhone-Bausteinen (Widget-Kacheln, beschriftete Icons im Raster, Glas-Dock): **Widgets** (Kalender + Wetter, zwei App-Reihen), **Groß** (großes Wetter-Widget, eine App-Reihe), **Unten** (freies Hintergrundbild, Apps direkt über dem Dock). Die ersten vier gewählten Apps stehen jeweils im Dock.
- Die drei schlichten Vorlagen (Liste, Symbole, Fokus) bleiben.

## Neu in v5.5.0 – Minimalistischer Modus, mehr Widgets, besserer Rechner
- **Minimalistischer Home-Bildschirm** (Bearbeiten → „Minimalistisch“): wichtigste Apps aus einer durchsuchbaren Liste wählen, drei Vorlagen – **Liste** (Uhr + App-Namen), **Symbole** (Uhr + Icons ohne Beschriftung), **Fokus** (nur Uhr, Apps im Dock). Der normale Home-Bildschirm bleibt gespeichert; zum Ändern den Hintergrund gedrückt halten.
- **Fünf neue Widgets**: Minimal (nur Schrift, ohne Hintergrund), Digitaluhr, Datum, Jahr (Fortschritt), Bildschirmzeit (braucht den Nutzungszugriff).
- **Rechner in Spotlight** versteht jetzt `10x10`, `39-18`, `3,5+2`, `200+19%`, `19% von 250`, `2^10`, `√81`, Klammern und Wörter („5 mal 3“); Tippen auf das Ergebnis kopiert es.
- **TMS OS** erscheint nicht mehr in Spotlight, nur noch in der App-Mediathek.

## Neu in v5.4.0 – Ordner wie bei iOS
- **Apps in Ordnern verschieben**: im Ordner per Ziehen umsortieren; aus dem Ordner herausziehen – er klappt zu und die App hängt auf dem Home-Bildschirm am Finger (Seiten wechseln, ablegen, in andere Ordner ziehen).
- **Ordner springen auf**: eine App länger über einen Ordner halten öffnet ihn, die Position im Ordner lässt sich direkt bestimmen.
- **App-Mediathek**: nach unten ziehen öffnet die Suche.
- **Spotlight**: nach oben wischen schließt.
- **Dialoge** („… entfernen?“): deckender Hintergrund, Beschriftungen vollständig.
- **Icons** für Rechner, Einstellungen und Kamera neu gezeichnet.
- **Entfernt**: die eigene App-Übersicht (Wischen vom Dock nach oben) – dafür ist die System-Übersicht da.

## Neu in v5.3.0 – fließende Übergänge
- **App-Mediathek**: Kategorien wachsen aus ihrer Kachel heraus und schrumpfen wieder hinein; die Suche gleitet (Feld rückt nach links, „Abbrechen“ schiebt sich herein, Kacheln und Liste blenden ineinander).
- **Spotlight**: Vorschläge und Treffer blenden weich über, Karten ändern ihre Größe animiert.
- **Ordner und Long-Press-Menü**: der Hintergrund wird weich unscharf (in 1/8-Auflösung berechnet statt als Vollbild-Unschärfe pro Bild).
- **Apps öffnen**: Start direkt aus dem Icon – auch aus Ordnern und Kategorien, ohne dass diese erst zuklappen; der Homescreen weicht sanfter zurück.
- **Flüssiger**: Icons tragen ihre Form im Bild (kein Zuschneiden pro Frame mehr), beim Seitenwischen wird nicht mehr der ganze Bildschirm neu aufgebaut.

## Neu in v5.2.0 – Icons füllen die Fläche
- **App-Symbole ausgefüllt**: Android-Icons werden jetzt korrekt auf den sichtbaren Ausschnitt zugeschnitten, kleine Logos zusätzlich zentriert vergrößert – wie bei iOS füllt das Motiv das Icon.
- **Icon-Stil „Dunkel"** neu: nur helle Hintergrundplatten werden dunkel, Motive behalten ihre Farbe.
- **Icon-Stil „Getönt"** neu: dunkler Grund, Motiv in der Akzentfarbe.
- **Echte Statusleiste** bleibt sichtbar (Netz, VPN, Benachrichtigungen); nur im Bearbeiten-Modus weicht sie den Knöpfen.

## Neu in v5.1.0 – ruhiges, neutrales Design
- **Neutrales dunkles Material** statt buntem Glas: Dock, Such-Pille, Mediathek-Kacheln, Menüs und Karten in einem einheitlichen Grau mit Haarlinie.
- **Kompaktes Raster**: quadratische Zellen, oben ausgerichtet; Dock über dem Navigationsbalken, Dock-Icons exakt in den Rasterspalten.
- **App-Mediathek** neu: zentrierte Such-Pille, Kategorie-Kacheln in Homescreen-Icon-Größe, Kategorien öffnen im Vollbild (Titel + Raster), alphabetische Suche mit A–Z-Leiste.
- **Spotlight** neu: Vorschläge-Karte, Verlauf-Chips, große Trefferzeilen, Kontakte mit Nachricht/Anruf, „In Apps suchen“ (Web, Store, Karten), Suchfeld unten.
- **Heute-Ansicht**: Kalender (mittel) + Wetter (groß), ohne Suchfeld.

## Neu in v5.0.0 – iOS 26 Edition
- **Echte Widgets** wie bei iOS (klein 2×2, mittel 4×2, groß 4×4): Wetter mit Stunden- und Tagesvorhersage, Kalender, Uhr/Weltzeit, Batterien, Siri-Vorschläge, Notizen – Icons fließen um Widgets herum.
- **Smart Stack:** mehrere Widgets übereinander, vertikal durchwischen; entsteht aus der Galerie oder durch Ziehen eines Widgets auf ein gleich großes.
- **Heute-Ansicht** links von Seite 1 und **Widget-Galerie** mit Suche und Größenauswahl („Bearbeiten“ → „Widget hinzufügen“).
- **Bearbeiten wie iOS:** lang drücken und ziehen, andere Icons weichen animiert aus, am Rand halten wechselt die Seite, Ordner per Draufhalten, Apps zwischen Dock und Raster ziehen, Apps aus Ordnern ziehen.
- **Spotlight folgt dem Finger** beim Herunterziehen.
- **Ordner** wachsen aus dem Icon zum Glas-Panel; **Long-Press-Menü** am angehobenen Icon mit echten App-Kurzbefehlen.
- **App-Umschalter:** auf dem Homescreen vom Dock hochwischen – Karten seitlich durchblättern, hochschnippen zum Schließen.
- **iOS-Bewegung:** App-Start zoomt aus dem Icon, beim Zurückkehren federn die Icons an ihren Platz; Liquid-Glass-Dock.

> Update: einfach die neue APK über die alte installieren – das Homescreen-Layout bleibt erhalten.

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
./gradlew assembleRelease
# APK: app/build.nosync/outputs/apk/release/app-release.apk
```
Die signierte und R8-optimierte APK wird unter `app/build/outputs/apk/release/app-release.apk` sowie im Projekt-Root als `TMS-OS-Launcher.apk` abgelegt.
