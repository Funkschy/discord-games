DROP TABLE IF EXISTS player;
DROP TABLE IF EXISTS running_game;
DROP TABLE IF EXISTS participant;
DROP TABLE IF EXISTS message;

CREATE TABLE IF NOT EXISTS running_game (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    game_tag   CHAR(32),
    game_state TEXT -- EDN blob
);


CREATE TABLE IF NOT EXISTS player (
    id INTEGER(8) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS participant (
    game_id   INTEGER,
    player_id INTEGER(8),

    owner BOOLEAN NOT NULL CHECK (owner IN (0, 1)),

    FOREIGN KEY (game_id) REFERENCES running_game(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE,
    PRIMARY KEY (game_id, player_id)
);

CREATE TABLE IF NOT EXISTS message (
    id         INTEGER(8) PRIMARY KEY,
    channel_id INTEGER(8),
    game_id    INTEGER(8),
    FOREIGN KEY (game_id) REFERENCES running_game(id) ON DELETE CASCADE
);
