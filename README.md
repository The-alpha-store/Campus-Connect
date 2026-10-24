# CampusConnect

CampusConnect is an Android (Kotlin + Jetpack Compose) campus app for universities in Tanzania. The project is being moved from a local prototype to a Supabase-backed application.

## What's in this repository

- `app/` — Android application source.
- `app/src/main/java/com/example/data/supabase/SupabaseClient.kt` — authenticated Supabase Auth, PostgREST, Storage and Edge Function HTTP transport.
- `supabase/migrations/` — database schema, row-level security (RLS), role limits and audit rules.
- `supabase/functions/campus-wipe-files/` — server-side campus wipe + Storage cleanup function.
- `supabase/tests/` — database authorization tests.
- `.github/workflows/` — Android CI, database tests, database deployment, release and Edge Function deployment workflows.
- `docs/` — setup, security and operational notes.

## Important status note

The Supabase database schema and a reusable network client are included, but the existing screens and `CampusRepository` / `AuthRepository` still contain local in-memory prototype behavior. The app is **not yet end-to-end live-backend connected**: do not use it with real student records until the ViewModel/repositories are migrated to the Supabase client and the integration tests pass. The default build disables the demo entry point; this alone does not convert the local repositories into a live backend.

## Privacy model

A student's programme/course selection is not authorization by itself. The server must enforce university membership, approved account status, course enrollment, programme audience and role using RLS on every query. A Computer Science student must not be able to read Human Resources materials, submissions, class lists or private profile data by changing client filters. UI filtering is only for usability; RLS is the security boundary.

Class representatives may publish only when posting on behalf of a selected responsible lecturer, and only for that lecturer's authorized course/audience. The server must validate the relationship rather than trusting the selected lecturer in the form.

## Quick setup

1. Create a Supabase project.
2. Apply migrations with `supabase link --project-ref YOUR_PROJECT_REF` and `supabase db push`.
3. Configure `SUPABASE_URL` and `SUPABASE_ANON_KEY` as Gradle properties or environment variables for the Android build. The publishable/anon key is allowed in a client; **never use the service-role key in the APK**.
4. Deploy `campus-wipe-files` after setting the function's Supabase environment. The function uses the caller's JWT for the database RPC, then uses the service-role key only server-side to delete Storage objects.
5. Run `supabase test db` and the Android CI before using a hosted project.

See [`docs/BACKEND.md`](docs/BACKEND.md) for the full configuration and security checklist.
