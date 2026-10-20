-- =====================================================================
-- CampusConnect RLS tests.
-- Paste into the Supabase SQL editor (or run with psql) AFTER the
-- migration. Everything runs inside a transaction and is rolled back,
-- so no data is left behind. The final table shows PASS/FAIL per check.
-- =====================================================================
begin;

create schema tests;
grant usage on schema tests to authenticated;

create table tests.results (
  at     timestamptz not null default clock_timestamp(),
  passed boolean not null,
  label  text not null,
  detail text
);
grant all on tests.results to authenticated;

-- Act as a signed-in user (JWT claims + authenticated role).
create function tests.as_user(p_uid uuid) returns void language plpgsql as $$
begin
  perform set_config('request.jwt.claims',
    json_build_object('sub', p_uid, 'role', 'authenticated')::text, true);
  perform set_config('request.jwt.claim.sub', p_uid::text, true);
  execute 'set local role authenticated';
end $$;

-- Back to the superuser session.
create function tests.as_superuser() returns void language plpgsql as $$
begin
  execute 'reset role';
  perform set_config('request.jwt.claims', '', true);
  perform set_config('request.jwt.claim.sub', '', true);
end $$;

-- The statement must FAIL (error raised).
create function tests.expect_error(p_sql text, p_label text) returns void language plpgsql as $$
begin
  begin
    execute p_sql;
  exception when others then
    insert into tests.results (passed, label, detail) values (true, p_label, 'blocked: ' || sqlerrm);
    return;
  end;
  insert into tests.results (passed, label, detail) values (false, p_label, 'was allowed but should be blocked');
end $$;

-- The query must return exactly this count.
create function tests.expect_count(p_sql text, p_expected bigint, p_label text) returns void language plpgsql as $$
declare v bigint;
begin
  execute p_sql into v;
  insert into tests.results (passed, label, detail)
  values (v is not distinct from p_expected, p_label, 'expected ' || p_expected || ', got ' || coalesce(v::text, 'null'));
exception when others then
  insert into tests.results (passed, label, detail) values (false, p_label, 'error: ' || sqlerrm);
end $$;

-- ---------------------------------------------------------------------
-- Seed (synthetic data only)
-- ---------------------------------------------------------------------
insert into auth.users (id, email, raw_user_meta_data) values
  ('00000000-0000-0000-0000-000000000001', 'admin@test.local',   '{"full_name":"Test Admin"}'),
  ('00000000-0000-0000-0000-000000000002', 'lec.a@test.local',   '{"full_name":"Lecturer A"}'),
  ('00000000-0000-0000-0000-000000000003', 'lec.b@test.local',   '{"full_name":"Lecturer B"}'),
  ('00000000-0000-0000-0000-000000000004', 'stu.a@test.local',   '{"full_name":"Student A"}'),
  ('00000000-0000-0000-0000-000000000005', 'stu.b@test.local',   '{"full_name":"Student B"}'),
  ('00000000-0000-0000-0000-000000000006', 'pending@test.local', '{"full_name":"Pending User","role":"admin"}');

-- Superuser session: auth.uid() is null, so the protection trigger allows this.
update public.profiles set role = 'admin',    status = 'active' where id = '00000000-0000-0000-0000-000000000001';
update public.profiles set role = 'lecturer', status = 'active' where id in ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003');
update public.profiles set role = 'student',  status = 'active' where id in ('00000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000005');

insert into public.programmes (id, code, name) values ('00000000-0000-0000-0000-000000000f01', 'BSC-IT', 'BSc Information Technology');
insert into public.academic_terms (id, name, start_date, end_date, is_current)
  values ('00000000-0000-0000-0000-000000000e01', '2026/27 Semester 1', '2026-10-01', '2027-02-28', true);
insert into public.courses (id, code, title, programme_id) values
  ('00000000-0000-0000-0000-000000000c01', 'IT101', 'Intro to Computing',  '00000000-0000-0000-0000-000000000f01'),
  ('00000000-0000-0000-0000-000000000c02', 'IT102', 'Databases',           '00000000-0000-0000-0000-000000000f01');
insert into public.course_offerings (id, course_id, term_id, lecturer_id) values
  ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000c01', '00000000-0000-0000-0000-000000000e01', '00000000-0000-0000-0000-000000000002'),
  ('00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000c02', '00000000-0000-0000-0000-000000000e01', '00000000-0000-0000-0000-000000000003');
insert into public.enrollments (offering_id, student_id) values
  ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000004'),
  ('00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000005');
insert into public.class_sessions (id, offering_id, starts_at, ends_at, venue, created_by) values
  ('00000000-0000-0000-0000-000000000501', '00000000-0000-0000-0000-0000000000a1', now() + interval '1 day', now() + interval '1 day 2 hours', 'Room A1', '00000000-0000-0000-0000-000000000002'),
  ('00000000-0000-0000-0000-000000000502', '00000000-0000-0000-0000-0000000000b1', now() + interval '2 day', now() + interval '2 day 2 hours', 'Room B1', '00000000-0000-0000-0000-000000000003');

-- ---------------------------------------------------------------------
-- Tests
-- ---------------------------------------------------------------------
do $$
declare
  admin_id uuid := '00000000-0000-0000-0000-000000000001';
  lec_a    uuid := '00000000-0000-0000-0000-000000000002';
  stu_a    uuid := '00000000-0000-0000-0000-000000000004';
  stu_b    uuid := '00000000-0000-0000-0000-000000000005';
  pend     uuid := '00000000-0000-0000-0000-000000000006';
  off_a    text := '00000000-0000-0000-0000-0000000000a1';
  off_b    text := '00000000-0000-0000-0000-0000000000b1';
begin
  -- Signup safety: metadata role must be ignored.
  perform tests.expect_count($q$select count(*) from public.profiles where id = '00000000-0000-0000-0000-000000000006' and role = 'student' and status = 'pending'$q$, 1, 'Signup ignores client-supplied role and starts pending');

  ---------------- pending user ----------------
  perform tests.as_user(pend);
  perform tests.expect_count('select count(*) from public.courses', 0, 'Pending user cannot read courses');
  perform tests.expect_count('select count(*) from public.class_sessions', 0, 'Pending user cannot read timetable');
  perform tests.expect_count('select count(*) from public.profiles', 1, 'Pending user sees only own profile');

  ---------------- student A (enrolled in A) ----------------
  perform tests.as_user(stu_a);
  perform tests.expect_count('select count(*) from public.courses', 2, 'Active student can read course catalogue');
  perform tests.expect_count('select count(*) from public.course_offerings', 1, 'Student sees only enrolled offering');
  perform tests.expect_count('select count(*) from public.class_sessions', 1, 'Student sees only own course sessions');
  perform tests.expect_count('select count(*) from public.profiles', 4, 'Student sees only self plus active staff profiles');
  perform tests.expect_count($q$select count(*) from public.profiles where role = 'student'$q$, 1, 'Student cannot read other students');
  perform tests.expect_error($q$update public.profiles set role = 'admin' where id = '00000000-0000-0000-0000-000000000004'$q$, 'Student cannot promote self to admin');
  perform tests.expect_count($q$with u as (update public.profiles set status = 'active', role = 'lecturer' where id = '00000000-0000-0000-0000-000000000006' returning 1) select count(*) from u$q$, 0, 'Student cannot approve another account');
  perform tests.expect_error($q$insert into public.announcements (author_id, audience, offering_id, title, body, status) values ('00000000-0000-0000-0000-000000000004','offering','00000000-0000-0000-0000-0000000000a1','Fake','Fake','published')$q$, 'Student cannot publish announcements');
  perform tests.expect_count($q$with u as (update public.class_sessions set venue = 'Hacked' returning 1) select count(*) from u$q$, 0, 'Student cannot edit timetable');
  perform tests.expect_count('select count(*) from public.audit_logs', 0, 'Student cannot read audit logs');

  ---------------- lecturer A ----------------
  perform tests.as_user(lec_a);
  perform tests.expect_count(format($q$with i as (insert into public.announcements (author_id, audience, offering_id, title, body, status) values (%L,'offering',%L,'Welcome','First lecture tomorrow','published') returning 1) select count(*) from i$q$, lec_a, off_a), 1, 'Lecturer can publish to own offering');
  perform tests.expect_count(format($q$with i as (insert into public.announcements (author_id, audience, offering_id, title, body, status) values (%L,'offering',%L,'Draft note','Not ready','draft') returning 1) select count(*) from i$q$, lec_a, off_a), 1, 'Lecturer can save a draft');
  perform tests.expect_error(format($q$insert into public.announcements (author_id, audience, offering_id, title, body, status) values (%L,'offering',%L,'Intruder','Not my course','published')$q$, lec_a, off_b), 'Lecturer cannot post to another lecturer''s offering');
  perform tests.expect_error(format($q$insert into public.announcements (author_id, audience, title, body, status) values (%L,'campus','Campus wide','Only admins','published')$q$, lec_a), 'Lecturer cannot post campus-wide');
  perform tests.expect_count($q$with u as (update public.class_sessions set venue = 'Room A2' where id = '00000000-0000-0000-0000-000000000501' returning 1) select count(*) from u$q$, 1, 'Lecturer can change venue of own session');
  perform tests.expect_count($q$with u as (update public.class_sessions set venue = 'Hacked' where id = '00000000-0000-0000-0000-000000000502' returning 1) select count(*) from u$q$, 0, 'Lecturer cannot edit another course''s session');
  perform tests.expect_count(format($q$with u as (update public.course_offerings set lecturer_id = %L where id = %L returning 1) select count(*) from u$q$, lec_a, off_b), 0, 'Lecturer cannot reassign courses');

  ---------------- students read results ----------------
  perform tests.as_user(stu_a);
  perform tests.expect_count('select count(*) from public.announcements', 1, 'Enrolled student sees published announcement but not the draft');
  perform tests.expect_count($q$select count(*) from public.notifications where kind = 'announcement'$q$, 1, 'Enrolled student got announcement notification');
  perform tests.expect_count($q$select count(*) from public.notifications where kind = 'timetable'$q$, 1, 'Enrolled student got timetable-change notification');
  perform tests.expect_count($q$select count(*) from public.class_sessions where id = '00000000-0000-0000-0000-000000000501' and status = 'changed'$q$, 1, 'Venue change marks session as changed');
  perform tests.expect_count($q$with u as (update public.notifications set read_at = now() returning 1) select count(*) from u$q$, 2, 'Student can mark own notifications read');
  perform tests.expect_error($q$update public.notifications set title = 'Edited'$q$, 'Student cannot edit notification content');

  perform tests.as_user(stu_b);
  perform tests.expect_count('select count(*) from public.announcements', 0, 'Non-enrolled student cannot see the announcement');
  perform tests.expect_count('select count(*) from public.notifications', 0, 'Non-enrolled student receives no notification');

  ---------------- admin ----------------
  perform tests.as_user(admin_id);
  perform tests.expect_count($q$with u as (update public.profiles set status = 'active' where id = '00000000-0000-0000-0000-000000000006' returning 1) select count(*) from u$q$, 1, 'Admin can approve a pending account');
  perform tests.expect_count($q$select count(*) from public.audit_logs where entity_type = 'profiles' and action = 'UPDATE' and actor_id = '00000000-0000-0000-0000-000000000001'$q$, 1, 'Approval is recorded in the audit log');
  perform tests.expect_count('select count(*) from public.profiles', 6, 'Admin can read all profiles');

  perform tests.as_superuser();
end $$;


-- ---------------------------------------------------------------------
-- Tests: deadlines, materials, storage, reminders, push tokens
-- ---------------------------------------------------------------------
do $$
declare
  lec_a uuid := '00000000-0000-0000-0000-000000000002';
  admin_id uuid := '00000000-0000-0000-0000-000000000001';
  stu_a uuid := '00000000-0000-0000-0000-000000000004';
  stu_b uuid := '00000000-0000-0000-0000-000000000005';
  pend  uuid := '00000000-0000-0000-0000-000000000006';
  off_a text := '00000000-0000-0000-0000-0000000000a1';
  off_b text := '00000000-0000-0000-0000-0000000000b1';
begin
  ---------------- deadlines ----------------
  perform tests.as_user(lec_a);
  perform tests.expect_count(format($q$with i as (insert into public.assignments (offering_id, created_by, title, due_at, status) values (%L, %L, 'Essay 1', now() + interval '12 hours', 'published') returning 1) select count(*) from i$q$, off_a, lec_a), 1, 'Lecturer can publish a deadline for own course');
  perform tests.expect_count(format($q$with i as (insert into public.assignments (offering_id, created_by, title, due_at, status) values (%L, %L, 'Hidden draft', now() + interval '3 days', 'draft') returning 1) select count(*) from i$q$, off_a, lec_a), 1, 'Lecturer can save a draft deadline');
  perform tests.expect_error(format($q$insert into public.assignments (offering_id, created_by, title, due_at, status) values (%L, %L, 'Not mine', now() + interval '1 day', 'published')$q$, off_b, lec_a), 'Lecturer cannot add a deadline to another course');

  perform tests.as_user(stu_a);
  perform tests.expect_count('select count(*) from public.assignments', 1, 'Enrolled student sees published deadline only');
  perform tests.expect_count($q$select count(*) from public.notifications where kind = 'deadline'$q$, 1, 'Student notified of new deadline');
  perform tests.expect_error(format($q$insert into public.assignments (offering_id, created_by, title, due_at, status) values (%L, %L, 'Mine', now() + interval '1 day', 'published')$q$, off_a, stu_a), 'Student cannot create deadlines');
  perform tests.expect_count($q$with u as (update public.assignments set title = 'Hacked' returning 1) select count(*) from u$q$, 0, 'Student cannot edit deadlines');
  perform tests.expect_error('select public.send_deadline_reminders()', 'Students cannot run the reminder job');

  perform tests.as_user(stu_b);
  perform tests.expect_count('select count(*) from public.assignments', 0, 'Non-enrolled student sees no deadlines');

  ---------------- reminders (run as the job owner) ----------------
  perform tests.as_superuser();
  perform tests.expect_count('select public.send_deadline_reminders()', 1, 'Reminder job notifies enrolled student once');
  perform tests.expect_count('select public.send_deadline_reminders()', 0, 'Reminder job does not repeat');
  perform tests.as_user(stu_a);
  perform tests.expect_count($q$select count(*) from public.notifications where kind = 'deadline' and title = 'Deadline reminder'$q$, 1, 'Student received deadline reminder');

  ---------------- materials ----------------
  perform tests.as_user(lec_a);
  perform tests.expect_count(format($q$with i as (insert into public.materials (offering_id, uploader_id, title, storage_path, size_bytes) values (%L, %L, 'Lecture 1', %L, 1000) returning 1) select count(*) from i$q$, off_a, lec_a, off_a || '/lec1.pdf'), 1, 'Lecturer can add material to own course');
  perform tests.expect_error(format($q$insert into public.materials (offering_id, uploader_id, title, storage_path, size_bytes) values (%L, %L, 'Intruder', %L, 1000)$q$, off_b, lec_a, off_b || '/x.pdf'), 'Lecturer cannot add material to another course');
  perform tests.expect_error(format($q$insert into public.materials (offering_id, uploader_id, title, storage_path, size_bytes) values (%L, %L, 'Too big', %L, 99999999)$q$, off_a, lec_a, off_a || '/big.pdf'), 'Material over 10 MB is rejected');
  perform tests.expect_count(format($q$with i as (insert into storage.objects (bucket_id, name) values ('materials', %L) returning 1) select count(*) from i$q$, off_a || '/lec1.pdf'), 1, 'Lecturer can upload file into own course folder');
  perform tests.expect_error(format($q$insert into storage.objects (bucket_id, name) values ('materials', %L)$q$, off_b || '/evil.pdf'), 'Lecturer cannot upload into another course folder');

  perform tests.as_user(admin_id);
  perform tests.expect_count(format($q$with i as (insert into public.materials (offering_id, uploader_id, title, storage_path, status) values (%L, %L, 'Pending paper', %L, 'pending') returning 1) select count(*) from i$q$, off_a, admin_id, off_a || '/pending.pdf'), 1, 'Admin can add pending material');
  perform tests.as_superuser();
  insert into storage.objects (bucket_id, name) values ('materials', off_a || '/pending.pdf');

  perform tests.as_user(stu_a);
  perform tests.expect_count('select count(*) from public.materials', 1, 'Enrolled student sees approved material only');
  perform tests.expect_count($q$select count(*) from public.notifications where kind = 'material'$q$, 1, 'Student notified of new material');
  perform tests.expect_count($q$select count(*) from storage.objects where bucket_id = 'materials'$q$, 1, 'Student can read only approved files in storage');
  perform tests.expect_error(format($q$insert into public.materials (offering_id, uploader_id, title, storage_path) values (%L, %L, 'Student upload', %L)$q$, off_a, stu_a, off_a || '/s.pdf'), 'Student cannot add material');
  perform tests.expect_error(format($q$insert into storage.objects (bucket_id, name) values ('materials', %L)$q$, off_a || '/s.pdf'), 'Student cannot upload files');

  perform tests.as_user(stu_b);
  perform tests.expect_count('select count(*) from public.materials', 0, 'Non-enrolled student sees no materials');
  perform tests.expect_count($q$select count(*) from storage.objects where bucket_id = 'materials'$q$, 0, 'Non-enrolled student cannot read files');

  ---------------- push tokens ----------------
  perform tests.as_user(stu_a);
  perform tests.expect_count($q$select count(*) from (select public.register_device_token('tok-1')) x$q$, 1, 'Active user can register a push token');
  perform tests.expect_count('select count(*) from public.device_tokens', 1, 'User sees own token');
  perform tests.as_user(stu_b);
  perform tests.expect_count($q$select count(*) from (select public.register_device_token('tok-1')) x$q$, 1, 'Token moves to the new account on a shared phone');
  perform tests.expect_count('select count(*) from public.device_tokens', 1, 'New owner sees the token');
  perform tests.as_user(stu_a);
  perform tests.expect_count('select count(*) from public.device_tokens', 0, 'Previous owner no longer has the token');
  perform tests.as_superuser();
  update public.profiles set status = 'pending' where id = pend;  -- earlier test approved this user
  perform tests.as_user(pend);
  perform tests.expect_error($q$select public.register_device_token('tok-2')$q$, 'Pending account cannot register a push token');

  perform tests.as_superuser();
end $$;

-- Review: every row should be passed = true.
select passed, label, detail from tests.results order by at;

rollback;
