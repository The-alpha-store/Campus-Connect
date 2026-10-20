# CampusConnect

A native Android app (Kotlin + Jetpack Compose, no XML layouts) that gives a university one trusted place for
**timetables, official announcements, study materials (PDF), deadlines and notifications**, for students,
lecturers and administrators. Backend: Supabase (PostgreSQL, login, file storage). Push: Firebase Cloud Messaging.
Languages: English and Kiswahili.

## Start here

**New to coding? Open [`SETUP_GUIDE.md`](SETUP_GUIDE.md) and follow it from Part 1.**

| Guide | What it covers |
|---|---|
| [`SETUP_GUIDE.md`](SETUP_GUIDE.md) | Everything, step by step: tools, database, connecting the app, Firebase, GitHub + CI/CD, signing keys, first run |
| [`docs/PROJECT_TOUR.md`](docs/PROJECT_TOUR.md) | What every folder and file is for |
| [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) | Common errors and fixes |
| [`docs/SECURITY_CHECKLIST.md`](docs/SECURITY_CHECKLIST.md) | One-page checklist before going live |
| [`docs/GLOSSARY.md`](docs/GLOSSARY.md) | Plain-language meaning of the technical words |

## Quick facts

- Package / application id: `com.campusconnect.app`
- Minimum Android: 8.0 (API 26)
- Database tests: `bash supabase/tests/run_tests.sh` (63 checks, also run automatically by GitHub Actions)
- Secrets live in `local.properties` and GitHub Secrets, **never** in the code

## Features

Sign-up with admin approval; role-based home (student, lecturer, admin); timetable with change and cancel alerts and
clash warnings; announcements to a course, programme or the whole campus; PDF study materials with in-app viewer and
offline copies; deadline calendar with 24-hour reminders; instant push notifications (with a background-check fallback);
admin tools for programmes, terms, courses, classes and enrolments; English and Kiswahili; light and dark themes.

## Status

SQL: tested on PostgreSQL 16. Android code: written but not yet compiled by its author; expect to fix a few small
errors on first build (see Part 9 of the setup guide).
