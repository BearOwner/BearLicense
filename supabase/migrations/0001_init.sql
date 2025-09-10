-- Supabase initial schema for BearLicense
-- Run with supabase CLI migrations or paste in Studio SQL editor.

-- Extensions
create extension if not exists "pgcrypto";

-- Profiles (Supabase Auth -> app fields)
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  fullname text,
  username text unique,
  email text,
  reset_link_token text,
  exp_date timestamptz,
  level integer,
  saldo integer,
  status boolean not null default true,
  uplink text,
  user_ip text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  expiration_date timestamptz
);

alter table public.profiles enable row level security;

create policy if not exists "profiles_select_own"
  on public.profiles for select to authenticated
  using (id = auth.uid());

create policy if not exists "profiles_update_own"
  on public.profiles for update to authenticated
  using (id = auth.uid());

create policy if not exists "profiles_insert_self"
  on public.profiles for insert to authenticated
  with check (id = auth.uid());

-- Keys (licenses)
create table if not exists public.keys_code (
  id_keys uuid primary key default gen_random_uuid(),
  game text not null,
  user_key text unique not null,
  duration integer not null,
  expired_date timestamptz,
  max_devices integer not null default 1,
  devices integer not null default 0,
  status text not null default 'active',
  registrator uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.keys_code enable row level security;

create policy if not exists "keys_code_select_all"
  on public.keys_code for select to authenticated
  using (true);

create policy if not exists "keys_code_insert_owner_admin"
  on public.keys_code for insert to authenticated
  with check (
    exists (
      select 1 from public.profiles p
      where p.id = auth.uid() and p.role in ('OWNER','ADMINISTRATOR')
    )
  );

create policy if not exists "keys_code_update_owner_admin"
  on public.keys_code for update to authenticated
  using (
    exists (
      select 1 from public.profiles p
      where p.id = auth.uid() and p.role in ('OWNER','ADMINISTRATOR')
    )
  );

-- Price
create table if not exists public.price (
  id uuid primary key default gen_random_uuid(),
  value numeric not null,
  duration integer not null,
  amount integer not null,
  role text not null check (role in ('OWNER','ADMINISTRATOR','RESELLER'))
);

alter table public.price enable row level security;

create policy if not exists "price_select_all" on public.price
  for select to authenticated using (true);

create policy if not exists "price_write_owner" on public.price
  for all to authenticated using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'OWNER')
  );

-- Referral codes
create table if not exists public.referral_code (
  id_reff uuid primary key default gen_random_uuid(),
  code text unique not null,
  referral text,
  level integer,
  set_saldo numeric,
  used_by uuid references auth.users(id),
  created_by uuid references auth.users(id),
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  acc_expiration timestamptz
);

alter table public.referral_code enable row level security;

create policy if not exists "referral_select_all" on public.referral_code
  for select to authenticated using (true);

create policy if not exists "referral_write_owner_admin" on public.referral_code
  for all to authenticated using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role in ('OWNER','ADMINISTRATOR'))
  );

-- Lib (files)
create table if not exists public.lib (
  id uuid primary key default gen_random_uuid(),
  file text not null,
  file_type text,
  file_size bigint,
  pass text,
  time timestamptz default now()
);

alter table public.lib enable row level security;

create policy if not exists "lib_select_all" on public.lib
  for select to authenticated using (true);

create policy if not exists "lib_write_owner" on public.lib
  for all to authenticated using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'OWNER')
  );

-- Auto update timestamps helper
create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

-- Attach triggers where column exists
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_profiles_updated_at') THEN
    CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_keys_code_updated_at') THEN
    CREATE TRIGGER trg_keys_code_updated_at
    BEFORE UPDATE ON public.keys_code
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_referral_code_updated_at') THEN
    CREATE TRIGGER trg_referral_code_updated_at
    BEFORE UPDATE ON public.referral_code
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
  END IF;
END$$;

-- Create a public storage bucket for lib files (if storage is enabled)
insert into storage.buckets (id, name, public)
values ('lib', 'lib', true)
on conflict (id) do nothing;
