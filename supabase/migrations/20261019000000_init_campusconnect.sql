-- =====================================================================
-- CampusConnect: initial schema, security (RLS), triggers, storage
-- Single campus. All timestamps are UTC (timestamptz); display in
-- Africa/Dar_es_Salaam in the app.
-- Run with `supabase db push` or paste into the Supabase SQL editor.
-- =====================================================================

create extension if not exists pgcrypto;
create extension if not exists btree_gist;

-- ---------------------------------------------------------------------
-- 1. ENUMS
-- ---------------------------------------------------------------------
create type public.user_role         as enum ('student', 'lecturer', 'admin');
create type public.account_status    as enum ('pending', 'active', 'suspended');
create type public.session_status    as enum ('scheduled', 'changed', 'cancelled');
create type public.publish_status    as enum ('draft', 'published', 'archived');
create type public.material_status   as enum ('pending', 'approved', 'rejected');
create type public.audience_kind     as enum ('campus', 'programme', 'offering');
create type public.enrollment_status as enum ('active', 'dropped');

-- ---------------------------------------------------------------------
-- 2. TABLES
-- ---------------------------------------------------------------------
create table public.programmes (
  id         uuid primary key default gen_random_uuid(),
  code       text not null unique,
  name       text not null,
  created_at timestamptz not null default now()
);

create table public.academic_terms (
  id         uuid primary key default gen_random_uuid(),
  name       text not null unique,
  start_date date not null,
  end_date   date not null,
  is_current boolean not null default false,
  created_at timestamptz not null default now(),
  constraint academic_terms_dates_chk check (end_date > start_date)
);
create unique index academic_terms_one_current on public.academic_terms (is_current) where is_current;

create table public.profiles (
  id             uuid primary key references auth.users (id) on delete cascade,
  email          text,
  full_name      text not null,
  role           public.user_role not null default 'student',
  status         public.account_status not null default 'pending',
  student_number text unique,
  staff_number   text unique,
  programme_id   uuid references public.programmes (id) on delete set null,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now()
);

create table public.courses (
  id           uuid primary key default gen_random_uuid(),
  code         text not null unique,
  title        text not null,
  programme_id uuid references public.programmes (id) on delete set null,
  created_at   timestamptz not null default now()
);

create table public.course_offerings (
  id          uuid primary key default gen_random_uuid(),
  course_id   uuid not null references public.courses (id) on delete cascade,
  term_id     uuid not null references public.academic_terms (id) on delete restrict,
  lecturer_id uuid references public.profiles (id) on delete set null,
  cohort      text not null default 'main',
  created_at  timestamptz not null default now(),
  unique (course_id, term_id, cohort)
);

create table public.enrollments (
  id          uuid primary key default gen_random_uuid(),
  offering_id uuid not null references public.course_offerings (id) on delete cascade,
  student_id  uuid not null references public.profiles (id) on delete cascade,
  status      public.enrollment_status not null default 'active',
  created_at  timestamptz not null default now(),
  unique (offering_id, student_id)
);

create table public.class_sessions (
  id          uuid primary key default gen_random_uuid(),
  offering_id uuid not null references public.course_offerings (id) on delete cascade,
  starts_at   timestamptz not null,
  ends_at     timestamptz not null,
  venue       text,
  status      public.session_status not null default 'scheduled',
  note        text,
  created_by  uuid references public.profiles (id) on delete set null,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  constraint class_sessions_time_chk check (ends_at > starts_at),
  -- Hard block: two active sessions cannot use the same venue at the same time.
  constraint class_sessions_no_venue_overlap exclude using gist (
    venue with =,
    tstzrange(starts_at, ends_at) with &&
  ) where (status <> 'cancelled' and venue is not null)
);

create table public.announcements (
  id           uuid primary key default gen_random_uuid(),
  author_id    uuid not null references public.profiles (id) on delete cascade,
  audience     public.audience_kind not null,
  programme_id uuid references public.programmes (id) on delete cascade,
  offering_id  uuid references public.course_offerings (id) on delete cascade,
  title        text not null check (char_length(title) between 1 and 160),
  body         text not null check (char_length(body) between 1 and 5000),
  status       public.publish_status not null default 'draft',
  published_at timestamptz,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  constraint announcements_audience_chk check (
    (audience = 'campus'    and programme_id is null     and offering_id is null) or
    (audience = 'programme' and programme_id is not null and offering_id is null) or
    (audience = 'offering'  and offering_id  is not null and programme_id is null)
  )
);

create table public.notifications (
  id              uuid primary key default gen_random_uuid(),
  recipient_id    uuid not null references public.profiles (id) on delete cascade,
  kind            text not null default 'announcement',
  announcement_id uuid references public.announcements (id) on delete cascade,
  session_id      uuid references public.class_sessions (id) on delete cascade,
  title           text not null,
  body            text,
  read_at         timestamptz,
  created_at      timestamptz not null default now()
);

create table public.materials (
  id           uuid primary key default gen_random_uuid(),
  offering_id  uuid not null references public.course_offerings (id) on delete cascade,
  uploader_id  uuid not null references public.profiles (id) on delete cascade,
  title        text not null,
  storage_path text not null unique,         -- convention: <offering_id>/<uuid>.pdf
  mime_type    text not null default 'application/pdf',
  size_bytes   bigint check (size_bytes is null or size_bytes <= 10485760),
  status       public.material_status not null default 'approved',
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create table public.assignments (
  id           uuid primary key default gen_random_uuid(),
  offering_id  uuid not null references public.course_offerings (id) on delete cascade,
  created_by   uuid not null references public.profiles (id) on delete cascade,
  title        text not null,
  instructions text,
  due_at       timestamptz not null,
  status       public.publish_status not null default 'draft',
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create table public.device_tokens (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references public.profiles (id) on delete cascade,
  fcm_token  text not null unique,
  updated_at timestamptz not null default now()
);

create table public.audit_logs (
  id          uuid primary key default gen_random_uuid(),
  actor_id    uuid,
  action      text not null,
  entity_type text not null,
  entity_id   uuid,
  details     jsonb,
  created_at  timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 3. INDEXES
-- ---------------------------------------------------------------------
create index profiles_role_status_idx       on public.profiles (role, status);
create index profiles_programme_idx         on public.profiles (programme_id);
create index offerings_lecturer_idx         on public.course_offerings (lecturer_id);
create index offerings_term_idx             on public.course_offerings (term_id);
create index enrollments_student_idx        on public.enrollments (student_id) where status = 'active';
create index enrollments_offering_idx       on public.enrollments (offering_id) where status = 'active';
create index sessions_offering_start_idx    on public.class_sessions (offering_id, starts_at);
create index sessions_updated_idx           on public.class_sessions (updated_at);
create index announcements_published_idx    on public.announcements (published_at desc) where status = 'published';
create index announcements_offering_idx     on public.announcements (offering_id);
create index announcements_programme_idx    on public.announcements (programme_id);
create index notifications_recipient_idx    on public.notifications (recipient_id, created_at desc);
create index materials_offering_idx         on public.materials (offering_id);
create index assignments_offering_due_idx   on public.assignments (offering_id, due_at);
create index device_tokens_user_idx         on public.device_tokens (user_id);
create index audit_logs_created_idx         on public.audit_logs (created_at desc);
create index audit_logs_entity_idx          on public.audit_logs (entity_type, entity_id);

-- ---------------------------------------------------------------------
-- 4. HELPER FUNCTIONS (SECURITY DEFINER so RLS checks never recurse)
-- ---------------------------------------------------------------------
-- Role of the signed-in user, but only if the account is ACTIVE.
-- Pending and suspended users get NULL, which every policy treats as "no access".
create or replace function public.app_role()
returns public.user_role
language sql stable security definer set search_path = public
as $$
  select p.role from public.profiles p
  where p.id = auth.uid() and p.status = 'active'
$$;

create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = public
as $$ select coalesce(public.app_role() = 'admin', false) $$;

create or replace function public.my_programme()
returns uuid
language sql stable security definer set search_path = public
as $$ select p.programme_id from public.profiles p where p.id = auth.uid() $$;

create or replace function public.is_lecturer_of(p_offering uuid)
returns boolean
language sql stable security definer set search_path = public
as $$
  select coalesce(public.app_role() = 'lecturer', false)
     and exists (select 1 from public.course_offerings o
                 where o.id = p_offering and o.lecturer_id = auth.uid())
$$;

create or replace function public.is_enrolled(p_offering uuid)
returns boolean
language sql stable security definer set search_path = public
as $$
  select coalesce(public.app_role() = 'student', false)
     and exists (select 1 from public.enrollments e
                 where e.offering_id = p_offering
                   and e.student_id = auth.uid()
                   and e.status = 'active')
$$;

create or replace function public.can_manage_offering(p_offering uuid)
returns boolean
language sql stable security definer set search_path = public
as $$ select public.is_admin() or public.is_lecturer_of(p_offering) $$;

-- ---------------------------------------------------------------------
-- 5. TRIGGERS
-- ---------------------------------------------------------------------

-- 5.1 New auth user -> profile. Role and status are NEVER read from client metadata.
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  insert into public.profiles (id, email, full_name)
  values (
    new.id,
    new.email,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''), split_part(new.email, '@', 1))
  );
  return new;
end $$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 5.2 Only admins may change privileged profile fields (blocks self-promotion).
create or replace function public.protect_profile_fields()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  if auth.uid() is not null and not public.is_admin() then
    if new.id <> old.id
       or new.role <> old.role
       or new.status <> old.status
       or new.email is distinct from old.email
       or new.student_number is distinct from old.student_number
       or new.staff_number is distinct from old.staff_number
       or new.programme_id is distinct from old.programme_id then
      raise exception 'Only administrators can change role, status or identity fields'
        using errcode = '42501';
    end if;
  end if;
  new.updated_at := now();
  return new;
end $$;

create trigger profiles_protect
  before update on public.profiles
  for each row execute function public.protect_profile_fields();

-- 5.3 Generic updated_at touch.
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end $$;

create trigger announcements_touch before update on public.announcements
  for each row execute function public.touch_updated_at();
create trigger materials_touch before update on public.materials
  for each row execute function public.touch_updated_at();
create trigger assignments_touch before update on public.assignments
  for each row execute function public.touch_updated_at();

-- 5.4 Sessions: mark as "changed" when venue or time moves; always touch updated_at.
create or replace function public.session_before_update()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  if new.status = 'scheduled'
     and (new.venue is distinct from old.venue
          or new.starts_at <> old.starts_at
          or new.ends_at <> old.ends_at) then
    new.status := 'changed';
  end if;
  return new;
end $$;

create trigger class_sessions_before_update
  before update on public.class_sessions
  for each row execute function public.session_before_update();

-- 5.5 Announcements: stamp published_at.
create or replace function public.announcement_stamp_published()
returns trigger
language plpgsql
as $$
begin
  if new.status = 'published' and new.published_at is null then
    new.published_at := now();
  end if;
  return new;
end $$;

create trigger announcements_stamp
  before insert or update on public.announcements
  for each row execute function public.announcement_stamp_published();

-- 5.6 Fan-out: published announcement -> one notification per recipient.
create or replace function public.fanout_announcement()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  if new.status = 'published'
     and (tg_op = 'INSERT' or old.status is distinct from 'published') then
    insert into public.notifications (recipient_id, kind, announcement_id, title, body)
    select p.id, 'announcement', new.id, new.title, left(new.body, 140)
    from public.profiles p
    where p.status = 'active'
      and p.id <> new.author_id
      and (
        new.audience = 'campus'
        or (new.audience = 'programme' and p.role = 'student'
            and p.programme_id = new.programme_id)
        or (new.audience = 'offering' and p.id in (
              select e.student_id from public.enrollments e
              where e.offering_id = new.offering_id and e.status = 'active'))
      );
  end if;
  return null;
end $$;

create trigger announcements_fanout
  after insert or update on public.announcements
  for each row execute function public.fanout_announcement();

-- 5.7 Fan-out: timetable change/cancel -> notify enrolled students.
create or replace function public.fanout_session_change()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  if new.venue is distinct from old.venue
     or new.starts_at <> old.starts_at
     or new.ends_at <> old.ends_at
     or new.status is distinct from old.status then
    insert into public.notifications (recipient_id, kind, session_id, title, body)
    select e.student_id,
           'timetable',
           new.id,
           case when new.status = 'cancelled' then 'Class cancelled' else 'Timetable updated' end,
           c.code || ' | ' || coalesce(new.venue, 'Venue to be announced') || ' | '
             || to_char(new.starts_at at time zone 'Africa/Dar_es_Salaam', 'DD Mon HH24:MI')
    from public.enrollments e
    join public.course_offerings o on o.id = new.offering_id
    join public.courses c on c.id = o.course_id
    where e.offering_id = new.offering_id and e.status = 'active';
  end if;
  return null;
end $$;

create trigger class_sessions_fanout
  after update on public.class_sessions
  for each row execute function public.fanout_session_change();

-- 5.8 Audit log for important tables.
create or replace function public.audit_trigger()
returns trigger
language plpgsql security definer set search_path = public
as $$
declare
  v jsonb;
begin
  v := case when tg_op = 'DELETE' then to_jsonb(old) else to_jsonb(new) end;
  insert into public.audit_logs (actor_id, action, entity_type, entity_id, details)
  values (
    auth.uid(),
    tg_op,
    tg_table_name,
    (v ->> 'id')::uuid,
    case when tg_op = 'UPDATE'
         then jsonb_build_object('old', to_jsonb(old), 'new', v)
         else v end
  );
  return null;
end $$;

do $$
declare t text;
begin
  foreach t in array array[
    'profiles','programmes','academic_terms','courses','course_offerings',
    'enrollments','class_sessions','announcements','materials','assignments'
  ] loop
    execute format(
      'create trigger %I after insert or update or delete on public.%I
         for each row execute function public.audit_trigger()',
      t || '_audit', t);
  end loop;
end $$;

-- ---------------------------------------------------------------------
-- 6. CONFLICT CHECK RPC (warnings for the UI before saving a session)
-- ---------------------------------------------------------------------
create or replace function public.session_conflicts(
  p_offering uuid,
  p_starts   timestamptz,
  p_ends     timestamptz,
  p_venue    text default null,
  p_exclude  uuid default null
)
returns table (
  conflict_kind       text,
  conflict_session_id uuid,
  conflict_venue      text,
  conflict_starts_at  timestamptz,
  conflict_ends_at    timestamptz
)
language plpgsql stable security definer set search_path = public
as $$
begin
  if not public.can_manage_offering(p_offering) then
    raise exception 'Not allowed' using errcode = '42501';
  end if;

  return query
  select 'venue'::text, s.id, s.venue, s.starts_at, s.ends_at
  from public.class_sessions s
  where p_venue is not null
    and s.venue = p_venue
    and s.status <> 'cancelled'
    and tstzrange(s.starts_at, s.ends_at) && tstzrange(p_starts, p_ends)
    and s.id is distinct from p_exclude
  union all
  select 'lecturer'::text, s.id, s.venue, s.starts_at, s.ends_at
  from public.class_sessions s
  join public.course_offerings o on o.id = s.offering_id
  where o.lecturer_id = (select o2.lecturer_id from public.course_offerings o2 where o2.id = p_offering)
    and s.status <> 'cancelled'
    and tstzrange(s.starts_at, s.ends_at) && tstzrange(p_starts, p_ends)
    and s.id is distinct from p_exclude;
end $$;

-- ---------------------------------------------------------------------
-- 7. ROW LEVEL SECURITY
-- ---------------------------------------------------------------------
alter table public.programmes        enable row level security;
alter table public.academic_terms    enable row level security;
alter table public.profiles          enable row level security;
alter table public.courses           enable row level security;
alter table public.course_offerings  enable row level security;
alter table public.enrollments       enable row level security;
alter table public.class_sessions    enable row level security;
alter table public.announcements     enable row level security;
alter table public.notifications     enable row level security;
alter table public.materials         enable row level security;
alter table public.assignments       enable row level security;
alter table public.device_tokens     enable row level security;
alter table public.audit_logs        enable row level security;

-- profiles: own row (even when pending, so the app can show "awaiting approval"); admins see all.
create policy profiles_select on public.profiles for select to authenticated
  using (id = auth.uid() or public.is_admin());
create policy profiles_update on public.profiles for update to authenticated
  using (id = auth.uid() or public.is_admin())
  with check (id = auth.uid() or public.is_admin());
-- No insert/delete policies: profiles are created by trigger, removed via auth.users cascade.

-- Reference data: readable by any ACTIVE user, writable by admins only.
create policy programmes_select on public.programmes for select to authenticated
  using (public.app_role() is not null);
create policy programmes_admin on public.programmes for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

create policy terms_select on public.academic_terms for select to authenticated
  using (public.app_role() is not null);
create policy terms_admin on public.academic_terms for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

create policy courses_select on public.courses for select to authenticated
  using (public.app_role() is not null);
create policy courses_admin on public.courses for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- course_offerings: admin, the assigned lecturer, or enrolled students.
create policy offerings_select on public.course_offerings for select to authenticated
  using (
    public.app_role() is not null
    and (public.is_admin() or lecturer_id = auth.uid() or public.is_enrolled(id))
  );
create policy offerings_admin on public.course_offerings for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- enrollments: students see their own, lecturers see their class lists, admins manage.
create policy enrollments_select on public.enrollments for select to authenticated
  using (
    public.app_role() is not null
    and (public.is_admin() or student_id = auth.uid() or public.is_lecturer_of(offering_id))
  );
create policy enrollments_admin on public.enrollments for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- class_sessions: readable by people tied to the offering; writable by its lecturer or admin.
create policy sessions_select on public.class_sessions for select to authenticated
  using (
    public.app_role() is not null
    and (public.is_admin() or public.is_lecturer_of(offering_id) or public.is_enrolled(offering_id))
  );
create policy sessions_insert on public.class_sessions for insert to authenticated
  with check (public.can_manage_offering(offering_id) and created_by = auth.uid());
create policy sessions_update on public.class_sessions for update to authenticated
  using (public.can_manage_offering(offering_id))
  with check (public.can_manage_offering(offering_id));
create policy sessions_delete on public.class_sessions for delete to authenticated
  using (public.can_manage_offering(offering_id));

-- announcements
create policy announcements_select on public.announcements for select to authenticated
  using (
    public.app_role() is not null
    and (
      public.is_admin()
      or author_id = auth.uid()
      or (
        status = 'published'
        and (
          audience = 'campus'
          or (audience = 'programme' and programme_id = public.my_programme())
          or (audience = 'offering'
              and (public.is_enrolled(offering_id) or public.is_lecturer_of(offering_id)))
        )
      )
    )
  );
-- Admins may post to any audience; lecturers only to offerings they teach.
create policy announcements_insert on public.announcements for insert to authenticated
  with check (
    author_id = auth.uid()
    and (
      public.is_admin()
      or (audience = 'offering' and public.is_lecturer_of(offering_id))
    )
  );
create policy announcements_update on public.announcements for update to authenticated
  using (public.is_admin() or (author_id = auth.uid() and public.app_role() = 'lecturer'))
  with check (
    public.is_admin()
    or (author_id = auth.uid() and audience = 'offering' and public.is_lecturer_of(offering_id))
  );
create policy announcements_delete on public.announcements for delete to authenticated
  using (public.is_admin() or (author_id = auth.uid() and public.app_role() = 'lecturer'));

-- notifications: strictly personal. Rows are created by SECURITY DEFINER triggers only.
create policy notifications_select on public.notifications for select to authenticated
  using (recipient_id = auth.uid());
create policy notifications_update on public.notifications for update to authenticated
  using (recipient_id = auth.uid()) with check (recipient_id = auth.uid());

-- materials
create policy materials_select on public.materials for select to authenticated
  using (
    public.app_role() is not null
    and (
      public.is_admin()
      or uploader_id = auth.uid()
      or public.is_lecturer_of(offering_id)
      or (status = 'approved' and public.is_enrolled(offering_id))
    )
  );
create policy materials_insert on public.materials for insert to authenticated
  with check (uploader_id = auth.uid() and public.can_manage_offering(offering_id));
create policy materials_update on public.materials for update to authenticated
  using (public.is_admin() or (uploader_id = auth.uid() and public.is_lecturer_of(offering_id)))
  with check (public.is_admin() or (uploader_id = auth.uid() and public.is_lecturer_of(offering_id)));
create policy materials_delete on public.materials for delete to authenticated
  using (public.is_admin() or (uploader_id = auth.uid() and public.is_lecturer_of(offering_id)));

-- assignments
create policy assignments_select on public.assignments for select to authenticated
  using (
    public.app_role() is not null
    and (
      public.is_admin()
      or public.is_lecturer_of(offering_id)
      or (status = 'published' and public.is_enrolled(offering_id))
    )
  );
create policy assignments_insert on public.assignments for insert to authenticated
  with check (created_by = auth.uid() and public.can_manage_offering(offering_id));
create policy assignments_update on public.assignments for update to authenticated
  using (public.can_manage_offering(offering_id))
  with check (public.can_manage_offering(offering_id));
create policy assignments_delete on public.assignments for delete to authenticated
  using (public.can_manage_offering(offering_id));

-- device_tokens: each user manages only their own tokens.
create policy device_tokens_own on public.device_tokens for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- audit_logs: admins read only. Rows are written by the audit trigger.
create policy audit_logs_select on public.audit_logs for select to authenticated
  using (public.is_admin());

-- ---------------------------------------------------------------------
-- 8. PRIVILEGES
-- ---------------------------------------------------------------------
revoke all on all tables in schema public from anon;
alter default privileges in schema public revoke all on tables from anon;

-- Clients may only mark notifications as read; nothing else on that table is writable.
revoke update on public.notifications from authenticated;
grant update (read_at) on public.notifications to authenticated;

-- ---------------------------------------------------------------------
-- 9. STORAGE: private PDF bucket, path = <offering_id>/<file>.pdf
-- ---------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('materials', 'materials', false, 10485760, array['application/pdf'])
on conflict (id) do nothing;

-- Download only if the caller can see the matching row in public.materials (RLS applies).
create policy materials_storage_read on storage.objects for select to authenticated
  using (
    bucket_id = 'materials'
    and exists (select 1 from public.materials m where m.storage_path = storage.objects.name)
  );

-- Upload/delete only inside the folder of an offering the caller teaches (or admin).
create policy materials_storage_insert on storage.objects for insert to authenticated
  with check (
    bucket_id = 'materials'
    and exists (
      select 1 from public.course_offerings o
      where o.id::text = (storage.foldername(name))[1]
        and public.can_manage_offering(o.id)
    )
  );

create policy materials_storage_delete on storage.objects for delete to authenticated
  using (
    bucket_id = 'materials'
    and exists (
      select 1 from public.course_offerings o
      where o.id::text = (storage.foldername(name))[1]
        and public.can_manage_offering(o.id)
    )
  );

-- =====================================================================
-- After the first admin signs up, promote them ONCE from the SQL editor
-- (auth.uid() is null there, so the protection trigger allows it):
--
--   update public.profiles
--      set role = 'admin', status = 'active'
--    where email = 'you@example.com';
-- =====================================================================
