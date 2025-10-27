# Discord MCP - Extensions Documentation

## 🎯 Neue Features für Text-Content-Reading

Dieser Fork erweitert das bestehende [discord-mcp](https://github.com/SaseQ/discord-mcp) um erweiterte Text-Content-Analyse-Funktionen.

---

## ✨ Neue Tools

### 1. `read_messages_by_intent`

**Beschreibung:** Liest Nachrichten aus einem Channel gefiltert nach Intent/Thema.

**Parameter:**
- `channelId` (required): Discord Channel ID
- `intent` (required): Intent-Typ ('question', 'answer', 'project_showcase', 'bug_report', 'feedback', 'general')
- `timespanDays` (optional): Such-Rückblick in Tagen (Standard: 30)
- `limit` (optional): Maximale Anzahl Nachrichten (Standard: 50)

**Beispiel:**
```javascript
read_messages_by_intent(
  channelId: "1039884405302378567",
  intent: "question",
  timespanDays: "30",
  limit: "20"
)
```

**Output:**
```
**Retrieved 15 messages with intent 'question':**
- (ID: 123456) [Intent: question] **[User]** `2024-10-15`: ```Wie erstelle ich einen Discord Bot?```
- ...
```

---

### 2. `search_messages_by_content`

**Beschreibung:** Sucht Nachrichten nach Inhalt/Keywords.

**Parameter:**
- `channelId` (required): Discord Channel ID
- `query` (required): Such-Keywords (getrennt durch Leerzeichen)
- `limit` (optional): Maximale Anzahl Nachrichten (Standard: 50)

**Beispiel:**
```javascript
search_messages_by_content(
  channelId: "968250156694790184",
  query: "Python async error",
  limit: "10"
)
```

**Output:**
```
**Found 8 messages matching 'Python async error':**
- (ID: 123456) [Intent: question] **[User]** `2024-10-10`: ```Ich habe einen Python async error```
- ...
```

---

### 3. `analyze_channel_stats`

**Beschreibung:** Analysiert Nachrichten-Statistiken eines Channels nach Intent-Typen.

**Parameter:**
- `channelId` (required): Discord Channel ID
- `timespanDays` (optional): Tage zum Analysieren (Standard: 30)

**Beispiel:**
```javascript
analyze_channel_stats(
  channelId: "968250156694790184",
  timespanDays: "90"
)
```

**Output:**
```
**Channel Statistics (last 90 days, 234 messages):**
- Questions: 45 (19.2%)
- Answers: 38 (16.2%)
- Project Showcases: 12 (5.1%)
- Bug Reports: 23 (9.8%)
- Feedback: 8 (3.4%)
- General: 108 (46.2%)
```

---

## 🧠 Intent-Klassifikation

### Pattern-basierte Klassifikation (Current)

Die aktuelle Implementierung nutzt **Pattern-Matching** für schnelle Klassifikation:

**Question:**
- Enthält `?`
- Beginnt mit: "wie", "warum", "wieso", "welche"
- Beginnt mit: "was ist", "kann "

**Answer:**
- Beginnt mit: "you can", "du kannst"
- Enthält: "answer", "solution", "lösung", "try", "versuch"

**Project Showcase:**
- Enthält: "github.com", "gitlab.com"
- Enthält: "check out my", "my project", "mein projekt"
- Enthält Link + "project"

**Bug Report:**
- Enthält: "error", "fehler", "bug", "exception", "crash"
- "funktioniert nicht", "doesn't work"

**Feedback:**
- Enthält: "feedback", "vorschlag", "suggestion"
- "should" + "be able"

**General:**
- Alles andere

---

## 🔧 Technische Implementierung

### Neue Service-Klasse

**Datei:** `MessageClassificationService.java`

**Standort:** `src/main/java/dev/saseq/services/`

**Dependencies:** Keine zusätzlichen Dependencies nötig (nutzt nur JDA)

**Integration:** Service wurde registriert in `DiscordMcpConfig.java`

### Build & Test

```bash
cd discord-mcp

# Build
mvn clean package

# Test mit eigener Bot-Token
java -jar target/discord-mcp-0.0.1-SNAPSHOT.jar
```

### Docker (Optional)

```bash
# Build Docker Image
docker build -t discord-mcp-extended:latest .

# Run
docker run -rm -i \
  -e DISCORD_TOKEN=$BOT_TOKEN \
  -e DISCORD_GUILD_ID=443790920576532490 \
  discord-mcp-extended:latest
```

---

## 📊 Use Cases für DevSky

### Use Case 1: Audit-Analyse verbessern

**Aktuell:**
```javascript
read_messages(channelId: "...", count: "5")
// Liefert alle Nachrichten, keine Filterung
```

**Mit Erweiterung:**
```javascript
read_messages_by_intent(channelId: "...", intent: "question", timespanDays: "30")
// Liefert NUR Fragen - für bessere Analyse
```

### Use Case 2: Feedback sammeln

```javascript
read_messages_by_intent(channelId: "vorschlaege", intent: "feedback")
// Alle Feedback-Nachrichten von Community
```

### Use Case 3: Channel Health Check

```javascript
analyze_channel_stats(channelId: "coding-chat")
// Zeigt: Welche Art von Aktivität gibt es? Zu viele Fragen, keine Answers?
```

### Use Case 4: Content-Suche

```javascript
search_messages_by_content(channelId: "...", query: "Python async await")
// Findet alle Diskussionen über spezifisches Thema
```

---

## 🚀 Nächste Schritte

### Phase 1: Pattern-basierte Klassifikation (✅ IMPLEMENTIERT)
- Schnell
- Keine API-Kosten
- Funktioniert out-of-the-box

### Phase 2: AI-Integration (Optional)
- Höhere Genauigkeit
- Kontextverständnis
- API-Kosten: ~$10-50/month

**Implementierung:**
```java
private String aiClassifyIntent(String content) {
    String prompt = String.format(
        "Classify: %s\nCategories: question, answer, showcase, bug, feedback, general",
        content
    );
    return claudeApi.complete(prompt);
}
```

### Phase 3: Semantic Search (Optional)
- Embeddings für Ähnlichkeitssuche
- Vektor-Speicher Integration

---

## 📝 Änderungen am Code

### Neue Datei:
- `src/main/java/dev/saseq/services/MessageClassificationService.java`

### Geänderte Datei:
- `src/main/java/dev/saseq/configs/DiscordMcpConfig.java`
  - Import hinzugefügt: `MessageClassificationService`
  - Service registriert in `toolObjects()`

### Keine Änderungen:
- `MessageService.java` (unverändert)
- `pom.xml` (keine neuen Dependencies)
- `application.properties` (unverändert)

---

## ✅ Testing

### Lokaler Test:

1. Bot-Token setzen:
```bash
export DISCORD_TOKEN="dein_token"
export DISCORD_GUILD_ID="443790920576532490"
```

2. Starten:
```bash
mvn spring-boot:run
```

3. Test-Calls:
- `read_messages_by_intent(channelId="...", intent="question")`
- `search_messages_by_content(channelId="...", query="Python")`
- `analyze_channel_stats(channelId="...")`

### Integration Test:

Mit DevSky Server testen:
```javascript
// Test Counter Channel
read_messages_by_intent(
  channelId: "1039884405302378567",
  intent: "general",
  limit: "10"
)
```

---

## 🎯 Vorteile der Erweiterung

### Für Audit-Analyse:
- ✅ **Intent-Filterung:** Finde nur relevante Nachrichten
- ✅ **Statistiken:** Verstehe Channel-Aktivität besser
- ✅ **Content-Suche:** Finde spezifische Themen

### Für Revive-Plan:
- ✅ **Channel-Health:** Welche Channels sind aktiv?
- ✅ **Feedback-Aufnahme:** Welche Wünsche hat die Community?
- ✅ **Content-Analyse:** Was wird diskutiert?

### Performance:
- ✅ **Schnell:** Pattern-basiert, keine API-Calls
- ✅ **Kostenlos:** Keine externen Dependencies
- ✅ **Offline:** Funktioniert ohne Internet

---

**Status:** ✅ Implementiert und ready for Testing  
**Next:** Integration mit DevSky Audit-Process

