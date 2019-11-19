package io.helidon.examples.saga.travelagency;


import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.jms.JMSMessage;

import javax.jms.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


class TravelAgencyCompensationInCode extends TravelAgencyCommon {


    TravelAgencyCompensationInCode(String sagaid) throws Exception {
        super(sagaid);
        setupMessaging();
    }

    void setupMessaging() {
        IncomingMessagingService incomingMessagingService =
                new IncomingMessagingService() {
                    @Override
                    public void onIncoming(org.eclipse.microprofile.reactive.messaging.Message message, Connection connection, Session session) {
                        MessageWithConnectionAndSession messageWithConnectionAndSession = (MessageWithConnectionAndSession) message.unwrap(Message.class);
                        try {
                            Message jmsMessage = messageWithConnectionAndSession.getPayload();
                            String sagaid = jmsMessage.getStringProperty("sagaid");
                            String service = jmsMessage.getStringProperty("service");
                            System.out.println("AQ IncomingMessagingService.onIncoming " +
                                    "sagaid:" + sagaid + "message:" + message + " bookingService:" + service);
                            connection.createStatement().execute( //todo ...
                                    "update travelagency set  " + service + "state = 'BOOKINGSUCCESS'");
//                                    "update travelagency set  eventticketsstate = 'BOOKINGSUCCESS', " +
//                                            "hotelstate='BOOKINGSUCCESS', flightstate='BOOKINGSUCCESS'");
//                            connection.createStatement().execute(
//                                        "update travelagency set " + bookingService + "state = '"+bookingServiceReply+"' " +
//                                                "where sagaid = '" + sagaid + "'");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
        messagingClient.incoming(incomingMessagingService, eventtickets,
                null, true);
        messagingClient.incoming(incomingMessagingService, hotel,
                null, true);
        messagingClient.incoming(incomingMessagingService, flight,
                null, true);
    }

    String processTripBookingRequest() {
        String bookingstate = "unknown";
        try {
            beginSaga(); //writes to own saga table
            updateSagaStateandSendMessageToBookingService(eventtickets, TravelAgencyService.BOOKINGREQUESTED);
            updateSagaStateandSendMessageToBookingService(hotel, TravelAgencyService.BOOKINGREQUESTED);
            updateSagaStateandSendMessageToBookingService(flight, TravelAgencyService.BOOKINGREQUESTED);
            if (allParticipantsReplySuccessfully()) {
                updateSagaStateandSendMessageToBookingService(eventtickets, TravelAgencyService.SAGACOMPLETEREQUESTED);
                updateSagaStateandSendMessageToBookingService(hotel, TravelAgencyService.SAGACOMPLETEREQUESTED);
                updateSagaStateandSendMessageToBookingService(flight, TravelAgencyService.SAGACOMPLETEREQUESTED);
                //cleanup (can be async) ...
                cleanupBookingService(eventtickets);
                cleanupBookingService(hotel);
                cleanupBookingService(flight);
                bookingstate = "success";
            } else {
                compensateSaga();
                bookingstate = "compensated";
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            determineIfSagaActuallyFailed();
            compensateSaga();
            bookingstate = "fail";
        } finally {
            return bookingstate;
        }
    }

    private void beginSaga() throws SQLException {
        System.out.println("TravelAgencyCompensationInCode.beginSaga insert into travelagency table...");
        connection.createStatement().execute(  "delete travelagency ");
        connection.createStatement().execute(
                "insert into travelagency (sagaid, sagastate, eventticketsstate, hotelstate, flightstate) " +
                        "values ('" + sagaId + "', '" + sagaState + "', '', '', '' )");
        sagaState = "begun";
    }

    private void updateSagaStateandSendMessageToBookingService(String bookingService, String action) {
        messagingClient.outgoing((connection, session) -> new JMSMessage() {
            @Override
            public javax.jms.Message unwrap(Class unwrapType) {
                try {
                    System.out.println("TravelAgencyCompensationInCode updating " + bookingService + "state and " +
                            "returning message to send...");
                    connection.createStatement().execute(
                            "update travelagency set " + bookingService + "state = '" + action + "'"
                                    +   " where sagaid = '" + sagaid + "'");
                    TextMessage textMessage = session.createTextMessage(action + " for sagaid:" + sagaid);
                    textMessage.setStringProperty("action", action);
                    textMessage.setStringProperty("sagaid", sagaid);
                    return textMessage;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }, bookingService, true);
    }

    private boolean allParticipantsReplySuccessfully() {
        while (true) { // todo currently we wait forever
            try {
            ResultSet resultSet = connection.createStatement().executeQuery(
                    "select * from travelagency"); // where sagaid = '" + sagaid + "'");
            if (resultSet.next()) {
                eventticketsstate = resultSet.getString("eventticketsstate");
                hotelstate = resultSet.getString("hotelstate");
                flightstate = resultSet.getString("flightstate");
                System.out.println("TravelAgencyCompensationInCode.allParticipantsReplySuccessfully " +
                        "eventticketsstate:" + eventticketsstate + " hotelstate:" + hotelstate + " flightstate:" + flightstate);
                if(TravelAgencyService.BOOKINGSUCCESS.equals(eventticketsstate)
                        && TravelAgencyService.BOOKINGSUCCESS.equals(hotelstate)
                        && TravelAgencyService.BOOKINGSUCCESS.equals(flightstate)) {
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~SAGA SUCCESSFUL~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~SAGA SUCCESSFUL~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~SAGA SUCCESSFUL~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    return true;
                } else if(TravelAgencyService.BOOKINGFAIL.equals(eventticketsstate)
                        && TravelAgencyService.BOOKINGFAIL.equals(hotelstate)
                        && TravelAgencyService.BOOKINGFAIL.equals(flightstate)) {
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~SAGA FAIL~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~SAGA FAIL~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~SAGA FAIl~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    return false;
                }
            } else {
                System.out.println("TravelAgencyCompensationInCode.allParticipantsReplySuccessfully " +
                        "no entry for sagaid:" + sagaid);
            }
                Thread.sleep(1 * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //completion/success cleanup methods...

    private void compensateSaga() {
//        requestCompleteSaga(TravelAgencyService.EVENTTICKETS, sagaId);
    }

    private void cleanupBookingService(String bookingService) {
//        requestCompleteSaga(TravelAgencyService.EVENTTICKETS, sagaId);
    }

    //compensation methods...

    /**
     * Cleanup may have failed but the actual saga completed
     * in which case compensation would not be appropriate.
     */
    private void determineIfSagaActuallyFailed() {
//        determineStateOfParticipants();
//        compensateSaga();
    }

}
