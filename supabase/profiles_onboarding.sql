-- WeddingLog Onboarding: profiles table extensions
--
-- This migration adds the minimum columns used by the onboarding flow:
-- - nickname (uses existing `profiles.name`)
-- - role (GROOM / BRIDE)
-- - wedding date (date or "undecided" flag)
-- - invite code for partner linking
-- - onboarding completion flag

alter table public.profiles
    add column if not exists role text;

alter table public.profiles
    add column if not exists wedding_date date;

alter table public.profiles
    add column if not exists is_wedding_date_undecided boolean not null default false;

alter table public.profiles
    add column if not exists invite_code text;

alter table public.profiles
    add column if not exists onboarding_completed boolean not null default false;

-- Optional but recommended: prevent duplicate invite codes (NULLs are allowed).
create unique index if not exists profiles_invite_code_key
    on public.profiles (invite_code);

