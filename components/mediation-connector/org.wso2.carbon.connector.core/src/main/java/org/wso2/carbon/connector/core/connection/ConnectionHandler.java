/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.connector.core.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.pool.Configuration;
import org.wso2.carbon.connector.core.pool.ConnectionFactory;
import org.wso2.carbon.connector.core.pool.ConnectionPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Handles the connections
 */
public class ConnectionHandler {

    private static final Log log = LogFactory.getLog(ConnectionHandler.class);
    private static final ConnectionHandler handler;
    // Stores connections/connection pools against connection code name
    // defined as <connector_name>:<connection_name>
    private final Map<String, Object> connectionMap;

    static {
        handler = new ConnectionHandler();
    }

    private ConnectionHandler() {

        this.connectionMap = new ConcurrentHashMap<>();
    }

    /**
     * Gets the Connection Handler instance
     *
     * @return ConnectionHandler instance
     */
    public static ConnectionHandler getConnectionHandler() {

        return handler;
    }

    /**
     * Creates a new connection pool and stores the connection
     *
     * @param connector      Name of the connector
     * @param connectionName Name of the connection
     * @param factory        Connection Factory that defines how to create connections
     * @param configuration  Configurations for the connection pool
     */
    public void createConnection(String connector, String connectionName, ConnectionFactory factory,
                                 Configuration configuration) {

        ConnectionPool pool = new ConnectionPool(factory, configuration);
        connectionMap.putIfAbsent(getCode(connector, connectionName), pool);
    }

    /**
     * Stores a new single connection
     *
     * @param connector      Name of the connector
     * @param connectionName Name of the connection
     * @param connection     Connection to be stored
     */
    public void createConnection(String connector, String connectionName, Connection connection) {

        connectionMap.putIfAbsent(getCode(connector, connectionName), connection);
    }

    /**
     * Retrieve connection by connector name and connection name
     *
     * @param connector      Name of the connector
     * @param connectionName Name of the connection
     * @return the connection
     * @throws ConnectException if failed to get connection
     */
    public Connection getConnection(String connector, String connectionName) throws ConnectException {

        Connection connection = null;
        String connectorCode = getCode(connector, connectionName);
        Object connectionObj = connectionMap.get(connectorCode);
        if (connectionObj != null) {
            if (connectionObj instanceof ConnectionPool) {
                connection = (Connection) ((ConnectionPool) connectionObj).borrowObject();
            } else if (connectionObj instanceof Connection) {
                connection = (Connection) connectionObj;
            }
        } else {
            throw new ConnectException(format("Error occurred during retrieving connection. " +
                    "Connection %s for %s connector does not exist.", connectionName, connector));
        }
        return connection;
    }

    /**
     * Return borrowed connection
     *
     * @param connector      Name of the connector
     * @param connectionName Name of the connection
     * @param connection     Connection to be returned to the pool
     */
    public void returnConnection(String connector, String connectionName, Connection connection) {

        String connectorCode = this.getCode(connector, connectionName);
        Object connectionObj = this.connectionMap.get(connectorCode);
        if (connectionObj instanceof ConnectionPool) {
            ((ConnectionPool) connectionObj).returnObject(connection);
        }
    }

    /**
     * Shutdown all the connection pools
     */
    public void shutdownConnections() {

        for (Map.Entry<String, Object> connection : connectionMap.entrySet()) {
            Object connectionObj = connection.getValue();
            if (connectionObj instanceof ConnectionPool) {
                try {
                    ((ConnectionPool) connectionObj).close();
                } catch (ConnectException e) {
                    log.error("Failed to close connection pool. ", e);
                }
            }
        }
    }

    /**
     * Shutdown connection pools for a specified connector
     *
     * @param connector Name of the connector
     */
    public void shutdownConnections(String connector) {

        for (Map.Entry<String, Object> connection : connectionMap.entrySet()) {
            if (connection.getKey().split(":")[0].equals(connector)) {
                Object connectionObj = connection.getValue();
                if (connectionObj instanceof ConnectionPool) {
                    try {
                        ((ConnectionPool) connectionObj).close();
                    } catch (ConnectException e) {
                        log.error("Failed to close connection pool. ", e);
                    }
                }
            }
        }
    }

    /**
     * Check if a connection exists for the connector by the same connection name
     *
     * @param connector      Name of the connector
     * @param connectionName Name of the connection
     * @return true if a connection exists, false otherwise
     */
    public boolean checkIfConnectionExists(String connector, String connectionName) {

        return connectionMap.containsKey(getCode(connector, connectionName));
    }

    /**
     * Retrieves the connection code defined as <connector_name>:<connection_name>
     *
     * @param connector      Name of the connector
     * @param connectionName Name of the connection
     * @return the connector code
     */
    private String getCode(String connector, String connectionName) {

        return format("%s:%s", connector, connectionName);
    }

}
