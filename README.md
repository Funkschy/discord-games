# discord-games

A discord bot to play interactive games.
It currently supports the following games:
 - Wordle
 - 2048

## Usage

To use the bot yourself, you'll need to set a channel for the bot to send its images to.
This is defined by the `:img-channel` value in the config.edn

The bot expects your discord oauth token as an environment variable with the name `DISCORD_GAMES_TOKEN`, so if your token is `asdf` you can just run the bot with `DISCORD_GAMES_TOKEN="asdf" lein run`

The prefix for commands can be changed in the config.edn, it's `$` by default

## Commands

The bot currently has the following commands:
 - $2048 will start a new 2048 game
 - $wordle \<guess\> will start a new wordle game
 - $quit \<game\> will quit your current session for \<game\>

## License

Copyright © 2022 Felix Schoeller

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
