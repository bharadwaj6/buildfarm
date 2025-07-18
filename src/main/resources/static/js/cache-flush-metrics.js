/**
 * Cache Flush Metrics Dashboard JavaScript.
 */

// Function to fetch metrics data from the API
async function fetchMetrics() {
  try {
    const response = await fetch('/admin/v1/cache/metrics');
    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching metrics:', error);
    return null;
  }
}

// Function to update the metrics dashboard
async function updateMetricsDashboard() {
  const metrics = await fetchMetrics();
  if (!metrics) {
    document.getElementById('metrics-error').style.display = 'block';
    document.getElementById('metrics-container').style.display = 'none';
    return;
  }
  
  document.getElementById('metrics-error').style.display = 'none';
  document.getElementById('metrics-container').style.display = 'block';
  
  const cacheTypes = metrics.cache_types;
  
  // Update Action Cache metrics
  const actionCache = cacheTypes['action-cache'];
  document.getElementById('ac-operations-success').textContent = actionCache.operations_success || 0;
  document.getElementById('ac-operations-failure').textContent = actionCache.operations_failure || 0;
  document.getElementById('ac-entries-removed').textContent = actionCache.entries_removed || 0;
  
  // Update CAS metrics
  const cas = cacheTypes['cas'];
  document.getElementById('cas-operations-success').textContent = cas.operations_success || 0;
  document.getElementById('cas-operations-failure').textContent = cas.operations_failure || 0;
  document.getElementById('cas-entries-removed').textContent = cas.entries_removed || 0;
  document.getElementById('cas-bytes-reclaimed').textContent = formatBytes(cas.bytes_reclaimed || 0);
}

// Function to format bytes into a human-readable format
function formatBytes(bytes) {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Initialize the dashboard
document.addEventListener('DOMContentLoaded', function() {
  // Initial update
  updateMetricsDashboard();
  
  // Set up auto-refresh every 30 seconds
  setInterval(updateMetricsDashboard, 30000);
  
  // Set up manual refresh button
  document.getElementById('refresh-metrics').addEventListener('click', function() {
    updateMetricsDashboard();
  });
});