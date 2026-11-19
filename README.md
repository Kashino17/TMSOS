# TMSOS – iOS-Style Android Launcher (Samsung Galaxy Fold Edition)

Ein maßgeschneiderter, werbefreier und kostenloser **iOS-Style Homescreen-Launcher** für Android, speziell optimiert für das **Samsung Galaxy Fold** (Cover Screen & aufgeklapptes Innendisplay).

## Direkt-Download (APK)
- **Im Repository:** [`TMS-OS-Launcher.apk`](./TMS-OS-Launcher.apk)
- **Oder über GitHub Releases:** [Neuestes Release herunterladen](../../releases/latest)

---

## Neu in v5.12.10 – Ordner schließen wie auf dem iPhone
- Beim Schließen einer Kategorie stehen die drei großen Icons des Ordners sofort wieder da.
- Die vier kleinen Icons fliegen aus der schrumpfenden Seite in ihre Mini-Plätze im Ordner und werden dabei kleiner – ohne Ein- oder Ausblenden, ohne Sprung am Ende.
- Das Öffnen bleibt unverändert.

## Neu in v5.12.9 – Keine doppelten App-Icons beim Öffnen eines Ordners
- Tippt man in der App-Mediathek auf einen Ordner, verschwinden dessen Icons sofort – ohne Vergrößern oder Ausblenden. Zu sehen ist nur noch die Kategorie-Seite, die sich aus dem Ordner auffächert; keine App erscheint doppelt.
- Beim Schließen tauchen die Icons erst wieder auf, wenn die Seite ganz im Ordner verschwunden ist.

## Neu in v5.12.8 – Kurzbefehl-Symbole in der Suche
- Behoben: Kurzbefehle mancher Apps (z. B. ChatGPT: Bilder, Kamera, Audio) zeigten in der Suche und im Haptic-Touch-Menü nur ein weißes Quadrat. Diese Apps legen die Platte mit in das Symbolbild; TMS OS erkennt das jetzt und zeigt nur das eigentliche Symbol.
- Auch kein blasses Quadrat mehr hinter Symbolen wie bei Chrome („Neuer Tab“, „Inkognito“). Lässt sich kein Symbol finden, erscheint das App-Icon statt eines weißen Feldes.

## Neu in v5.12.7 – Große Icons verschwinden schneller
- Beim Öffnen eines Ordners in der App-Mediathek verschwinden dessen große Icons jetzt sofort (nach einem Viertel der Animation) statt langsam mit der Mediathek auszublenden. Beim Schließen kommen sie erst ganz am Ende zurück.

## Neu in v5.12.6 – Ordner der App-Mediathek Bild für Bild wie auf dem iPhone
- Die Kategorie-Seite wächst jetzt genau aus dem kleinen Viererfeld des Ordners: Ihr Icon-Raster startet so groß wie das Viererfeld und an derselben Stelle.
- Die vier kleinen Icons zoomen als Gruppe mit der Seite mit (oben links → erste Spalte, oben rechts → letzte Spalte) und verblassen, während die Seite deckend wird; die großen Icons bleiben im Ordner.
- Die Mediathek weicht dabei zurück: Sie wird um den Ordner herum etwas kleiner, unscharf und blendet aus.
- Der Glas-Hintergrund der Suchleiste verschwindet sofort beim Antippen, der Text „App-Mediathek“ etwas später – wie auf dem iPhone.
- Schließen läuft genau rückwärts, zurück ins Viererfeld.

## Neu in v5.12.5 – Ordner-Animation ruhiger
- Öffnen und Schließen der Ordner in der App-Mediathek laufen langsamer und ruhiger (Öffnen etwa 0,5 s, Schließen etwa 0,4 s).

## Neu in v5.12.4 – Ordner-Animation der App-Mediathek nachjustiert
- Wie auf dem iPhone erscheint beim Öffnen die ganze Kategorie-Seite (Titel, Icons, Namen) als kleine, durchscheinende Miniatur im Ordner und wächst auf Vollbild; beim Schließen schrumpft sie zurück in den Ordner.
- Nur die vier kleinen Icons des Ordners fliegen einzeln und voll sichtbar an ihren Platz; die großen Icons bleiben im Ordner.
- Die Mediathek dahinter zoomt nicht mehr, sondern blendet an Ort und Stelle aus und wird nur leicht unscharf.
- Schließen ist jetzt so schnell wie auf dem iPhone, Öffnen etwas knackiger.

## Neu in v5.12.3 – Ordner der App-Mediathek öffnen wie auf dem iPhone
- Beim Öffnen eines Ordners in der App-Mediathek fliegt jetzt jede App einzeln aus dem Ordner an ihren Platz: die großen und kleinen Icons genau von dort, wo sie im Ordner liegen, weitere Apps wachsen aus dem kleinen Viererfeld. Die Mediathek dahinter zoomt und verschwimmt, der Titel und die App-Namen blenden erst zum Schluss ein.
- Beim Schließen läuft alles rückwärts: Namen und Titel verschwinden zuerst, die Icons schrumpfen zurück in den Ordner, die Mediathek kommt scharf zurück.
- Kein Einblenden der ganzen Seite mehr.

## Neu in v5.12.2 – Suche der App-Mediathek bleibt offen
- Behoben: Die Suche der App-Mediathek schloss sich immer wieder von selbst, sobald die Tastatur erschien. Die Tastatur machte das Fenster kurz kleiner, dadurch verschob sich die Seitennummer der Mediathek, und die Suche hielt das für „weggewischt“.
- Die Startseiten werden nicht mehr neu aufgeteilt, wenn die Tastatur das Fenster verkleinert.
- Die Suche schließt nur noch bei einem echten Wisch zur Seite (oder wenn eine andere Seite zur Ruhe kommt), nicht bei kleinen Seitwärtsbewegungen beim Scrollen der Liste.

## Neu in v5.12.1 – Richtige Namen und Icons bei Galerien
- iOS-Icons und -Namen bekommen nur noch die vorinstallierten Apps des Handys. Apps aus dem Store behalten ihr Icon und ihren Namen: „Edge Gallery“ (KI) ist nicht mehr „Fotos“, nur weil im Paketnamen „gallery“ steht.
- Haben zwei Apps denselben Namen, steht der Hersteller davor: „Samsung Galerie“ und „Google Fotos“ statt zweimal „Fotos“ (gilt für alle doppelten Namen). Selbst vergebene Namen bleiben unverändert.
- Die Icons werden dafür einmal neu gezeichnet.

## Neu in v5.12.0 – Text markieren wie auf dem iPhone
- **Notizen**: Markierter Text ist gelb hinterlegt mit iOS-Griffen (Strich mit Punkt oben am Anfang, unten am Ende). Darüber erscheint die Glas-Kapsel „Ausschneiden | Kopieren | Einsetzen | ›“; der Pfeil öffnet die Karte mit den drei Symbol-Knöpfen und Löschen, Format › (Fett, Kursiv, Unterstrichen, Durchgestrichen), Datei anhängen, Auswahl suchen, Teilen, Aktionen anderer Apps (z. B. Übersetzen) und Alles auswählen.
- Ein Tipp auf den Cursor zeigt „Auswählen | Alles auswählen | Einsetzen“, ein zweiter Tipp blendet es aus; Tippen schließt das Menü.
- Bei markiertem Text wird die Leiste über der Tastatur zur Format-Leiste: B, I, U, S, Marker, Aa.
- **Überall in TMS OS** (Spotlight, App-Mediathek, Einstellungen, Suchfelder, Dialoge): dasselbe iOS-Menü statt des Android-Menüs, mit blauer iOS-Markierung (in den Notizen gelb).
- In anderen Apps zeichnen diese Apps bzw. das System ihr Menü selbst; dort kann TMS OS es nicht ersetzen.

## Neu in v5.11.6 – Samsung-Import ohne leere Bilder
- Samsung Notes legt in jede Word-Datei leere Bilder (seine leere Zeichenebene), oben und unten. Der Import erkennt leere Bilder (durchsichtig oder rein weiß) und lässt sie weg; echte Bilder und Zeichnungen bleiben.
- Kein doppelter Titel mehr: Samsungs Datumsanhang im Dateinamen („_210625_185056“) wird entfernt, der Dateiname steht nur noch vor Notizen, die mit einem Bild beginnen.
- Schon importierte Notizen werden beim ersten Start einmalig bereinigt: leere Bilder und der zusätzliche Dateiname-Titel verschwinden, der Text dazwischen wird wieder zusammengefügt. Datum der Notizen bleibt.
- Zahlen, die an Wörter oder Unterstriche grenzen, werden nicht mehr als Telefonnummer verlinkt.
- Durchsichtige Bildteile werden beim Import weiß statt schwarz.

## Neu in v5.11.5 – Notizen: Links, Import aus Samsung Notes
- **Links werden automatisch erkannt**: Webadressen, E-Mail-Adressen und Telefonnummern erscheinen gelb unterstrichen wie auf dem iPhone. Beim Lesen öffnet ein Tipp den Browser (bzw. Mail oder Telefon); beim Schreiben setzt ein Tipp nur den Cursor.
- **Import aus Samsung Notes**: Samsung Notes gibt seine Notizen nicht direkt an andere Apps heraus, aber als Dateien. In Samsung Notes Notizen auswählen → „Teilen“ → „Microsoft Word-Datei“ (mit Bildern und Formatierung) oder „Textdatei“ → „Notizen“ wählen → Zielordner wählen oder neu anlegen.
  - Word-Dateien: Titel, Fett/Kursiv/Unterstrichen/Durchgestrichen, Überschriften, Aufzählungen, Checklisten (abgehakt oder offen), Tabellen und Bilder.
  - Text: Zeilen, Aufzählungen und Checklisten. PDF: Seiten als Bilder wie ein Scan. Bilder: als Bild-Notiz.
- In „Ordner“ gibt es „Aus Samsung Notes importieren“ mit Anleitung, „Samsung Notes öffnen“ und „Dateien auswählen“ (für schon gespeicherte Dateien).
- Die Notizen-App ist außerdem Ziel im Teilen-Menü für Text, Links und Bilder aus jeder App.

## Neu in v5.11.4 – Notizen: kein Datenverlust mehr, Scanner, Verschieben
- **Notizen verschwinden nicht mehr**: Speicherte die App automatisch und ging gleichzeitig in den Hintergrund, schrieben zwei Vorgänge parallel in dieselbe Datei; der Verlierer löschte dabei die Notizdatei. Jetzt wird nacheinander geschrieben, auf den Speicher synchronisiert und atomar ersetzt; die alte Datei wird nie vorher gelöscht. Dazu eine Sicherung (`notes.json.bak`), aus der beim Start automatisch wiederhergestellt wird. Eine unlesbare Datei wird beiseitegelegt, nie überschrieben.
- Eine Notiz, die Inhalt hatte und geleert wurde, landet in „Zuletzt gelöscht“ statt endgültig zu verschwinden.
- **Dokumente scannen stürzt nicht mehr ab**: Der Google-Scanner wurde im eigenen Prozess der Notizen nicht initialisiert. Beim ersten Scannen lädt Google Play-Dienste das Scanner-Modul.
- **Büroklammer-Menü** erscheint über der Tastatur statt dahinter.
- **Tippen irgendwo auf die Notiz** öffnet die Tastatur und schreibt am Ende weiter.
- **Tastatur zu = Bearbeiten beendet**: Die untere Leiste mit Büroklammer verschwindet nicht mehr.
- **Notiz bewegen**: kompaktes Fenster wie auf dem iPhone, mit „Neuer Ordner“ (anlegen und direkt hineinbewegen).
- Menüs sind nicht mehr durchscheinend.

## Neu in v5.11.3 – Notizen: Diagnose-Rekorder
- Die Notizen-App zeichnet die letzten Minuten auf: jede Berührung (wann sie ankam, wie lange die App brauchte), jeden Klick, Seitenwechsel, Tastatur und Fokus. Bei jeder Stockung über 0,05 s auf dem Hauptthread steht dazu, welcher Code gerade lief.
- Die Aufzeichnung steht in `Download/TMS-Notizen-Diagnose.txt` und wird alle paar Sekunden aktualisiert.
- Hintergrund: Die Probleme mit verzögerten oder verschluckten Berührungen treten im Emulator in keinem Test auf (Kaltstart, Fold-Auflösung, schnelles Tippen, mehrere Finger, Speicherzugriffe). Die Aufzeichnung vom Gerät zeigt, wo es hängt.

## Neu in v5.11.2 – Notizen: Fehler behoben, echtes Icon
- **Notizen werden gespeichert**: Beim Verlassen einer Notiz wurde ihr Text mit dem leeren Anfangszustand überschrieben und die Notiz als „leer“ gelöscht. Behoben – auch nach dem Beenden der App ist alles da.
- **Enter macht immer eine neue Zeile**: Tastaturen wie die Samsung-Tastatur senden bei Enter zusätzlich „Fertig“, was die Tastatur schloss. Das wird jetzt abgefangen.
- **Kein doppeltes Zurückspringen mehr**: Ein Bildschirm, der gerade hinausgleitet, nimmt keine Tipps mehr an; zweimal schnell „Zurück“ springt nicht mehr zwei Ebenen.
- **Zeichnen**: Die Tastatur schließt sich, und der vorher getippte Text bleibt erhalten. Auch „Fertig“ (gelber Haken) schließt die Tastatur jetzt zuverlässig.
- **Original-Icon**: Das nachgebaute iOS-Notizen-Icon (gelbes Band, Perforation, zwei Linien) wurde nie benutzt, weil die App intern „…launcher.notes“ heißt. Jetzt wird es überall verwendet.
- **Diagnose**: Hängt die Notizen-App länger als 5 Sekunden oder stürzt sie ab, steht die genaue Stelle in `Download/TMS-Notizen-Diagnose.txt`.

## Neu in v5.11.1 – Profilbilder in der Suche
- **WhatsApp-Kontakte mit Profilbild**: Im Toptreffer erscheinen Chats (WhatsApp, Telegram …) als runde Profilbilder statt als weiße Flächen; Symbole wie „Kamera“ oder „Neuer Tab“ bleiben weiß auf Glas.
- **Chats in der Suche**: Ein Name findet den passenden Chat, unter dem Namen der App, mit Profilbild; ein Tipp öffnet ihn direkt.
- **Kontakte mit Foto** statt Initialen.
- Behoben: Spotlight durchsuchte nur die ersten 200 Kontakte.
- Auch im Menü beim langen Drücken auf ein App-Icon sind Chats jetzt runde Profilbilder.

## Neu in v5.11.0 – Notizen-App wie auf dem iPhone
- **Eigene Notizen-App in TMS OS**, immer installiert (App „Notizen“ mit iPhone-Icon in der App-Mediathek, auf den Home-Bildschirm legbar). Gestaltet nach der Notizen-App aus iOS 26, hell und dunkel.
- **Ordner**: „Auf meinem Gerät“ (einklappbar), „Alle auf meinem Gerät“, „Notizen“, eigene Ordner mit Anzahl, „Zuletzt gelöscht“ (30 Tage). Neuer Ordner, „Bearbeiten“ zum Umbenennen und Löschen.
- **Notizliste**: großer Titel mit Anzahl, Gruppen nach Datum (Heute, Gestern, Vorherige 7/30 Tage, Monate), „Angeheftet“. Wischen nach rechts heftet an, nach links: Bewegen, Löschen. Langes Drücken: Menü. „…“: Notizen auswählen, Sortieren, Gruppieren, Ordner umbenennen. Suche und Neue-Notiz-Knopf unten.
- **Editor**: Datum oben, erste Zeile als Titel, gelber „Fertig“-Haken, Rückgängig, Teilen. Über der Tastatur: Aa, Checkliste, Tabelle, Anhang, Zeichnen. **Format**: Titel, Überschrift, Unterüberschrift, Text, Monostil; fett, kursiv, unterstrichen, durchgestrichen, Markierung, Textfarbe; Aufzählung, Strich-Liste, Nummerierung, Einrücken, Zitat. Listen laufen beim Zeilenumbruch weiter.
- **Checklisten** zum Abhaken, **Tabellen** (Zeilen/Spalten einfügen und löschen), **Fotos** (auswählen oder aufnehmen), **Dokumente scannen**, **Zeichnen** (Stift, Marker, Bleistift, Radierer, Farben).
- **Notiz sperren** mit Touch ID; gesperrte Notizen zeigen in Listen nur „Gesperrt“.
- **Spotlight findet Notizen** (Titel und Inhalt, gesperrte nicht).
- **Notizen-Widget** zeigt jetzt die neuesten Notizen der App.
- Notizen läuft in einem eigenen Prozess: ein Fehler dort kann den Home-Bildschirm nicht mitreißen.

## Neu in v5.10.1 – Suche wie auf dem iPhone, Teil 2
- **Die „Suchen“-Pille wird zum Suchfeld**: Sie wächst an ihrer Stelle in die Breite, wird zum Feld und fährt mit der Tastatur hoch; beim Schließen schrumpft sie wieder in die Pille.
- **Hintergrund wie beim iPhone**: viel stärker verschwommen und dunkel, ohne erkennbare Formen oder Farbflecken.
- **Siri-Vorschläge und Toptreffer**: schmalere Karte mit mehr Rand, Icons füllen sie fast aus, leichtere Beschriftung, rundere Ecken, Karte heller abgesetzt.
- Suchfeld etwas höher, wie beim iPhone.

## Neu in v5.10.0 – Suche und App-Mediathek wie auf dem iPhone
- **Spotlight neu gebaut**: Der Home-Bildschirm verschwimmt dahinter, das Suchfeld wächst aus der „Suchen“-Pille und sitzt direkt über der Tastatur. Ohne Eingabe: „Siri-Vorschläge“ (8 Apps, Weniger/Mehr) und Kurzbefehl-Vorschläge der Apps.
- **Beim Tippen**: „Toptreffer“ mit bis zu drei Schnellaktionen der App als runde Glas-Knöpfe, „Vorschläge“ (Websuche ↗), „Einstellungen“ (Systemseiten wie WLAN, Bluetooth, Akku … und „Einstellungen für X öffnen“), Apps mit „Mehr“, Kontakte, **Dateien** und **Fotos**, „Mehr Ergebnisse anzeigen“ und „In Apps suchen“. Im Feld steht die Vervollständigung („gm — Google Maps“), Return öffnet sie; das Mikrofon diktiert.
- Beim Scrollen geht die Tastatur weg und das Suchfeld wird zur Leiste unten.
- Dateien und Fotos erscheinen erst, nachdem du unten in der Suche „Erlauben“ getippt hast.
- **App-Mediathek**: Die Suche zeigt eine flache A–Z-Liste über der verschwommenen Mediathek, die A–Z-Leiste lässt sich antippen und entlangwischen. Die Ordner scrollen unter die „App-Mediathek“-Pille und scheinen verschwommen durch. Beim Öffnen einer Kategorie verschwimmt die Mediathek, der Titel ist fett. Ordner-Kacheln etwas heller, wie Glas.

## Neu in v5.9.1
- Das Home-Raster wird an der Fensterhöhe gemessen: eine offene Tastatur (Suche, Spotlight) packt die Seiten nicht mehr um, und die App-Mediathek bleibt dort, wo sie ist.
- Ändert sich die Zahl der Home-Seiten, während die Mediathek offen ist, bleibt man auf der Mediathek.

## Neu in v5.9.0 – Apps sichern, Ausgeblendet-Ordner
- **Mit Touch ID sichern** (App lange drücken → Mehr): Die App öffnet sich nur nach Fingerabdruck oder Geräte-Code – vom Home-Bildschirm, Dock, Mediathek, Spotlight, Ordnern und Kurzbefehlen. Sichern und Entsichern verlangen ebenfalls Touch ID.
- **Ausgeblendet** in der App-Mediathek: wirkt immer leer, zeigt die ausgeblendeten Apps erst nach Touch ID. Dort lassen sie sich über langes Drücken wieder einblenden.
- Behoben: Ein Tipp während eines Seitenwechsels (z. B. direkt nach der Home-Taste) ließ den Home-Bildschirm zwischen zwei Seiten stehen.

## Neu in v5.8.7
- App-Mediathek: Wischt man von der geöffneten Suche weg, wird die Suche beendet und die Tastatur geschlossen.

## Neu in v5.8.6 – kein Einfrieren mehr
- Ein abgebrochenes Herunterziehen (zweiter Finger, Systemgeste, Seitenwechsel) ließ Spotlight unsichtbar halb offen stehen; es fing dann jede Berührung ab und der Home-Bildschirm wirkte eingefroren. Spotlight ist jetzt immer ganz auf oder ganz zu und nimmt Berührungen nur an, wenn es offen ist.
- Gleiches Muster behoben bei der Kategorie-Ansicht der App-Mediathek und beim Ablegen von Icons: unterbrochene Animationen hinterlassen keinen blockierten Zustand mehr.
- Solvado-Zwischenspeicher wird beim Start nicht mehr auf dem Haupt-Thread gelesen.

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
