package io.helidon.examples.saga.travelagency;


import io.helidon.messaging.jms.JMSMessage;

import javax.jms.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


class TravelAgencyCompensationInCode extends TravelAgencyCommon {
    TravelAgencyCompensationInCode(String sagaid) throws Exception {
        super(sagaid);
        checkForRecovery();
    }

    private void checkForRecovery() throws Exception {
        ResultSet resultSet = connection.createStatement().executeQuery(
                "select * from travelagency");
        while (resultSet.next())
        {
            String sagaId = resultSet.getString("sagaid");
            String sagaState = resultSet.getString("sagastate");
            recoverBasedOnStatus(sagaId, sagaState);
        }
    }

    private String recoverBasedOnStatus(String sagaId, String sagaState) {
        // todo backdown retries etc...
        if (sagaState.equals(TravelAgencyService.SAGACOMPLETEREQUESTED)) {
            System.out.println("TravelAgencyCompensationInCode.recoverBasedOnStatus complete");
            return "success";
        } else if (sagaState.equals(TravelAgencyService.SAGACOMPENSATEREQUESTED)) {
            System.out.println("TravelAgencyCompensationInCode.recoverBasedOnStatus compensate");
            return "compensated";
        }
        return "unknown";

    }

    boolean updateSagaStatus(String sagastate) {
        try {
            sagaState = sagastate;
            connection.createStatement().execute(
                    "update travelagency set sagastate = '" + sagastate + "' where sagaid = '" + sagaId + "'");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    String processTripBookingRequest() {
        //begin local transaction
        beginSaga();
        sendMessageToBookingService(TravelAgencyService.EVENTTICKETS, TravelAgencyService.BOOKINGREQUESTED);
        sendMessageToBookingService(TravelAgencyService.HOTEL, TravelAgencyService.BOOKINGREQUESTED);
        sendMessageToBookingService(TravelAgencyService.FLIGHT, TravelAgencyService.BOOKINGREQUESTED);
        //commit local transaction
        sagaState = TravelAgencyService.BOOKINGREQUESTED;
        if (allParticipantsReplySuccessfully(
                TravelAgencyService.BOOKINGSUCCESS, TravelAgencyService.BOOKINGFAIL, 300)) {
            // complete/cleanup...
            updateSagaStatus(TravelAgencyService.SAGACOMPLETEREQUESTED);
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
                bookingstate = recoverBasedOnStatus(sagaid, TravelAgencyService.SAGACOMPLETEREQUESTED);
            }
        } else { //compensate...
            updateSagaStatus(TravelAgencyService.SAGACOMPENSATEREQUESTED);
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
                bookingstate =  recoverBasedOnStatus(sagaid, TravelAgencyService.SAGACOMPENSATEREQUESTED);
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
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    void sendMessageToBookingService(String bookingService, String action) {
        messagingClient.outgoing((connection, session) -> new JMSMessage() {
            @Override
            public Message unwrap(Class unwrapType) {
                try {
                    ((java.sql.Connection)connection.unwrap(java.sql.Connection.class)).createStatement().execute(
                            "update travelagency set " + bookingService + "state = '" + action + "'"
                                    + " where sagaid = '" + sagaid + "'");
                    TextMessage textMessage =
                            ((javax.jms.Session)session.unwrap(javax.jms.Session.class))
                                    .createTextMessage(action + " for sagaid:" + sagaid);
                    textMessage.setStringProperty("action", action);
                    textMessage.setStringProperty("sagaid", sagaid);
                    boolean isFailTest = TravelAgencyService.failtestMap.get(bookingService) != null &&
                            TravelAgencyService.failtestMap.get(bookingService).equals(action);
                    textMessage.setBooleanProperty("failtest", isFailTest);
                    System.out.println("TravelAgencyCompensationInCode.unwrap " +
                            "bookingService:" + bookingService +
                            " action:" + textMessage.getStringProperty("action") +
                            " isFailTest:" + textMessage.getBooleanProperty("failtest"));
                    return textMessage;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }, bookingService, true);
    }

    public void updateDataInReactionToMessage(Connection connection, String service, String action) throws SQLException {
        connection.createStatement().execute(
                "update travelagency set  " + service + "state = '" + action + "'");
    }

}
