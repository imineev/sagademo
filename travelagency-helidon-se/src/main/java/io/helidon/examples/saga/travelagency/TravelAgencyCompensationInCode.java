package io.helidon.examples.saga.travelagency;


import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.jms.JMSMessage;

import javax.jms.*;
import java.sql.Connection;
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
                            String action = jmsMessage.getStringProperty("action");
                            System.out.println("AQ IncomingMessagingService.onIncoming " +
                                    "sagaid:" + sagaid + "message:" + message +
                                    " bookingService:" + service + " action/reply:" + action);
                            updateDataInReactionToMessage(connection, service, action);
                            switch (service) {
                                case TravelAgencyService.EVENTTICKETS:
                                    eventticketsstate = action;
                                    break;
                                case TravelAgencyService.HOTEL:
                                    hotelstate = action;
                                    break;
                                case TravelAgencyService.FLIGHT:
                                    flightstate = action;
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.eventtickets,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.hotel,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.flight,
                null, true);
    }

    public void updateDataInReactionToMessage(Connection connection, String service, String action) throws SQLException {
        connection.createStatement().execute(
                "update travelagency set  " + service + "state = '" + action + "'");
    }

    String processTripBookingRequest() {
        String bookingstate;
//        try {
        beginSaga();
        sendMessageToBookingService(TravelAgencyService.eventtickets, TravelAgencyService.BOOKINGREQUESTED);
        sendMessageToBookingService(TravelAgencyService.hotel, TravelAgencyService.BOOKINGREQUESTED);
        sendMessageToBookingService(TravelAgencyService.flight, TravelAgencyService.BOOKINGREQUESTED);
        if (allParticipantsReplySuccessfully(
                TravelAgencyService.BOOKINGSUCCESS, TravelAgencyService.BOOKINGFAIL, 300)) {
            // complete/cleanup...
            sendMessageToBookingService(TravelAgencyService.eventtickets, TravelAgencyService.SAGACOMPLETEREQUESTED);
            sendMessageToBookingService(TravelAgencyService.hotel, TravelAgencyService.SAGACOMPLETEREQUESTED);
            sendMessageToBookingService(TravelAgencyService.flight, TravelAgencyService.SAGACOMPLETEREQUESTED);
            if (allParticipantsReplySuccessfully(TravelAgencyService.SAGACOMPLETESUCCESS,
                    TravelAgencyService.SAGACOMPLETEFAIL, 120)) {
                bookingstate = "success";
            } else { //request status...
                sendMessageToBookingService(TravelAgencyService.eventtickets, TravelAgencyService.STATUSREQUESTED);
                sendMessageToBookingService(TravelAgencyService.hotel, TravelAgencyService.STATUSREQUESTED);
                sendMessageToBookingService(TravelAgencyService.flight, TravelAgencyService.STATUSREQUESTED);
                bookingstate = recoverBasedOnStatus();
            }
        } else { //compensate...
            sendMessageToBookingService(TravelAgencyService.eventtickets, TravelAgencyService.SAGACOMPENSATEREQUESTED);
            sendMessageToBookingService(TravelAgencyService.hotel, TravelAgencyService.SAGACOMPENSATEREQUESTED);
            sendMessageToBookingService(TravelAgencyService.flight, TravelAgencyService.SAGACOMPENSATEREQUESTED);
            if (allParticipantsReplySuccessfully(TravelAgencyService.SAGACOMPENSATESUCCESS,
                    TravelAgencyService.SAGACOMPENSATEFAIL, 120)) {
                bookingstate = "compensated";  //ie failed but compensated successfully
            } else { //request status...
                sendMessageToBookingService(TravelAgencyService.eventtickets, TravelAgencyService.STATUSREQUESTED);
                sendMessageToBookingService(TravelAgencyService.hotel, TravelAgencyService.STATUSREQUESTED);
                sendMessageToBookingService(TravelAgencyService.flight, TravelAgencyService.STATUSREQUESTED);
                bookingstate =  recoverBasedOnStatus();
            }
        }
        return bookingstate;
    }

    boolean beginSaga() {
        try {
            connection.createStatement().execute("delete travelagency "); //todo temporary to insure cleanAll run
            connection.createStatement().execute(
                    "insert into travelagency (sagaid, sagastate, eventticketsstate, hotelstate, flightstate) " +
                            "values ('" + sagaId + "', '" + sagaState + "', '', '', '' )");
            sagaState = "begun";
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    void sendMessageToBookingService(String bookingService, String action) {
        messagingClient.outgoing((connection, session) -> new JMSMessage() {
            @Override
            public javax.jms.Message unwrap(Class unwrapType) {
                try {
                    connection.createStatement().execute(
                            "update travelagency set " + bookingService + "state = '" + action + "'"
                                    + " where sagaid = '" + sagaid + "'");
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


    private String recoverBasedOnStatus() {
        determineIfSagaActuallyFailed();
        // ...
        return "unknown";
    }

    /**
     * Cleanup may have failed but the actual saga completed
     * in which case compensation would not be appropriate.
     */
    private void determineIfSagaActuallyFailed() {
//        determineStateOfParticipants();
//        compensateSaga();
    }

}
