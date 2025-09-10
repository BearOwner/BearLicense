-- Feature flags for global Owner-controlled toggles and per-user controls table

-- 1) Global feature flags
create table if not exists public.feature_flags (
  flag_key text primary key,
  enabled boolean not null default true,
  audience text not null default 'ALL' check (audience in ('ALL','ADMINISTRATOR','RESELLER')),
  updated_at timestamptz not null default now()
);

alter table public.feature_flags enable row level security;

create policy if not exists "ff_select_all"
  on public.feature_flags for select to authenticated
  using (true);

create policy if not exists "ff_write_owner"
  on public.feature_flags for all to authenticated
  using (
    exists (
      select 1 from public.profiles p
      where p.id = auth.uid() and p.role = 'OWNER'
    )
  );

-- 2) Per-user capability switches
create table if not exists public.user_controls (
  user_id uuid primary key references auth.users(id) on delete cascade,
  can_login boolean not null default true,
  can_create_keys boolean not null default true,
  can_reset_keys boolean not null default true,
  can_add_balance boolean not null default true,
  updated_at timestamptz not null default now()
);

alter table public.user_controls enable row level security;

create policy if not exists "uc_select_self_or_owner"
  on public.user_controls for select to authenticated
  using (
    user_id = auth.uid() or exists (
      select 1 from public.profiles p where p.id = auth.uid() and p.role = 'OWNER'
    )
  );

create policy if not exists "uc_write_owner"
  on public.user_controls for all to authenticated
  using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'OWNER')
  );

-- 3) Optional: profiles.status column to hard-disable a user
alter table public.profiles
  add column if not exists status boolean not null default true;
