document.addEventListener('DOMContentLoaded', function() {
    // Tab switching functionality
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabContents = document.querySelectorAll('.tab-content');
    
    tabButtons.forEach(button => {
        button.addEventListener('click', function() {
            // Remove active class from all buttons and contents
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabContents.forEach(content => content.classList.remove('active'));
            
            // Add active class to clicked button and corresponding content
            this.classList.add('active');
            const tabId = this.getAttribute('data-tab') + '-tab';
            document.getElementById(tabId).classList.add('active');
        });
    });
    
    // Handle scope selection for Action Cache
    const acScopeSelect = document.getElementById('ac-scope');
    const acInstanceGroup = document.getElementById('ac-instance-group');
    const acDigestGroup = document.getElementById('ac-digest-group');
    
    acScopeSelect.addEventListener('change', function() {
        if (this.value === 'INSTANCE') {
            acInstanceGroup.classList.remove('hidden');
            acDigestGroup.classList.add('hidden');
        } else if (this.value === 'DIGEST_PREFIX') {
            acInstanceGroup.classList.add('hidden');
            acDigestGroup.classList.remove('hidden');
        } else {
            acInstanceGroup.classList.add('hidden');
            acDigestGroup.classList.add('hidden');
        }
    });
    
    // Handle scope selection for CAS
    const casScopeSelect = document.getElementById('cas-scope');
    const casInstanceGroup = document.getElementById('cas-instance-group');
    const casDigestGroup = document.getElementById('cas-digest-group');
    
    casScopeSelect.addEventListener('change', function() {
        if (this.value === 'INSTANCE') {
            casInstanceGroup.classList.remove('hidden');
            casDigestGroup.classList.add('hidden');
        } else if (this.value === 'DIGEST_PREFIX') {
            casInstanceGroup.classList.add('hidden');
            casDigestGroup.classList.remove('hidden');
        } else {
            casInstanceGroup.classList.add('hidden');
            casDigestGroup.classList.add('hidden');
        }
    });
    
    // Form submission for Action Cache
    const acForm = document.getElementById('action-cache-form');
    const acResult = document.getElementById('ac-result');
    const acResultContent = acResult.querySelector('.result-content');
    
    acForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // Validate form
        if (!validateActionCacheForm()) {
            return;
        }
        
        // Show loading state
        const submitButton = this.querySelector('button[type="submit"]');
        const originalButtonText = submitButton.textContent;
        submitButton.innerHTML = '<span class="spinner"></span> Processing...';
        submitButton.disabled = true;
        
        // Prepare request data
        const formData = new FormData(this);
        const requestData = {
            scope: formData.get('scope'),
            flushRedis: formData.get('flushRedis') === 'on',
            flushInMemory: formData.get('flushInMemory') === 'on'
        };
        
        if (formData.get('scope') === 'INSTANCE') {
            requestData.instanceName = formData.get('instanceName');
        } else if (formData.get('scope') === 'DIGEST_PREFIX') {
            requestData.digestPrefix = formData.get('digestPrefix');
        }
        
        // Send API request
        fetch('/admin/v1/cache/action/flush', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errorData => {
                    throw new Error(errorData.message || 'Unknown error occurred');
                });
            }
            return response.json();
        })
        .then(data => {
            // Display success result
            acResult.classList.remove('hidden');
            acResultContent.innerHTML = formatActionCacheResult(data);
            acResultContent.classList.add('success');
            acResultContent.classList.remove('error');
        })
        .catch(error => {
            // Display error result
            acResult.classList.remove('hidden');
            acResultContent.textContent = 'Error: ' + error.message;
            acResultContent.classList.add('error');
            acResultContent.classList.remove('success');
        })
        .finally(() => {
            // Restore button state
            submitButton.innerHTML = originalButtonText;
            submitButton.disabled = false;
        });
    });
    
    // Form submission for CAS
    const casForm = document.getElementById('cas-form');
    const casResult = document.getElementById('cas-result');
    const casResultContent = casResult.querySelector('.result-content');
    
    casForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // Validate form
        if (!validateCASForm()) {
            return;
        }
        
        // Show loading state
        const submitButton = this.querySelector('button[type="submit"]');
        const originalButtonText = submitButton.textContent;
        submitButton.innerHTML = '<span class="spinner"></span> Processing...';
        submitButton.disabled = true;
        
        // Prepare request data
        const formData = new FormData(this);
        const requestData = {
            scope: formData.get('scope'),
            flushFilesystem: formData.get('flushFilesystem') === 'on',
            flushInMemoryLRU: formData.get('flushInMemoryLRU') === 'on',
            flushRedisWorkerMap: formData.get('flushRedisWorkerMap') === 'on'
        };
        
        if (formData.get('scope') === 'INSTANCE') {
            requestData.instanceName = formData.get('instanceName');
        } else if (formData.get('scope') === 'DIGEST_PREFIX') {
            requestData.digestPrefix = formData.get('digestPrefix');
        }
        
        // Send API request
        fetch('/admin/v1/cache/cas/flush', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errorData => {
                    throw new Error(errorData.message || 'Unknown error occurred');
                });
            }
            return response.json();
        })
        .then(data => {
            // Display success result
            casResult.classList.remove('hidden');
            casResultContent.innerHTML = formatCASResult(data);
            casResultContent.classList.add('success');
            casResultContent.classList.remove('error');
        })
        .catch(error => {
            // Display error result
            casResult.classList.remove('hidden');
            casResultContent.textContent = 'Error: ' + error.message;
            casResultContent.classList.add('error');
            casResultContent.classList.remove('success');
        })
        .finally(() => {
            // Restore button state
            submitButton.innerHTML = originalButtonText;
            submitButton.disabled = false;
        });
    });
    
    // Form validation functions
    function validateActionCacheForm() {
        const scope = acScopeSelect.value;
        
        if (scope === 'INSTANCE') {
            const instanceName = document.getElementById('ac-instance').value.trim();
            if (!instanceName) {
                alert('Instance name is required when scope is set to Instance');
                return false;
            }
        } else if (scope === 'DIGEST_PREFIX') {
            const digestPrefix = document.getElementById('ac-digest').value.trim();
            if (!digestPrefix) {
                alert('Digest prefix is required when scope is set to Digest Prefix');
                return false;
            }
        }
        
        const flushRedis = document.getElementById('ac-redis').checked;
        const flushInMemory = document.getElementById('ac-memory').checked;
        
        if (!flushRedis && !flushInMemory) {
            alert('At least one backend must be selected for flushing');
            return false;
        }
        
        return true;
    }
    
    function validateCASForm() {
        const scope = casScopeSelect.value;
        
        if (scope === 'INSTANCE') {
            const instanceName = document.getElementById('cas-instance').value.trim();
            if (!instanceName) {
                alert('Instance name is required when scope is set to Instance');
                return false;
            }
        } else if (scope === 'DIGEST_PREFIX') {
            const digestPrefix = document.getElementById('cas-digest').value.trim();
            if (!digestPrefix) {
                alert('Digest prefix is required when scope is set to Digest Prefix');
                return false;
            }
        }
        
        const flushFilesystem = document.getElementById('cas-filesystem').checked;
        const flushInMemoryLRU = document.getElementById('cas-memory').checked;
        const flushRedisWorkerMap = document.getElementById('cas-redis').checked;
        
        if (!flushFilesystem && !flushInMemoryLRU && !flushRedisWorkerMap) {
            alert('At least one backend must be selected for flushing');
            return false;
        }
        
        return true;
    }
    
    // Result formatting functions
    function formatActionCacheResult(data) {
        let result = '';
        
        if (data.success) {
            result += '<div class="success">✓ Operation completed successfully</div>';
        } else {
            result += '<div class="error">✗ Operation failed: ' + data.message + '</div>';
        }
        
        result += '<p><strong>Entries removed:</strong> ' + data.entriesRemoved + '</p>';
        
        if (data.entriesRemovedByBackend) {
            result += '<p><strong>Entries removed by backend:</strong></p>';
            result += '<ul>';
            for (const [backend, count] of Object.entries(data.entriesRemovedByBackend)) {
                result += '<li>' + backend + ': ' + count + '</li>';
            }
            result += '</ul>';
        }
        
        return result;
    }
    
    function formatCASResult(data) {
        let result = '';
        
        if (data.success) {
            result += '<div class="success">✓ Operation completed successfully</div>';
        } else {
            result += '<div class="error">✗ Operation failed: ' + data.message + '</div>';
        }
        
        result += '<p><strong>Entries removed:</strong> ' + data.entriesRemoved + '</p>';
        result += '<p><strong>Bytes reclaimed:</strong> ' + formatBytes(data.bytesReclaimed) + '</p>';
        
        if (data.entriesRemovedByBackend) {
            result += '<p><strong>Entries removed by backend:</strong></p>';
            result += '<ul>';
            for (const [backend, count] of Object.entries(data.entriesRemovedByBackend)) {
                result += '<li>' + backend + ': ' + count + '</li>';
            }
            result += '</ul>';
        }
        
        if (data.bytesReclaimedByBackend) {
            result += '<p><strong>Bytes reclaimed by backend:</strong></p>';
            result += '<ul>';
            for (const [backend, bytes] of Object.entries(data.bytesReclaimedByBackend)) {
                result += '<li>' + backend + ': ' + formatBytes(bytes) + '</li>';
            }
            result += '</ul>';
        }
        
        return result;
    }
    
    // Utility function to format bytes
    function formatBytes(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
});