# Development
## Dev Profile
The development profile is defined in `application-dev.yml`. It is filled in with default values for you to avoid having to pass around environment files between developers. Check your IDE for how to run the application with the 'dev' profile.

Note: The dev profile is insecure and should never be used outside of development on your local machine

## Dev Containers
The application interacts with a few external services such as Supabase for auth and RabbitMQ for the message queue. These containers are defined in `docker-compose-dev.yml`. Note that these containers do not have persistent volumes, and if you delete the container all database contents will be lost.