-- =====================================================================
-- CampusConnect migration 3: deadline + material notifications,
-- deadline reminders (pg_cron), and push-token RPCs for FCM.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Notification links
-- ---------------------------------------------------------------------
alter table public.notifications
  add column if not exists assignment_id uuid references public.assignments (id) on delete cascade,
  add column if not exists material_id   uuid references public.materials (id)   on delete cascade;

alter table public.assignments
  add column if not exists reminder_sent_at timestamptz;

create index if not exists assignments_due_idx on public.assignments (due_at) where status = 'published';

-- Moving a deadline re-arms its reminder.
create or replace function public.assignment_before_update()
returns trigger
language plpgsql
as $$
begin
  if new.due_at <> old.due_at then
    new.reminder_sent_at := null;
  end if;
  return new;
end $$;

create trigger assignments_rearm
  before update on public.assignments
  for each row execute function public.assignment_before_update();

-- ---------------------------------------------------------------------
-- 2. Fan-out: new deadline
-- ---------------------------------------------------------------------
create or replace function public.fanout_assignment()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  if new.status = 'published'
     and (tg_op = 'INSERT' or old.status is distinct from 'published') then
    insert into public.notifications (recipient_id, kind, assignment_id, title, body)
    select e.student_id,
           'deadline',
           new.id,
           'New deadline',
           c.code || ': ' || new.title || ' | due '
             || to_char(new.due_at at time zone 'Africa/Dar_es_Salaam', 'DD Mon HH24:MI')
    from public.enrollments e
    join public.profiles p on p.id = e.student_id and p.status = 'active'
    join public.course_offerings o on o.id = new.offering_id
    join public.courses c on c.id = o.course_id
    where e.offering_id = new.offering_id and e.status = 'active';
  end if;
  return null;
end $$;

create trigger assignments_fanout
  after insert or update on public.assignments
  for each row execute function public.fanout_assignment();

-- ---------------------------------------------------------------------
-- 3. Fan-out: new approved study material
-- ---------------------------------------------------------------------
create or replace function public.fanout_material()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  if new.status = 'approved'
     and (tg_op = 'INSERT' or old.status is distinct from 'approved') then
    insert into public.notifications (recipient_id, kind, material_id, title, body)
    select e.student_id,
           'material',
           new.id,
           'New study material',
           c.code || ': ' || new.title
    from public.enrollments e
    join public.profiles p on p.id = e.student_id and p.status = 'active'
    join public.course_offerings o on o.id = new.offering_id
    join public.courses c on c.id = o.course_id
    where e.offering_id = new.offering_id and e.status = 'active';
  end if;
  return null;
end $$;

create trigger materials_fanout
  after insert or update on public.materials
  for each row execute function public.fanout_material();

-- ---------------------------------------------------------------------
-- 4. Deadline reminders: one reminder per assignment, 24h before it is due.
--    Called by pg_cron (below). Not callable by app users.
-- ---------------------------------------------------------------------
create or replace function public.send_deadline_reminders()
returns integer
language plpgsql security definer set search_path = public
as $$
declare
  n integer;
begin
  with due as (
    update public.assignments a
       set reminder_sent_at = now()
     where a.status = 'published'
       and a.reminder_sent_at is null
       and a.due_at > now()
       and a.due_at <= now() + interval '24 hours'
    returning a.id, a.offering_id, a.title, a.due_at
  ), ins as (
    insert into public.notifications (recipient_id, kind, assignment_id, title, body)
    select e.student_id,
           'deadline',
           d.id,
           'Deadline reminder',
           c.code || ': ' || d.title || ' | due '
             || to_char(d.due_at at time zone 'Africa/Dar_es_Salaam', 'DD Mon HH24:MI')
    from due d
    join public.enrollments e on e.offering_id = d.offering_id and e.status = 'active'
    join public.profiles p on p.id = e.student_id and p.status = 'active'
    join public.course_offerings o on o.id = d.offering_id
    join public.courses c on c.id = o.course_id
    returning 1
  )
  select count(*) into n from ins;
  return n;
end $$;

revoke execute on function public.send_deadline_reminders() from public, anon, authenticated;

-- Schedule every 15 minutes if pg_cron is available (enable it in
-- Dashboard > Database > Extensions on Supabase). Skipped safely otherwise.
do $$
begin
  create extension if not exists pg_cron;
  perform cron.schedule('deadline-reminders', '*/15 * * * *', 'select public.send_deadline_reminders()');
exception when others then
  raise notice 'pg_cron not available; schedule public.send_deadline_reminders() manually. (%)', sqlerrm;
end $$;

-- ---------------------------------------------------------------------
-- 5. Push tokens: register/unregister through RPC so a token can move
--    cleanly between accounts on a shared phone (RLS alone would block it).
-- ---------------------------------------------------------------------
create or replace function public.register_device_token(p_token text)
returns void
language plpgsql security definer set search_path = public
as $$
begin
  if auth.uid() is null or public.app_role() is null then
    raise exception 'Not allowed' using errcode = '42501';
  end if;
  insert into public.device_tokens (user_id, fcm_token)
  values (auth.uid(), p_token)
  on conflict (fcm_token) do update set user_id = auth.uid(), updated_at = now();
end $$;

create or replace function public.unregister_device_token(p_token text)
returns void
language plpgsql security definer set search_path = public
as $$
begin
  delete from public.device_tokens where fcm_token = p_token and user_id = auth.uid();
end $$;

revoke execute on function public.register_device_token(text)   from public, anon;
revoke execute on function public.unregister_device_token(text) from public, anon;
grant  execute on function public.register_device_token(text)   to authenticated;
grant  execute on function public.unregister_device_token(text) to authenticated;
