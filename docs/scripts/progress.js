/**
 * Progress Indicator
 * Shows current page number and progress bar
 */

(function() {
  'use strict';

  // Page configuration
  const PAGES = [
    { id: 'index', title: 'Home', number: 0 },
    { id: 'page-01-title', title: 'Introduction', number: 1 },
    { id: 'page-02-monitoring-gap', title: 'The Monitoring Gap', number: 2 },
    { id: 'page-03-three-pillars', title: 'The Three Pillars', number: 3 },
    { id: 'page-04-stack', title: 'Our Observability Stack', number: 4 },
    { id: 'page-05-logs-elk', title: 'Logs with ELK Stack', number: 5 },
    { id: 'page-06-elk-action', title: 'ELK in Action', number: 6 },
    { id: 'page-07-metrics', title: 'Metrics with Prometheus', number: 7 },
    { id: 'page-08-custom-metrics', title: 'Custom Metrics', number: 8 },
    { id: 'page-09-grafana', title: 'Grafana Visualization', number: 9 },
    { id: 'page-10-tracing-mdc', title: 'Distributed Tracing with MDC', number: 10 },
    { id: 'page-11-consul', title: 'Service Discovery with Consul', number: 11 },
    { id: 'page-12-complete-flow', title: 'The Complete Flow', number: 12 },
    { id: 'page-13-debugging', title: 'Real-World Debugging', number: 13 },
    { id: 'page-14-best-practices', title: 'Best Practices', number: 14 },
    { id: 'page-15-resources', title: 'Getting Started & Resources', number: 15 }
  ];

  const TOTAL_PAGES = 15; // Excluding index

  // Get current page info
  function getCurrentPageInfo() {
    const currentPath = window.location.pathname.split('/').pop() || 'index.html';
    const currentFile = currentPath.replace('.html', '');

    const pageInfo = PAGES.find(page => page.id === currentFile);
    return pageInfo || PAGES[0];
  }

  // Update progress indicator
  function updateProgressIndicator() {
    const progressText = document.getElementById('progress-text');
    const progressBarFill = document.getElementById('progress-bar-fill');

    const currentPage = getCurrentPageInfo();

    // Don't show progress on index page
    if (currentPage.number === 0) {
      if (progressText) progressText.style.display = 'none';
      if (progressBarFill) progressBarFill.parentElement.style.display = 'none';
      return;
    }

    // Update progress text
    if (progressText) {
      progressText.textContent = `Page ${currentPage.number} of ${TOTAL_PAGES}`;
      progressText.style.display = 'block';
    }

    // Update progress bar
    if (progressBarFill) {
      const percentage = (currentPage.number / TOTAL_PAGES) * 100;
      progressBarFill.style.width = `${percentage}%`;
      progressBarFill.parentElement.style.display = 'block';
    }
  }

  // Get navigation info (previous and next pages)
  function getNavigationInfo() {
    const currentPage = getCurrentPageInfo();
    const currentIndex = PAGES.findIndex(page => page.id === currentPage.id);

    const prevPage = currentIndex > 0 ? PAGES[currentIndex - 1] : null;
    const nextPage = currentIndex < PAGES.length - 1 ? PAGES[currentIndex + 1] : null;

    return { prevPage, nextPage };
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      updateProgressIndicator();
    });
  } else {
    updateProgressIndicator();
  }

  // Export functions for use in HTML
  window.getCurrentPageInfo = getCurrentPageInfo;
  window.getNavigationInfo = getNavigationInfo;
  window.PAGES = PAGES;
})();
