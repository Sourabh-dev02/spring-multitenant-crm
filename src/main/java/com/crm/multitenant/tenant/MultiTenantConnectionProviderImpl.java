package com.crm.multitenant.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Swaps the PostgreSQL search_path on each connection to route queries
 * to the right tenant schema.
 *
 * We include "public" in the search_path so the shared tenants table
 * remains accessible even when we're in a tenant schema context.
 *
 * Schema names must be validated before use to prevent SQL injection -
 * see TenantProvisioningService which enforces a strict naming pattern.
 */
@Slf4j
@Component("multiTenantConnectionProvider")
@RequiredArgsConstructor
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        try {
            switchSchema(connection, tenantIdentifier);
        } catch (SQLException e) {
            log.error("Failed to switch connection to schema [{}]", tenantIdentifier, e);
            // return the connection to the pool before re-throwing
            releaseAnyConnection(connection);
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // always reset to default before handing back to the pool
            // otherwise the next thread borrowing this connection gets the wrong schema
            switchSchema(connection, TenantIdentifierResolver.DEFAULT_SCHEMA);
        } catch (SQLException e) {
            log.warn("Could not reset schema to public on connection release - closing connection instead", e);
            connection.close();
            return;
        }
        connection.close();
    }

    private void switchSchema(Connection connection, String schema) throws SQLException {
        // schema names are alphanumeric + underscores only (validated at tenant creation)
        // still worth guarding here as a second line of defense
        if (!schema.matches("[a-z0-9_]+")) {
            throw new SQLException("Invalid schema name: " + schema);
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + schema + ", public");
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // set to false unless using JTA/XA datasources - HikariCP doesn't need it
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Unwrap not supported on this provider");
    }
}
