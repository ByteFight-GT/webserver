# ByteFight Webserver
This repository contains the code for the backend webserver of ByteFight. The responsibility of the webserver is to maintain the authoritative state of ByteFight's platform. Specifically, the webserver manages team/player data, bot code submissions, match scheduling and results, tournament bracket progression, and team ratings using the Glicko-2 system.

![System Diagram](docs/assets/diagram.svg)

## Project Structure
The webserver is organized into decoupled modules, each handling a specific aspect of the platform. Below are a few of the most important ones:
- `auth/` - Authentication and user management
- `user/` - User account management
- `team/` - Team management
- `player/` - Player profiles and stats
- `competition/` - Competition management
- `tournament/` - Tournament bracket creation and scheduling
- `ladder/` - Ladder and ranking system
- `leaderboard/` - Leaderboard generation and ranking
- `gamematch/` - Match scheduling and coordination
- `matchmaking/` - Matchmaking algorithm
- `submission/` - Bot submission handling
- `storage/` - File storage service
- `glicko/` - Glicko-2 rating system

## Configuration

Coming soon...

## Maintainer

The current primary maintainer of this repository is:
- Jaeheon Shim (jaeheon.shim@gatech.edu)

Please address all concerns related to security or competition integrity directly to the ByteFight developers or to the maintainer. Do NOT create a public issue on this repository.

## License