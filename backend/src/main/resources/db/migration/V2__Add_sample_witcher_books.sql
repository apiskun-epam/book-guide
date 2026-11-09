-- Add the first 3 Witcher books to the demo user's collection
INSERT INTO collection (user_id, title, author, isbn, cover_url, publish_year, description, rating)
VALUES
    ('demo-user', 'The Last Wish', 'Andrzej Sapkowski', '9780575082441',
     'https://covers.openlibrary.org/b/id/8747763-L.jpg', 1993,
     'Geralt of Rivia is a Witcher, a man whose magic powers and lifelong training have made him a brilliant fighter and a merciless assassin. Yet he is no ordinary killer: he hunts the vile fiends that ravage the land and attack the innocent.',
     5),

    ('demo-user', 'Sword of Destiny', 'Andrzej Sapkowski', '9780316389709',
     'https://covers.openlibrary.org/b/id/12848709-L.jpg', 1992,
     'Geralt is a Witcher, a man whose magic powers, enhanced by long training and a mysterious elixir, have made him a brilliant fighter and a merciless assassin. Yet he is no ordinary murderer: his targets are the multifarious monsters and vile fiends that ravage the land and attack the innocent.',
     4),

    ('demo-user', 'Blood of Elves', 'Andrzej Sapkowski', '9780316029181',
     'https://covers.openlibrary.org/b/id/7374615-L.jpg', 1994,
     'For over a century, humans, dwarves, gnomes, and elves have lived together in relative peace. But times have changed, the uneasy peace is over, and now the races are fighting once again. The only good elf, it seems, is a dead elf.',
     5);
