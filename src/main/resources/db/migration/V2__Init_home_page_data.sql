INSERT INTO
  pages(page_name, page_data)
VALUES
  ('home', '{"lastViewProcessed":0,"videosWatched":0}'::jsonb)
ON CONFLICT DO NOTHING;
