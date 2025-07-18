document.addEventListener('DOMContentLoaded', function() {
    // Tab switching functionality
    const navLinks = document.querySelectorAll('.dashboard-nav a');
    const sections = document.querySelectorAll('.dashboard-section');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all links and sections
            navLinks.forEach(navLink => navLink.classList.remove('active'));
            sections.forEach(section => section.classList.remove('active'));
            
            // Add active class to clicked link and corresponding section
            this.classList.add('active');
            const sectionId = this.getAttribute('data-section');
            document.getElementById(sectionId).classList.add('active');
        });
    });
    
    // Handle direct navigation to cache flush page
    const cacheFlushLinks = document.querySelectorAll('a[href^="/admin/ui/cache-flush"]');
    cacheFlushLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // No need to prevent default as we want to navigate to the cache flush page
            // Just track the click for analytics if needed
            console.log('Navigating to cache flush page');
        });
    });
    
    // Fetch mock data for dashboard (in a real implementation, this would call actual API endpoints)
    fetchMockDashboardData();
});

// Function to fetch mock dashboard data
function fetchMockDashboardData() {
    // In a real implementation, these would be API calls to get actual system data
    
    // Mock Action Cache data
    document.getElementById('ac-entries').textContent = '15,432';
    document.getElementById('ac-hit-rate').textContent = '87%';
    document.getElementById('ac-total-entries').textContent = '15,432';
    document.getElementById('ac-stats-hit-rate').textContent = '87%';
    document.getElementById('ac-stats-miss-rate').textContent = '13%';
    document.getElementById('ac-avg-size').textContent = '24.5 KB';
    
    // Mock CAS data
    document.getElementById('cas-entries').textContent = '256,789';
    document.getElementById('cas-storage').textContent = '45.7 GB';
    document.getElementById('cas-total-entries').textContent = '256,789';
    document.getElementById('cas-total-storage').textContent = '45.7 GB';
    document.getElementById('cas-avg-size').textContent = '182.3 KB';
    document.getElementById('cas-utilization').textContent = '78%';
    
    // Mock system resource data
    document.getElementById('cpu-usage').textContent = '42%';
    document.getElementById('memory-usage').textContent = '6.2 GB / 16 GB';
    document.getElementById('disk-usage').textContent = '128.5 GB / 500 GB';
}

// Function to handle hash changes for direct navigation to tabs
window.addEventListener('hashchange', function() {
    const hash = window.location.hash.substring(1); // Remove the # character
    if (hash) {
        const targetSection = document.getElementById(hash);
        if (targetSection) {
            // Remove active class from all sections and nav links
            document.querySelectorAll('.dashboard-section').forEach(section => {
                section.classList.remove('active');
            });
            document.querySelectorAll('.dashboard-nav a').forEach(link => {
                link.classList.remove('active');
            });
            
            // Activate the target section
            targetSection.classList.add('active');
            
            // Activate the corresponding nav link
            const navLink = document.querySelector(`.dashboard-nav a[data-section="${hash}"]`);
            if (navLink) {
                navLink.classList.add('active');
            }
        }
    }
});