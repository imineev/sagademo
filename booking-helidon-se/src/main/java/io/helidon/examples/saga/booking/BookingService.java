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

package io.helidon.examples.saga.booking;


// Booking Service for EventTickets, Flight, and Hotel
// As they all serve the same basic functionality we use the same impl here and
//  use environment/config to say which io.helidon.examples.saga.booking service type
public class BookingService {

    protected static final String BOOKINGREQUESTED = "BOOKINGREQUESTED";
    protected static final String BOOKINGFAIL = "BOOKINGFAIL";
    protected static final String BOOKINGSUCCESS = "BOOKINGSUCCESS";
    protected static final String SAGACOMPLETEREQUESTED = "SAGACOMPLETEREQUESTED";
    protected static final String SAGACOMPLETESUCCESS = "SAGACOMPLETESUCCESS";
    protected static final String SAGACOMPLETEFAIL = "SAGACOMPLETEFAIL";
    protected static final String SAGACOMPENSATEREQUESTED = "SAGACOMPENSATEREQUESTED";
    protected static final String SAGACOMPENSATEFAIL = "SAGACOMPENSATEFAIL";
    protected static final String SAGACOMPENSATESUCCESS = "SAGACOMPENSATESUCCESS";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String JOURNAL = "journal ";
    static final boolean IS_AUTO_COMPENSATING_DB =
            Boolean.valueOf(System.getProperty("autocompensating.db", "false"));
    static String serviceName;

    public static void main(String args[]) throws Exception {
        serviceName = args[0];
        System.out.println("BookingService.main serviceName:" + serviceName);
        Object implemention = IS_AUTO_COMPENSATING_DB?
                new BookingAutoCompensationInDB():
                new BookingCompensationInCode();
        while (true) Thread.sleep(1000 * 30);
    }





}
