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

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import java.util.HashMap;
import java.util.Map;


public class TravelAgencyService implements Service {

    public static final String EVENTTICKETS = "eventtickets", FLIGHT = "flight",  HOTEL = "hotel";
    static final String BOOKINGREQUESTED = "BOOKINGREQUESTED";
    static final String BOOKINGFAIL = "BOOKINGFAIL";
    static final String BOOKINGSUCCESS = "BOOKINGSUCCESS";
    static final String SAGACOMPLETEREQUESTED = "SAGACOMPLETEREQUESTED";
    static final String SAGACOMPLETEFAIL = "SAGACOMPLETEFAIL";
    static final String SAGACOMPLETESUCCESS = "SAGACOMPLETESUCCESS";
    protected static final String SAGACOMPENSATEREQUESTED = "SAGACOMPENSATEREQUESTED";
    static final String SAGACOMPENSATEFAIL = "SAGACOMPENSATEFAIL";
    static final String SAGACOMPENSATESUCCESS = "SAGACOMPENSATESUCCESS";
    protected static final String STATUSREQUESTED = "STATUSREQUESTED";
    static final boolean IS_AUTO_COMPENSATING_DB =
            Boolean.valueOf(System.getProperty("autocompensating.db", "false"));
    static final String eventtickets = "eventtickets";
    static final String hotel = "hotel";
    static final String flight = "flight";
    static String url = System.getProperty("url");
    static String user = System.getProperty("user");
    static String password = System.getProperty("password");
    static Map failtestMap = new HashMap();
    private TravelAgencyCommon travelAgencyServiceOperations;

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/booktrip", this::booktrip);
        rules.get("/booktripfailflightbooking", this::booktripfailflightbooking);
        rules.get("/", this::home);
    }

    private void booktrip(ServerRequest serverRequest, ServerResponse serverResponse) {
        String sagaId = serverRequest.path().param("sagaid");
        String bookingstate = doBookTrip(false);
        sendResponse(serverResponse, bookingstate);
    }

    private void booktripfailflightbooking(ServerRequest serverRequest, ServerResponse serverResponse) {
        String bookingstate = doBookTrip(false);
        sendResponse(serverResponse, bookingstate);
    }

    //todo Items not currently shown in the demo...
    //todo Disadvantages when running a non-AQ messaging system (ie both scenarios use AQ and it's capabilities)
    //todo Failure during complete/commit
    //todo Failure during compensate/abort
    //todo Travelagency crash
    //todo Booking service crash
    //todo Concurrent access of inventory in booking services (ie escrow feature). Inventory counts are 0 or 1 for convenient testing.
    String doBookTrip(boolean isFailTest) {
        if(isFailTest) failtestMap.put(FLIGHT, BOOKINGREQUESTED);
        String sagaid = "testsagaid";
        System.out.println("TravelAgencyService.booktrip IS_AUTO_COMPENSATING_DB:" + IS_AUTO_COMPENSATING_DB);
        try {
            travelAgencyServiceOperations = IS_AUTO_COMPENSATING_DB?
                    new TravelAgencyAutoCompensationInDB(sagaid):
                    new TravelAgencyCompensationInCode(sagaid);
            return travelAgencyServiceOperations.processTripBookingRequest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private Void sendResponse(ServerResponse response, String bookingstate) {
        response.status(Http.Status.OK_200);
        System.out.println("travel io.helidon.examples.saga.booking:" + bookingstate);
        response.send("travel io.helidon.examples.saga.booking:" + bookingstate);
        return null;
    }


    private void home(ServerRequest serverRequest, ServerResponse serverResponse) {
//        serverResponse.status(Http.Status.OK_200);
//        serverResponse.registerFilter()
        System.out.println("home page...");
        serverResponse.status(Http.Status.OK_200).headers().contentType(MediaType.TEXT_HTML);
        serverResponse.send( getHomePage());
//        serverResponse.send( "curl http://locahost:8080/travelagency/booktrip" +
//                "\n or \n" +
//                "curl http://locahost:8080/travelagency/booktripfailflightbooking");
    }


    public String getHomePage() {
        return "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                "    <title>Home</title>" +
                "    <link rel=\"stylesheet\" href=\"./oracledbdemo_files/style.css\">" +
                "  </head>" +
                "  <body>" +
                "    <h1>Oracle DB Microservices Saga Demo</h1>" +
                "<p>Supplier Service Backend/Admin</p>" +
                "<p><a href=\"success\">Book travel - success/complete/commit</a></p>" +
                "<p><a href=\"fail\">Book travel - fail/compensate/abort (flight service will fail causing compensation)</a></p>" +
                "</body></html>";
    }

}
