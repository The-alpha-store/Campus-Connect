-- Campus maintenance ("offline mode") and campus details.
-- While a campus is under maintenance its people are paused at the database: they stop being "active",
-- so every read and write that needs an active member is refused, whatever the app does. Only the app
-- manager can switch it on or off. People can still ask whether their campus is paused (so the app can say
-- why nothing works), and can still read their own profile and sign out. Safe to run twice.

alter table public.universities
  add column if not exists maintenance       boolean     not null default false,
  add column if not exists maintenance_note  text        not null default '',
  add column if not exists maintenance_since timestamptz;

-- True while the campus is paused. The reserved app-manager home is never paused.
create or replace function public.campus_paused(p_university uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select coalesce((select u.maintenance and not u.is_platform from public.universities u where u.id = p_university), false)
$$;

-- The role helpers every policy is built on now also require the campus to be open.
create or replace function public.is_active()
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.profiles
                 where id = auth.uid() and status = 'approved' and not public.campus_paused(university_id))
$$;

create or replace function public.is_student()
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.profiles
                 where id = auth.uid() and status = 'approved' and role = 'student' and not public.campus_paused(university_id))
$$;

create or replace function public.is_lecturer()
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.profiles
                 where id = auth.uid() and status = 'approved' and role = 'lecturer' and not public.campus_paused(university_id))
$$;

create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.profiles
                 where id = auth.uid() and status = 'approved' and role = 'admin' and not public.campus_paused(university_id))
     and (public.setting_text('require_admin_mfa', 'true') <> 'true'
          or coalesce(auth.jwt() ->> 'aal', '') = 'aal2')
$$;

-- The app manager switches a campus on or off. The note is shown to the campus's people.
create or replace function public.set_campus_maintenance(p_university uuid, p_on boolean, p_note text default '')
returns void language plpgsql security definer set search_path = public as $$
declare v_name text;
begin
  if not public.is_platform_admin() then raise exception 'not_allowed' using errcode = '42501'; end if;
  select name into v_name from public.universities where id = p_university and not is_platform;
  if not found then raise exception 'campus_not_found' using errcode = 'P0001'; end if;

  update public.universities
     set maintenance = p_on,
         maintenance_note = case when p_on then left(coalesce(trim(p_note), ''), 300) else '' end,
         maintenance_since = case when p_on then now() else null end
   where id = p_university;

  perform public.log_audit(case when p_on then 'CAMPUS_MAINTENANCE_ON' else 'CAMPUS_MAINTENANCE_OFF' end, v_name);
  if not p_on then
    -- Told once the campus is open again (nobody could read a message while it was paused).
    perform public.notify_users(
      array(select p.id from public.profiles p where p.university_id = p_university and p.status = 'approved'),
      'CampusConnect is back', v_name || ' is open again. Thank you for your patience.', 'announcement', false);
  end if;
end $$;

-- Anyone signed in can ask about their own campus, even while it is paused.
create or replace function public.my_campus_status()
returns table (paused boolean, note text, since timestamptz)
language sql stable security definer set search_path = public as $$
  select (u.maintenance and not u.is_platform), u.maintenance_note, u.maintenance_since
  from public.universities u where u.id = public.my_university()
$$;

-- For the app manager's campus list: every real campus with its state.
create or replace function public.platform_campus_status()
returns table (university_id uuid, code text, name text, maintenance boolean, note text, since timestamptz)
language plpgsql stable security definer set search_path = public as $$
begin
  if not public.is_platform_admin() then raise exception 'not_allowed' using errcode = '42501'; end if;
  return query select u.id, u.code, u.name, u.maintenance, u.maintenance_note, u.maintenance_since
               from public.universities u where not u.is_platform order by u.code;
end $$;

-- Campus details. A campus administrator edits their own campus; the app manager edits any. The code never changes.
create or replace function public.update_campus_details(p_name text)
returns void language plpgsql security definer set search_path = public as $$
declare v_uni uuid := public.my_university();
begin
  if not public.is_admin() or public.is_platform_admin() then raise exception 'not_allowed' using errcode = '42501'; end if;
  if length(trim(coalesce(p_name, ''))) not between 3 and 120 then raise exception 'name_invalid' using errcode = 'P0001'; end if;
  update public.universities set name = trim(p_name) where id = v_uni and not is_platform;
  perform public.log_audit('CAMPUS_DETAILS_EDITED', trim(p_name));
end $$;

create or replace function public.platform_update_university(p_university uuid, p_name text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if not public.is_platform_admin() then raise exception 'not_allowed' using errcode = '42501'; end if;
  if length(trim(coalesce(p_name, ''))) not between 3 and 120 then raise exception 'name_invalid' using errcode = 'P0001'; end if;
  update public.universities set name = trim(p_name) where id = p_university and not is_platform;
  if not found then raise exception 'campus_not_found' using errcode = 'P0001'; end if;
  perform public.log_audit('CAMPUS_DETAILS_EDITED', trim(p_name));
end $$;

revoke all on function public.campus_paused(uuid), public.set_campus_maintenance(uuid, boolean, text),
  public.my_campus_status(), public.platform_campus_status(), public.update_campus_details(text),
  public.platform_update_university(uuid, text) from public, anon;
grant execute on function public.campus_paused(uuid), public.set_campus_maintenance(uuid, boolean, text),
  public.my_campus_status(), public.platform_campus_status(), public.update_campus_details(text),
  public.platform_update_university(uuid, text) to authenticated;
