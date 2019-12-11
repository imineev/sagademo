# sagademo

0. Create AQ user(s) in the database. This is not done by this demo/repos.


Instructions to use existing images at `docker.io/paulparkinson/travelagency-helidon-se:0.1` and `docker.io/paulparkinson/booking-helidon-se:0.1` ...

1. Pull the image and docker export in order to get the underlying jar or contact Paul Parkinson for the same

2. Start the travel agency service with `java -Doracle.ucp.jdbc.PoolDataSource.travelagency.URL=someURL -Doracle.ucp.jdbc.PoolDataSource.travelagency.user=someUser -Doracle.ucp.jdbc.PoolDataSource.travelagency.password=somePW -jar travelagency-helidon-se/target/travelagency-helidon-se.jar` 

3. Tables and queues can be created via `curl http://localhost:8080/travelagency/setup`

4. Tables and queues can be cleaned via `curl http://localhost:8080/travelagency/clean`

5. Start each booking service with an argument to specify which service it is...
    `java -jar booking-helidon-se/target/booking-helidon-se.jar eventtickets`
    `java -jar booking-helidon-se/target/booking-helidon-se.jar flight`
    `java -jar booking-helidon-se/target/booking-helidon-se.jar hotel`

6. Run a successful booking with `curl http://localhost:8080/travelagency/booktrip`

6. Run a fail/compensating booking with `curl http://localhost:8080/travelagency/booktripfailflightbooking`

7. If deploying to kubernetes, modify travelagency-helidon-se-deployment.yaml and booking-helidon-se-deployment.yaml to point to the correct image location and modify the env to override properties in microprofile-config.properties


Instructions to build and publish image for use...

0. Note that the demo uses a Helidon feature that is currently in a fork of the main branch. Contact Paul Parkinson for instructions

1. Rename src/main/resources/microprofile-config.properties_template to src/main/resources/microprofile-config.properties in both travelagency an booking service.

2. Values in microprofile-config.properties can be override either directly in the properties file or via system or env properties.

3. Only the `url`, `user`, and `password` values need to be set.

4. Run `mvn install` from the base dir or either service dir to build

5. Set/export a value for `DEMOREGISTRY` and run the `build.sh` script in each service dir to build and push the docker image to that ${DEMOREGISTRY} 

