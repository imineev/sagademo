package io.helidon.examples.saga.travelagency;

import io.helidon.messaging.jms.JMSMessage;

import javax.jms.TextMessage;
import java.sql.Connection;
import java.sql.SQLException;

public class TravelAgencyAutoCompensationInDB extends TravelAgencyCommon {

    OracleConnection oracleConnection = new OracleConnection();

    public TravelAgencyAutoCompensationInDB(String sagaid) throws Exception {
        super(sagaid);
        setupMessaging();
    }

    public void updateDataInReactionToMessage(Connection connection, String service, String action) throws SQLException {
        // no-op
    }

    String processTripBookingRequest() {
        String bookingstate;
        beginSaga();
        sendMessageToBookingService(TravelAgencyService.eventtickets, TravelAgencyService.BOOKINGREQUESTED);
        sendMessageToBookingService(TravelAgencyService.hotel, TravelAgencyService.BOOKINGREQUESTED);
        sendMessageToBookingService(TravelAgencyService.flight, TravelAgencyService.BOOKINGREQUESTED);
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
        sagaId = oracleConnection.beginSaga();
        return true;
    }

    void sendMessageToBookingService(String bookingService, String action) {
        messagingClient.outgoing((connection, session) -> new JMSMessage() {
            @Override
            public javax.jms.Message unwrap(Class unwrapType) {
                try {
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

}
