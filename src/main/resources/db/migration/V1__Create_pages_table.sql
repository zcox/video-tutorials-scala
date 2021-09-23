create table pages(
    page_name text primary key,
    page_data jsonb not null default '{}'::jsonb
);
