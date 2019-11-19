package io.helidon.examples.saga.booking;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class BookingCommon {

    Config createConfig() {
        return new Config() {
            HashMap<String, String> values = new HashMap<>();
            String connectorName = "aq";
            private void createValues() {
                values.put("mp.messaging.connector."+connectorName+".classname",
                        "io.helidon.messaging.jms.connector.JMSConnector");
                createValuesForChannel(BookingService.serviceName, "mp.messaging.incoming.");
                createValuesForChannel(BookingService.serviceName, "mp.messaging.outgoing.");
            }

            private void createValuesForChannel(String channelname, String incomingoroutgoingprefix) {
                values.put(incomingoroutgoingprefix + channelname + ".connector", connectorName);
                values.put(incomingoroutgoingprefix + channelname + ".url", BookingService.url);
                values.put(incomingoroutgoingprefix + channelname + ".user", BookingService.user);
                values.put(incomingoroutgoingprefix + channelname + ".password", BookingService.password);
                values.put(incomingoroutgoingprefix + channelname + ".queue", channelname + "queue");
                if (incomingoroutgoingprefix.equals("mp.messaging.incoming."))
                    values.put(incomingoroutgoingprefix + channelname + ".selector",
                            "action = '" + BookingService.BOOKINGREQUESTED + "' OR " +
                                    "action = '" + BookingService.SAGACOMPLETEREQUESTED + "' OR " +
                                    "action = '" + BookingService.SAGACOMPENSATEREQUESTED + "' "
                    );
            }

            @Override
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                createValues();
                return (T) values.get(propertyName);
            }

            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                createValues();
                return Optional.empty();
            }

            @Override
            public Iterable<String> getPropertyNames() {
                createValues();
                return values.keySet();
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                return null;
            }
        };

    }

    Message getReplyMessage(Session session, String sagaId, String replyMessageAction) {
        try {
            TextMessage textMessage = session.createTextMessage(
                    "booking result for " + BookingService.serviceName + " replyMessageAction:" + replyMessageAction);
            textMessage.setStringProperty("action", replyMessageAction);
            textMessage.setStringProperty("sagaid", sagaId);
            textMessage.setStringProperty("service", BookingService.serviceName);
            return new Message() {
                @Override
                public Object getPayload() {
                    return "booking result";
                }

                @Override
                public CompletionStage<Void> ack() {
                    return null;
                }

                @Override
                public Object unwrap(Class unwrapType) {
                    return textMessage;
                }
            };
        } catch (JMSException e) {
            e.printStackTrace();
            return null; //todo
        }
    }

}

