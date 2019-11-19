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
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;


public class TravelAgencyService implements Service {

    public static final String EVENTTICKETS = "eventtickets", FLIGHT = "flight",  HOTEL = "hotel";
    protected static final String SAGACOMPLETEREQUESTED = "SAGACOMPLETEREQUESTED";
    protected static final String BOOKINGFAIL = "BOOKINGFAIL";
    protected static final String BOOKINGSUCCESS = "BOOKINGSUCCESS";
    protected static final String SAGACOMPLETEFAIL = "SAGACOMPLETEFAIL";
    protected static final String SAGACOMPLETESUCCESS = "SAGACOMPLETESUCCESS";
    protected static final String SAGACOMPENSATEREQUESTED = "SAGACOMPENSATEREQUESTED";
    protected static final String SAGACOMPENSATEFAIL = "SAGACOMPENSATEFAIL";
    protected static final String SAGACOMPENSATESUCCESS = "SAGACOMPENSATESUCCESS";
    protected static final String BOOKINGREQUESTED = "BOOKINGREQUESTED";
    static final boolean IS_AUTO_COMPENSATING_DB =
            Boolean.valueOf(System.getProperty("autocompensating.db", "false"));
    static String url = System.getProperty("url");
    static String user = System.getProperty("user");
    static String password = System.getProperty("password");
    private TravelAgencyCommon travelAgencyServiceOperations;

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/booktrip", this::booktrip);
    }

    private void booktrip(ServerRequest serverRequest, ServerResponse serverResponse) {
        String bookingstate = doBookTrip();
        sendResponse(serverResponse, bookingstate);
    }

    String doBookTrip() {
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


}
