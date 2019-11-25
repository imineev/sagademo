package io.helidon.examples.saga.travelagency;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents future JDBC client SAGA API
 */
public class OracleConnection {
    Connection connection = null;
    String sagaId ;

    public static OracleConnection build() {
        return null;
    }

    public String beginSaga() {
        sagaId = "testSagaId";
        try {
            connection.createStatement().execute("execute DBMS_Saga_Package.begin_Saga(" + sagaId + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sagaId;
    }

    public void commitSaga(String sagaid) {
        // if(JSON doc1a = success & JSON doc2a = success & JSON doc3a = success)
        try {
            connection.createStatement().execute("execute DBMS_Saga_Package.commit_saga(" + sagaId + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // else throw exception which will result in abortSaga being called
    }

    public void abortSaga(String sagaid) {
        try {
            connection.createStatement().execute("execute DBMS_Saga_Package.abort_saga(" + sagaId + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
