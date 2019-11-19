package io.helidon.examples.saga.booking;

import io.helidon.messaging.MessagingClient;
import io.helidon.messaging.ProcessingMessagingService;
import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletionStage;

public class BookingCommon {
    String tablename;


}

