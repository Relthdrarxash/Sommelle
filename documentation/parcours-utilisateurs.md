# Parcours utilisateurs formalisés : Sommelle

Novembre 2026. Ce document formalise le parcours patient et le parcours praticien dans **Sommelle** (accompagnement de l'insomnie chronique par la TCC-I en 6 à 8 semaines).

---

## 1. Parcours patient : de l'accueil au maintien après 6 semaines

| Étape | Ce que vit le patient | Réponse dans Sommelle | Fichier d'implémentation |
|---|---|---|---|
| **1. Questionnaire d'entrée (2 min)** | Vérifier que la restriction du temps au lit est adaptée et sans danger | Orientation déterministe vers l'un des 3 parcours : **Programme complet**, **Programme sans restriction** (contrôle du stimulus + cognitif), ou **Orientation médicale** (apnée suspectée, trouble bipolaire, épilepsie, grossesse, travail de nuit, retard de phase) | `ScreeningEvaluator.kt`, `OnboardingScreens.kt` |
| **2. Semaine 0 (Observation)** | Observer son sommeil actuel sans consigne culpabilisante | Saisie matinale en 20 secondes (Consensus Sleep Diary Core, durées estimées par boutons pour ne jamais chronométrer la nuit) | `DiaryScreen.kt`, `SleepMetrics.kt` |
| **3. Semaine 1 (Première fenêtre & contrôle du stimulus)** | Traverser la semaine la plus exigeante en comprenant pourquoi la fatigue diurne est temporaire (Simon et al., 2025) | Calcul de la fenêtre initiale (`moyenne TST` arrondie aux 15 min supérieures, plancher `5 h 30` ou `6 h 00`), heure de lever fixe 7 j/7, et affichage sur l'accueil des **repères personnalisés du soir** (*Sas calme dès [coucher − 45 min]* et *Dernier café ou thé avant [lever + 9 h]*) | `SleepWindowCalculator.kt`, `HomeScreen.kt` |
| **4. Éveil nocturne (« Je n'arrive pas à dormir »)** | Savoir quoi faire à 3 h du matin sans regarder l'heure (Tang et al., 2007) | Écran nocturne ambre sur fond noir, sans horloge ni chiffre, rappelant la consigne de Bootzin (sortir du lit si l'éveil se prolonge) + **repère sonore doux à 20 minutes** facultatif sans aucun décompte visible + guide de respiration `4 s / 6 s` | `NightScreen.kt`, `RelaxationScreens.kt` |
| **5. Semaines 2 à 5 (Consolidation & exercices)** | Élargir progressivement la fenêtre et apaiser les pensées qui entretiennent l'insomnie | Ajustement hebdomadaire automatique (`+15 min` si efficacité `≥ 85 %`, `−15 min` si `< 80 %`, seuils abaissés de 5 points dès 65 ans) + déblocage progressif des 5 exercices cognitifs + carte interactive **« Mes repères du soir et de la chambre »** (5 habitudes cochables issues de *CBT-i Coach*) | `ProgramPlanner.kt`, `CognitiveScreens.kt` |
| **6. Semaine 6 et maintien (Prévention des rechutes)** | Voir le chemin parcouru et savoir quoi faire si une mauvaise nuit revient plus tard | Bilan avant/après (semaine 0 vs semaine 6) + guide dédié **« Que faire si une mauvaise nuit revient plus tard ? »** (distinguer une nuit isolée d'une rechute de plus de 2 semaines) | `WeekScreen.kt`, `ProgramOutcome.kt` |

---

## 2. Parcours praticien (médecin, psychologue TCC-I, pharmacien)

1. **Avant de conseiller l'application :** consultation de la fiche imprimable d'une page (`docs/professionnels.html`) et du dossier scientifique complet avec correspondance fichier par fichier (`docs/protocole.html`).
2. **En consultation de suivi (5 minutes) :** le patient ouvre ou partage le **Résumé pour le médecin** (PDF d'une page A4 ou export CSV de l'agenda complet) généré localement sur son téléphone (`ClinicianReport.kt`).
