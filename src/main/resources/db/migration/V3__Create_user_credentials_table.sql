create table user_credentials(
    user_id uuid primary key,
    email text not null,
    password_hash text not null
);

create index user_credentials_email_idx on user_credentials (email);
