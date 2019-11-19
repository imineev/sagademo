package io.helidon.examples.saga.booking;


import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.MessagingClient;
import io.helidon.messaging.ProcessingMessagingService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class BookingAutoCompensationInDB extends BookingCommon {

    private MessagingClient messagingClient;

    public BookingAutoCompensationInDB() {
        messagingClient = MessagingClient.build(createConfig());
        setupMessaging();
    }


    private void setupMessaging()  {
        ProcessingMessagingService processingMessagingService = (message, connection, session) -> {
            MessageWithConnectionAndSession messageWithConnectionAndSession =
                    (MessageWithConnectionAndSession) message.unwrap(javax.jms.Message.class);
            javax.jms.Message jmsMessage = messageWithConnectionAndSession.getPayload();
            String action = jmsMessage.getStringProperty("action");
            String sagaId = jmsMessage.getStringProperty("sagaid");
            String replyMessageAction = "unknown";
            switch (action) { //todo inventory is either 0 or 1 currently
                case BookingService.BOOKINGREQUESTED:
                    if (BookingService.IS_FAILBOOKING_TEST) {
                        replyMessageAction = BookingService.BOOKINGFAIL;
                        break;
                    }
                    connection.createStatement().execute(
                            "update " + BookingService.serviceName + " set inventorycount = 0");
                    replyMessageAction = BookingService.BOOKINGSUCCESS;
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
