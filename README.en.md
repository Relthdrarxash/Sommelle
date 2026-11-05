# Sommelle

[Français](README.md) · **English**

[![CI](https://github.com/Relthdrarxash/sommelle/actions/workflows/ci.yml/badge.svg)](https://github.com/Relthdrarxash/sommelle/actions/workflows/ci.yml)

An Android app that supports people with insomnia, based on CBT-I (cognitive behavioral therapy for insomnia), the first-line treatment recommended by European and American guidelines. Free, in English and French, no account and no ads. No data leaves the phone.

Sommelle is a self-help and education tool. It is not a medical device and does not replace medical advice.

**[Sommelle website](https://relthdrarxash.github.io/sommelle/en/)** · [For health professionals](https://relthdrarxash.github.io/sommelle/en/professionals.html) · [Privacy](https://relthdrarxash.github.io/sommelle/en/privacy.html)

## What the app does

Over six to eight weeks:

- **An entry questionnaire** rules out sleep restriction when it is not advised (possible apnea, bipolar disorder, epilepsy, pregnancy, safety-critical job) and points to a doctor when needed.
- **A sleep diary** is filled in each morning in about twenty seconds, without a keyboard.
- **A sleep window** (bedtime and rise time) is calculated after an observation week, then adjusted each week based on your nights.
- **A night screen**, almost black, reminds you what to do when sleep does not come.
- **Five cognitive exercises** unlock over the program: ideas about sleep, putting the day to rest, paradoxical intention, decatastrophizing, relaxation.
- **A before/after review** compares the observation week with the latest nights.
- **A summary for the doctor**, to show on screen or share as a PDF and a spreadsheet.
- Optional **reminders** in the morning and evening.

## Preview

<table>
  <tr>
    <td align="center"><img src="docs/captures/en/accueil.png" width="220" alt="Home: sleep window, bedtime 12:30 am and rise time 6:45 am, Log my night button"><br>Home</td>
    <td align="center"><img src="docs/captures/en/agenda_heure.png" width="220" alt="Guided diary: bedtime in large type with two buttons, minus 15 and plus 15 minutes"><br>Guided diary</td>
    <td align="center"><img src="docs/captures/en/agenda_estimation.png" width="220" alt="Guided diary: large chips to estimate how long it took to fall asleep"><br>Estimates</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/captures/en/agenda_resume.png" width="220" alt="Diary summary: the whole night on one card, each line editable"><br>Diary summary</td>
    <td align="center"><img src="docs/captures/en/bilan.png" width="220" alt="Weekly review: summary sentence, new window and chart of time in bed and time asleep"><br>Weekly review</td>
    <td align="center"><img src="docs/captures/en/nuit.png" width="220" alt="Almost black night screen in warm tones, three instructions and a button"><br>I can't sleep</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/captures/en/resume_medecin.png" width="220" alt="Summary for the doctor: program, pathway, current window, with buttons to share as PDF and spreadsheet"><br>Summary for the doctor</td>
    <td align="center"><img src="docs/captures/en/exercices.png" width="220" alt="List of cognitive exercises with their status, done or to do"><br>Exercises</td>
    <td align="center"><img src="docs/captures/en/bilan_programme.png" width="220" alt="Program results: comparison between the observation week and the last seven nights"><br>Before/after review</td>
  </tr>
</table>

Screenshots are generated from the screen code on every change.

## Why Sommelle is free

Cognitive behavioral therapy for insomnia (CBT-I) is the recommended first-line treatment, but it remains hard to access: few trained practitioners, long waits, and a cost often paid by the patient. Sommelle aims to help as many people as possible, because everyone deserves help to sleep better. The app is free, open source and collects no data. It does not replace a doctor, but it lets you get started.

## Principles

- **Based on guidelines.** Consensus Sleep Diary, sleep restriction and stimulus control following European (2023) and American (2021) guidelines. The rules, thresholds and sources are described in the [scientific dossier](https://relthdrarxash.github.io/sommelle/en/protocol.html), with their limitations.
- **100% local.** No account, no server, no Internet permission, no trackers. Since the code is public, anyone can check.
- **For everyone.** Designed for someone who is half awake, including older people or those less comfortable with technology: big buttons, no jargon, no hidden gestures, Atkinson Hyperlegible font, warm night theme. The choices are explained in the [interface principles](documentation/design.md) (in French).
- **No guilt.** No daily score, no streak to keep up, no red.
- **Only what is needed in the app, the explanations on the website.** The app sticks to what is needed to follow the program. The reason for each step, and what comes from a study versus a design choice, is on the [Why Sommelle works the way it does](https://relthdrarxash.github.io/sommelle/en/why.html) page.

## Languages

The app follows the phone's language: French on a phone set to French, English otherwise. Interface texts are Android resources (`values/` in English, `values-fr/` in French); times, durations and labels built from data come from `TextFormat` in `:core:presentation` (`FrenchFormat`, `EnglishFormat`).

## Building

Open the project in Android Studio, or from the command line:

```bash
./gradlew test                 # tests for the engine and screen logic
./gradlew :app:assembleDebug   # development build
```

The development APK for each change can also be downloaded from the Actions tab.

## Contact

Questions, feedback, comments from health professionals: [pilmil.studio@proton.me](mailto:pilmil.studio@proton.me).

## If you are in distress

If you are going through a hard time, call your local emergency number or a crisis line in your country, such as 988 in the United States. In France, 3114 (the national suicide prevention line) is free, 24/7.

## License

Sommelle is distributed under the [GNU GPL v3.0](LICENSE) license. You can use, study, modify and redistribute it, provided that any redistributed version remains published under the same license, with its source code.

## A note on development

Most of Sommelle's code was written with Claude, Anthropic's AI assistant. I reviewed it, tested it and maintain it: the program engine and screen logic are covered by automated tests, and I answer issues and messages myself.
