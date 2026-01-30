The ultimate goal of this project is to restructure ByteFight's webserver into a form that can be reused for all competitions in the foreseeable future, and to create a foundation that can be consistently extended to meet evolving needs. The idea is that we will never need to change the fundamentals of the codebase as much as we are today.
##  Summary

- Multiple competition support. This is the big one. The current platform can only hold information about a single competition, and this limitation traces back to the database schema itself. Therefore, we need to restructure the database completely to define the notion of a 'competition' entity and the concept of a many-to-many relationship between competitions and players
- Generalized ladders. It's not unreasonable that in the future we'll want dynamic control over the different 'ladders' in a specific competition. For example, a system might exist where teams are able to participate in Div1/Div2 ladders depending on their proficiency. We'll also want to create a separate ladder for competitors joining a competition after its original lifecycle ends. Therefore, a competition should support as many independent ladders as we want, each with its own glicko configuration. We'll keep `scrimmage` and `ranking`ladders with this new system.
- RabbitMQ exchange. We want to leverage the features of RabbitMQ to create a singular object that can support publisher/consumer functionality over multiple competitions and multiple ladders. The configuration of this object should be owned by the webserver so that no external configuration is necessary when a new competition/ladder is created. To do this, we will use SupportRabbitMQ exchanges with routing keys
- Game result storage. Storing large json blobs in the database causes it to blow up in size quickly. In the future, we might even want to store other outputs from the competition, such as stdout logs or additional information about a game. Given the variety of information that can be relevant to the result of a game, we should store these artifacts in files on the webserver's host filesystem rather than as rows in the database. We need permissions control on these artifacts, for example we'd want game results to be viewable to everyone but maybe stdout of a bot is viewable only by the team that owns the bot.
- Flyway database migration. Our database will only grow in size and complexity. We need a way to manage all of that. Flyway is a good choice I think.

The above are refactors to existing processes. They're the most critical as development on the new platform is blocked until those tasks are complete. Once those tasks are done, we can add the below features as extensions/simple modifications

- Whitelists. Simple but important. Some competitions should be limited to only certain students. We should note that at the time of registering the whitelist, not all members of the whitelist may have accounts in our system.
- Player profiles. People should be able to add personality to their profile! Profile pictures, biographies, achievements, etc. We want people to be able to show off their hard-earned achievements on our platform.
- Tournaments. We need to support generation of tournament brackets and queuing/execution of the games in the bracket. Ideally we won't rely on an external service for this.

The name of the game is making our platform future-proof, or in other words having sufficiently abstract features that allows for a diverse competition configuration.
## Description of changes
### Multiple Competition Support [Complete]
- A new Competition entity is added. Each competition is identified by a unique slug (all lowercase alphanumeric and recognizable identifier).
- Team entity is extended to have a reference to the competition.
- TeamMember entity is added to create a join table that allows for a many-to-many relationship between players and teams
- Competition specific endpoints are defined under a new controller, and competition-specific team operations are moved under this controller (e.g. create team, join team, leave team, any operation that you might want to do for only a single team in a specific competition)
### Generalized Ladders [In progress]
- A new TeamStat entity is added. Contains information about the team's current stats *on a particular ladder* (e.g. glicko, volatility, W/D/L)
- A new TeamGlickoHistory entity is added. Represents an event in time where a team's glicko changed on a particular ladder.
- A new Ladder entity is added. Composite primary key on competition_id and slug (slug will take on values like `scrimmage`, `ranked`, etc.)
### RabbitMQ Exchange [Complete]
- The `match.schedule` exchange is used for the webserver to queue matches for workers. Routing keys on this exchange take on a form of `competition.{competition_slug}.{ladder_slug}`. It'll be up to the engines to subscribe to the proper routes.
- A new `match.updates` *queue* is added. This queue is for publishing lightweight updates such as when a match moves from the queue to being played out on an engine
- The `match.results` queue (refactored name) is still used to report back match results to the webserver.
### Game Result Storage [Complete]
- A new GameMatchFile entity is created. Contains the `gameMatch` as well as a slug and an optional team (owner). References existing file storage system in the form of foreign key to FileRecord
- `GameMatchFileController` provides an upload endpoint in the form of `/api/v1/game-match-file/` (admin only) and a download endpoint in the form of `/api/v1/game-match-file/{matchUuid}/{slug}` (public endpoint).
- (slug, gameMatch, team) is unique. If you request a result owned by a team through the public endpoint, you'll get your team's result if you're on one of the two teams, or forbidden. We'll want another admin endpoint that actually lets you specify the team.
### Flyway [In Progress]
- Migration files under `resources/db/migration` in the form of SQL files. The update flow should always go like this: update schema in dbdiagram.io -> generate new SQL -> add to webserver repo as migration. We want dbdiagram.io to be our ground truth for database schema.
- Set ddl-auto=validate instead of update