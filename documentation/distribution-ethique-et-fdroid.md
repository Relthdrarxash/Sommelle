# Kit de distribution éthique (F-Droid, IzzyOnDroid, annuaires libres et soignants)

Ce document regroupe les fiches techniques prêtes à copier-coller pour référencer **Sommelle**, **Lunaison** et **Respire** (Pilmil Studio) dans les dépôts d'applications libres et auprès des réseaux de santé.

---

## 1. Métadonnées F-Droid (`metadata/fr.sommelle.yml`)

```yaml
Categories:
  - Science & Education
  - Sports & Health
License: MPL-2.0
AuthorName: Pilmil Studio
AuthorEmail: pilmil.studio@proton.me
WebSite: https://relthdrarxash.github.io/sommelle/
SourceCode: https://github.com/Relthdrarxash/sommelle
IssueTracker: https://github.com/Relthdrarxash/sommelle/issues
Changelog: https://github.com/Relthdrarxash/sommelle/releases

AutoName: Sommelle

RepoType: git
Repo: https://github.com/Relthdrarxash/sommelle.git

Builds:
  - versionName: 1.2.0
    versionCode: 10200
    commit: v1.2.0
    subdir: app
    gradle:
      - yes
    prebuild: sed -i -e 's/versionCode = .*/versionCode = 10200/' -e 's/versionName = .*/versionName = "1.2.0"/' build.gradle.kts

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.2.0
CurrentVersionCode: 10200
```

---

## 2. Pages dédiées aux professionnels de santé

- **Sommelle** : `https://relthdrarxash.github.io/sommelle/professionnels.html` (et `/en/professionals.html`) avec fiche PDF à imprimer (`fiche-prescripteurs.pdf`).
- **Lunaison** : `https://relthdrarxash.github.io/lunaison/professionnels.html` (et `/en/professionals.html`).
