-- Add role column to profiles to support RBAC policies
alter table public.profiles
  add column if not exists role text not null default 'RESELLER' check (role in ('OWNER','ADMINISTRATOR','RESELLER'));

-- Optional: seed owner for local dev (replace the UUID with your auth user id)
-- insert into public.profiles (id, username, role, email)
-- values ('00000000-0000-0000-0000-000000000000', 'owner_local', 'OWNER', 'owner@example.com')
-- on conflict (id) do update set role = excluded.role;
