-- Active users can see the names of active staff (for "posted by" on announcements).
create policy profiles_staff_visible on public.profiles for select to authenticated
  using (
    public.app_role() is not null
    and role in ('lecturer', 'admin')
    and status = 'active'
  );
