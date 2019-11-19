package io.helidon.examples.saga.booking;

import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.MessagingClient;
import io.helidon.messaging.ProcessingMessagingService;

public class BookingCompensationInCode extends BookingCommon {

    private MessagingClient messagingClient;

    public BookingCompensationInCode()  {
        messagingClient = MessagingClient.build(createConfig());
        setupMessaging();
    }

    private void setupMessaging()  {
        ProcessingMessagingService processingMessagingService = (message, connection, session) -> {
            System.out.println("-------------->MessagingService.doIncomingOutgoing connection:" + connection +
                    "Session:" + session + " do db work...");
            MessageWithConnectionAndSession messageWithConnectionAndSession =
                    (MessageWithConnectionAndSession) message.unwrap(javax.jms.Message.class);
            javax.jms.Message jmsMessage = messageWithConnectionAndSession.getPayload();
            String action = jmsMessage.getStringProperty("action");
            System.out.println("BookingCompensationInCode incoming jmsMessage action property:" + action);
            String sagaId = jmsMessage.getStringProperty("sagaid");
            System.out.println("BookingCompensationInCode incoming jmsMessage sagaid property:" + sagaId);
            String replyMessageAction = "unknown";
            switch (action) { //todo inventory is either 0 or 1 currently
                case BookingService.BOOKINGREQUESTED:
                    if (BookingService.IS_FAILBOOKING_TEST) {
                        replyMessageAction = BookingService.BOOKINGFAIL;
                        break;
                    }
                    connection.createStatement().execute(
                            "update " + BookingService.serviceName + " set inventorycount = 0");
                    connection.createStatement().execute(
                            "update " + BookingService.serviceName + "journal " +
                                    "set sagastate = '" + BookingService.BOOKINGSUCCESS +"', originalinventorycount = 1");
                    replyMessageAction = BookingService.BOOKINGSUCCESS;
                    break;
                case BookingService.SAGACOMPLETEREQUESTED:
                    if (BookingService.IS_FAILCOMPLETE_TEST) {
                        replyMessageAction = BookingService.SAGACOMPLETEFAIL;
                        break;
                    }
                    connection.createStatement().execute(
                            "delete " + BookingService.serviceName + "journal " );
                    replyMessageAction = BookingService.SAGACOMPLETESUCCESS;
                    break;
                case BookingService.SAGACOMPENSATEREQUESTED:
                    if (BookingService.IS_FAILCOMPENSATE_TEST) {
                        replyMessageAction = BookingService.SAGACOMPENSATEFAIL;
                        break;
                    }
                    connection.createStatement().execute(
                            "update " + BookingService.serviceName + " set inventorycount = 1");
                    connection.createStatement().execute(
                            "update " + BookingService.serviceName + "journal " +
                                    "set sagastate = '" + BookingService.SAGACOMPENSATESUCCESS +"', originalinventorycount = 1");
                    replyMessageAction = BookingService.SAGACOMPENSATESUCCESS;
                    break;
            }
            return getReplyMessage(session, sagaId, replyMessageAction);
        };
        messagingClient.incomingoutgoing( processingMessagingService,
                BookingService.serviceName, BookingService.serviceName,
                null,  true);

        System.out.println("Waiting for booking requests...");
    }


}