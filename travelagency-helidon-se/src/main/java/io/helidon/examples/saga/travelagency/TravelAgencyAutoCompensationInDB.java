package io.helidon.examples.saga.travelagency;

import io.helidon.messaging.jms.JMSMessage;

import javax.jms.TextMessage;
import java.sql.*;

public class TravelAgencyAutoCompensationInDB extends TravelAgencyCommon {

    OracleConnection oracleConnection = OracleConnection.build();

    public TravelAgencyAutoCompensationInDB(String sagaid) throws Exception {
        super(sagaid);
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
            oracleConnection.commitSaga(sagaid);
            bookingstate = "success";
        } else {
            oracleConnection.abortSaga(sagaid);
            bookingstate = "compensated";
        }
        return bookingstate;
    }

    boolean beginSaga() {
        sagaId = oracleConnection.beginSaga(); // function begin_saga return saga_id_t;
        return true;
    }

    // Does function send_request (saga_id, sender, recipient, payload)
    void sendMessageToBookingService(String bookingService, String action) {
        messagingClient.outgoing((connection, session) -> new JMSMessage() {
            @Override
            public javax.jms.Message unwrap(Class unwrapType) {
                try {
                    TextMessage textMessage = session.createTextMessage(action + " for sagaid:" + sagaid);
                    textMessage.setStringProperty("action", action);
                    textMessage.setStringProperty("sagaid", sagaid);
                    boolean isFailTest = TravelAgencyService.failtestMap.get(bookingService) != null &&
                            TravelAgencyService.failtestMap.get(bookingService).equals(action);
                    textMessage.setBooleanProperty("failtest", isFailTest);
                    System.out.println("TravelAgencyCompensationInCode.unwrap " +
                            "action:" + textMessage.getStringProperty("action")+
                            "isFailTest:" + textMessage.getBooleanProperty("failtest"));
                    return textMessage;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }, bookingService, true);
    }

    public void updateDataInReactionToMessage(Connection connection, String service, String action) throws SQLException {
        // no-op
    }

}
