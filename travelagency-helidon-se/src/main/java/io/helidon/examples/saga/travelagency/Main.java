/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.saga.travelagency;

import io.helidon.config.Config;
import io.helidon.media.jsonb.server.JsonBindingSupport;
import io.helidon.media.jsonp.server.JsonSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;

import java.io.IOException;
import java.util.logging.LogManager;

public final class Main {

    private Main() {
    }

    public static void main(final String[] args) throws Exception {
        boolean isFailtTest = false;
        if(args.length > 0) {
            if (args[0].equals("setup")) {
                new TravelAgencyResourceSetup().createAll();
                return;
            } else if (args[0].equals("clean")) {
                new TravelAgencyResourceSetup().cleanAll();
                return;
            }else if (args[0].equals("fail")) {
                isFailtTest = true;
            }
        }
        TravelAgencyService travelAgencyService = new TravelAgencyService();
        startServer();
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~ saga result:" +travelAgencyService.doBookTrip(isFailtTest));
    }

    public static void main0(final String[] args) throws IOException {
        startServer();
    }

    static WebServer startServer() throws IOException {

        // load logging configuration
        LogManager.getLogManager().readConfiguration(
                Main.class.getResourceAsStream("/logging.properties"));

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        // Get webserver config from the "server" section of application.yaml
        ServerConfiguration serverConfig =
                ServerConfiguration.builder(config.get("server"))
//                        .tracer(TracerBuilder.create(config.get("tracing")).buildAndRegister())
                        .build();

        WebServer server = WebServer.create(serverConfig, createRouting(config));

        // Start the server and print some info.
        server.start().thenAccept(ws -> {
            System.out.println(
                    "WEB server is up! http://localhost:" + ws.port() + "/");
        });

        // Server threads are not daemon. NO need to block. Just react.
        server.whenShutdown().thenRun(() -> System.out.println("WEB server is DOWN. Good bye!"));

        return server;
    }

    /**
     * Creates new {@link Routing}.
     *
     * @param config configuration of this server
     * @return routing configured with JSON support, a health check, and a service
     */
    private static Routing createRouting(Config config) {
        return Routing.builder()
                .register(JsonSupport.create())
                .register(JsonBindingSupport.create())
                .register("/travelagency", new TravelAgencyService())
                .build();
    }
}
