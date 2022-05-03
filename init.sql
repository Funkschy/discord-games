DROP TABLE IF EXISTS player;
DROP TABLE IF EXISTS running_game;
DROP TABLE IF EXISTS co_player;

CREATE TABLE IF NOT EXISTS player (
    id   INTEGER(8) PRIMARY KEY,
    nick CHAR(255)
);

CREATE TABLE IF NOT EXISTS running_game (
    game_tag   CHAR(32),
    channel_id INTEGER(8),
    player_one INTEGER(8),

    game_state TEXT,
    message_id INTEGER(8),

    FOREIGN KEY (player_one) REFERENCES player(id),
    PRIMARY KEY (game_tag, channel_id, player_one)
);


CREATE TABLE IF NOT EXISTS co_player (
    game_tag   INTEGER(8),
    channel_id INTEGER(8),
    player_one INTEGER(8),
    player     INTEGER(8),

    FOREIGN KEY (player) REFERENCES player(id),
    FOREIGN KEY (game_tag, channel_id, player_one) REFERENCES running_game(game_tag, channel_id, player_one ),
    PRIMARY KEY (game_tag, channel_id, player_one, player)
);
