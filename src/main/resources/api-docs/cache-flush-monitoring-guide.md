# Cache Flush Monitoring Guide

This guide explains how to monitor the Cache Flush API using the provided metrics, dashboards, and alerts.

## Metrics

The Cache Flush API collects the following metrics:

1. **Cache Flush Operations**: Tracks the number of flush operations by cache type, backend, scope, and success status.
   - Metric name: `cache_flush_operations_total`
   - Labels: `cache_type`, `backend`, `scope`, `success`

2. **Entries Removed**: Tracks the number of entries removed by cache flush operations.
   - Metric name: `cache_flush_entries_removed_total`
   - Labels: `cache_type`, `backend`

3. **Bytes Reclaimed**: Tracks the amount of space reclaimed by cache flush operations.
   - Metric name: `cache_flush_bytes_reclaimed_total`
   - Labels: `cache_type`, `backend`

## Dashboards

### Web UI Dashboard

A simple dashboard is available in the Cache Flush web UI. To access it:

1. Navigate to the Cache Flush page
2. Click on the "Metrics" tab
3. The dashboard shows:
   - Action Cache metrics (successful operations, failed operations, entries removed)
   - CAS metrics (successful operations, failed operations, entries removed, space reclaimed)

### Grafana Dashboard

A more comprehensive Grafana dashboard is provided for advanced monitoring. To use it:

1. Import the dashboard JSON file (`cache-flush-dashboard.json`) into your Grafana instance
2. The dashboard includes:
   - Cache flush operations over time
   - Entries removed over time
   - Bytes reclaimed over time
   - Failed operations counter
   - Operations summary table

## Alerts

The following alerts are configured to detect abnormal flush patterns:

1. **HighFailedFlushOperations**: Triggers when there are more than 5 failed cache flush operations in the last hour.
   - Severity: Warning

2. **ExcessiveCacheFlushOperations**: Triggers when there are more than 20 cache flush operations in the last hour.
   - Severity: Warning

3. **LargeSpaceReclamation**: Triggers when more than 10GB of space is reclaimed in the last hour.
   - Severity: Info

4. **HighEntriesRemoved**: Triggers when more than 10,000 cache entries are removed in the last hour.
   - Severity: Info

## Integration with Prometheus

To integrate the alerts with Prometheus:

1. Add the `cache-flush-alerts.yml` file to your Prometheus configuration
2. Update your Prometheus configuration to include the file:

```yaml
rule_files:
  - "cache-flush-alerts.yml"
```

3. Restart Prometheus to apply the changes

## Best Practices

1. **Regular Monitoring**: Check the dashboards regularly to understand normal flush patterns
2. **Alert Tuning**: Adjust alert thresholds based on your environment's normal behavior
3. **Correlation**: When alerts trigger, correlate with other system metrics to understand the impact
4. **Documentation**: Document any unusual flush patterns and their causes for future reference