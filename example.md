# How to Use?

1. Create a class that extends AbstractRoutingDataSource. And Override determineCurrentLookupKey().
2. And add your Custom Datasource.
3. You call PTSync.execute(), when you want.

### Example
```java
public class RoutingDataSource extends AbstractRoutingDataSource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

        PrivateDockerConfig dockerConfig = new PrivateDockerConfig(
                "tcp://{your_docker_host}:{port}",
                "{docker_name}",
                "{docker_password}",
                "{docker_email}"
        );

        if (readOnly) {
            try {
                PTSync.execute(dockerConfig, "{your_master_db_container_name}",
                        "pt-table-sync --execute --no-check-slave h={master_host},D={master_database},t={master_table},u={master_user},p={master_password}" +
                                " h={slave_host},D={slave_database},t={slave_table},u={slave_user},p={slave_password}");
                log.info("!Success Call PTSync!");
            } catch (Exception e) {
                log.error("error by PTSync message = {},   cause = {}", e.getMessage(), e.getCause());
            }

            //return your Custom Datasource
            return DataSourceType.REPLICA;
        }

        //return your Custom Datasource
        return DataSourceType.SOURCE;
    }
}
```