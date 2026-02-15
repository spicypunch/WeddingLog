-- WeddingLedger: Money feature tables (categories, budget_items, expenses)
-- Apply in Supabase SQL editor.

-- Optional helper: updated_at trigger function (safe if already exists).
create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

-- 1) categories
create table if not exists public.categories (
  id uuid not null default gen_random_uuid (),
  wedding_id uuid not null,
  name text not null,
  is_default boolean not null default false,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint categories_pkey primary key (id)
);

create trigger trg_categories_updated_at
before update on public.categories
for each row execute function public.set_updated_at();

-- 2) budget_items
create table if not exists public.budget_items (
  id uuid not null default gen_random_uuid (),
  wedding_id uuid not null,
  category_id uuid not null references public.categories (id) on delete cascade,
  name text not null,
  target_amount int not null default 0,
  memo text null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint budget_items_pkey primary key (id)
);

create trigger trg_budget_items_updated_at
before update on public.budget_items
for each row execute function public.set_updated_at();

-- 3) expenses
create table if not exists public.expenses (
  id uuid not null default gen_random_uuid (),
  wedding_id uuid not null,
  budget_item_id uuid not null references public.budget_items (id) on delete cascade,
  amount int not null,
  date date not null,
  payment_method text not null, -- CARD | CASH
  payer text not null,          -- GROOM | BRIDE
  memo text null,
  image_url text null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint expenses_pkey primary key (id)
);

create trigger trg_expenses_updated_at
before update on public.expenses
for each row execute function public.set_updated_at();

-- Recommended: enable RLS and policies (couple share via profiles.wedding_id)
-- NOTE: adjust if you already have custom policies.
alter table public.categories enable row level security;
alter table public.budget_items enable row level security;
alter table public.expenses enable row level security;

create policy "categories_select" on public.categories
for select
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = categories.wedding_id));

create policy "categories_insert" on public.categories
for insert
with check (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = categories.wedding_id));

create policy "categories_update" on public.categories
for update
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = categories.wedding_id))
with check (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = categories.wedding_id));

create policy "categories_delete" on public.categories
for delete
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = categories.wedding_id));

create policy "budget_items_select" on public.budget_items
for select
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = budget_items.wedding_id));

create policy "budget_items_insert" on public.budget_items
for insert
with check (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = budget_items.wedding_id));

create policy "budget_items_update" on public.budget_items
for update
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = budget_items.wedding_id))
with check (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = budget_items.wedding_id));

create policy "budget_items_delete" on public.budget_items
for delete
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = budget_items.wedding_id));

create policy "expenses_select" on public.expenses
for select
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = expenses.wedding_id));

create policy "expenses_insert" on public.expenses
for insert
with check (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = expenses.wedding_id));

create policy "expenses_update" on public.expenses
for update
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = expenses.wedding_id))
with check (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = expenses.wedding_id));

create policy "expenses_delete" on public.expenses
for delete
using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.wedding_id = expenses.wedding_id));

