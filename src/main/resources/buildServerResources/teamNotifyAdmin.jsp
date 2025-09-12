<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>

<jsp:useBean id="allWebhooks" scope="request" type="java.util.List"/>
<jsp:useBean id="totalWebhooks" scope="request" type="java.lang.Integer"/>

<div class="tn-admin-container">
  <!-- Header Section -->
  <div class="tn-admin-header">
    <div class="tn-admin-header-content">
      <div class="tn-admin-header-title">
        <svg class="tn-icon tn-icon-webhook" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M10 16l-6-6 6-6M14 8l6 6-6 6M8 12h8"/>
        </svg>
        <h2>TeamNotify Global Overview</h2>
      </div>
      <div class="tn-admin-stats">
        <div class="tn-stat-card">
          <div class="tn-stat-value">${totalWebhooks}</div>
          <div class="tn-stat-label">Total Webhooks</div>
        </div>
        <div class="tn-stat-card">
          <div class="tn-stat-value">${fn:length(allWebhooks)}</div>
          <div class="tn-stat-label">Active Projects</div>
        </div>
        <div class="tn-admin-actions">
          <button class="tn-backup-btn" onclick="makeBackup()">
            <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
            Backup
          </button>
          <button class="tn-restore-btn" onclick="document.getElementById('restoreFile').click()">
            <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
            Restore
          </button>
          <input type="file" id="restoreFile" accept=".json" style="display: none;" onchange="restoreBackup(this)">
        </div>
      </div>
    </div>
  </div>

  <c:choose>
    <c:when test="${totalWebhooks == 0}">
      <div class="tn-admin-empty">
        <svg viewBox="0 0 24 24" class="tn-empty-icon" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
        </svg>
        <h3>No Webhooks Configured</h3>
        <p>No webhook notifications are currently configured in any project.</p>
        <p>Configure webhooks at the project or build configuration level to enable notifications.</p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="tn-admin-table-container">
        <table class="tn-admin-table">
          <thead>
            <tr>
              <th width="20%">Project</th>
              <th width="35%">Webhook</th>
              <th width="10%">Platform</th>
              <th width="25%">Triggers</th>
              <th width="10%">Actions</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="webhookInfo" items="${allWebhooks}">
              <tr class="tn-admin-row ${!webhookInfo.webhook.enabled ? 'tn-webhook-disabled' : ''}">
                <td class="tn-project-cell">
                  <div class="tn-project-info">
                    <div class="tn-project-name">${fn:escapeXml(webhookInfo.projectName)}</div>
                    <div class="tn-project-id">${fn:escapeXml(webhookInfo.projectId)}</div>
                  </div>
                </td>
                <td class="tn-webhook-cell">
                  <div class="tn-webhook-url">
                    <c:choose>
                      <c:when test="${fn:length(webhookInfo.webhook.url) > 50}">
                        <span title="${fn:escapeXml(webhookInfo.webhook.url)}">
                          ${fn:escapeXml(fn:substring(webhookInfo.webhook.url, 0, 30))}...${fn:escapeXml(fn:substring(webhookInfo.webhook.url, fn:length(webhookInfo.webhook.url) - 15, fn:length(webhookInfo.webhook.url)))}
                        </span>
                      </c:when>
                      <c:otherwise>
                        ${fn:escapeXml(webhookInfo.webhook.url)}
                      </c:otherwise>
                    </c:choose>
                  </div>
                </td>
                <td class="tn-platform-cell">
                  <span class="tn-platform-badge tn-platform-${fn:toLowerCase(webhookInfo.webhook.platform)}">
                    ${webhookInfo.webhook.platform}
                  </span>
                </td>
                <td class="tn-triggers-cell">
                  <div class="tn-triggers-grid">
                    <c:if test="${webhookInfo.webhook.onStart}">
                      <span class="tn-trigger-tag">On Start</span>
                    </c:if>
                    <c:if test="${webhookInfo.webhook.onSuccess}">
                      <span class="tn-trigger-tag tn-trigger-tag-success">On Success</span>
                    </c:if>
                    <c:if test="${webhookInfo.webhook.onFailure}">
                      <span class="tn-trigger-tag tn-trigger-tag-failure">On Failure</span>
                    </c:if>
                    <c:if test="${webhookInfo.webhook.onStall}">
                      <span class="tn-trigger-tag tn-trigger-tag-warning">On Stall</span>
                    </c:if>
                    <c:if test="${webhookInfo.webhook.buildLongerThan != null}">
                      <span class="tn-trigger-tag tn-trigger-tag-info">&gt; ${webhookInfo.webhook.buildLongerThan}s</span>
                    </c:if>
                    <c:if test="${webhookInfo.webhook.buildLongerThanAverage}">
                      <span class="tn-trigger-tag tn-trigger-tag-info">&gt; Average</span>
                    </c:if>
                    <c:if test="${webhookInfo.webhook.onFirstFailure}">
                      <span class="tn-trigger-tag tn-trigger-tag-error">First Failure</span>
                    </c:if>
                    <c:if test="${webhookInfo.webhook.onBuildFixed}">
                      <span class="tn-trigger-tag tn-trigger-tag-fixed">Build Fixed</span>
                    </c:if>
                  </div>
                  <c:if test="${webhookInfo.webhook.branchFilter != null}">
                    <div class="tn-branch-filter">
                      <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M7 2a2 2 0 00-2 2v5a2 2 0 002 2h6a2 2 0 002-2V4a2 2 0 00-2-2H7zM3 9V4a4 4 0 014-4h6a4 4 0 014 4v5a4 4 0 01-3 3.874V16a2 2 0 01-2 2H8a2 2 0 01-2-2v-3.126A4 4 0 013 9z"/>
                      </svg>
                      ${fn:escapeXml(webhookInfo.webhook.branchFilter)}
                    </div>
                  </c:if>
                </td>
                <td class="tn-actions-cell">
                  <div class="tn-actions-group">
                    <button onclick="toggleWebhook('${fn:escapeXml(webhookInfo.webhook.url)}', '${fn:escapeXml(webhookInfo.projectId)}', '${fn:escapeXml(webhookInfo.buildTypeId)}', ${webhookInfo.webhook.enabled})" 
                            class="tn-toggle-btn ${webhookInfo.webhook.enabled ? 'tn-toggle-enabled' : 'tn-toggle-disabled'}"
                            title="${webhookInfo.webhook.enabled ? 'Disable' : 'Enable'} webhook">
                      <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
                        <c:choose>
                          <c:when test="${webhookInfo.webhook.enabled}">
                            <!-- Lightbulb on -->
                            <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z"/>
                          </c:when>
                          <c:otherwise>
                            <!-- Lightbulb off -->
                            <path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clip-rule="evenodd"/>
                          </c:otherwise>
                        </c:choose>
                      </svg>
                    </button>
                    <button onclick="deleteWebhook('${fn:escapeXml(webhookInfo.webhook.url)}', '${fn:escapeXml(webhookInfo.projectId)}', '${fn:escapeXml(webhookInfo.buildTypeId)}')" 
                            class="tn-delete-btn"
                            title="Delete webhook">
                      <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </c:otherwise>
  </c:choose>
</div>

<!-- Toast Notification Container -->
<div id="tn-toast-container" style="position: fixed; top: 20px; right: 20px; z-index: 10000;"></div>

<script>
// Toast notification system
function showToast(message, type = 'info') {
  const toastContainer = document.getElementById('tn-toast-container');
  const toast = document.createElement('div');
  
  const styles = {
    success: 'background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white;',
    error: 'background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white;',
    warning: 'background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white;',
    info: 'background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); color: white;'
  };
  
  toast.style.cssText = (styles[type] || styles.info) + 
    'padding: 16px 24px; border-radius: 8px; margin-bottom: 10px; ' +
    'box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); font-size: 14px; ' +
    'font-weight: 500; display: flex; align-items: center; gap: 12px; ' +
    'animation: slideIn 0.3s ease-out; min-width: 300px; max-width: 500px;';
  
  toast.innerHTML = '<span>' + message + '</span>';
  toastContainer.appendChild(toast);
  
  setTimeout(() => {
    toast.style.animation = 'slideOut 0.3s ease-in';
    setTimeout(() => toast.remove(), 300);
  }, 5000);
}

// Add CSS animations
if (!document.getElementById('tn-toast-styles')) {
  const style = document.createElement('style');
  style.id = 'tn-toast-styles';
  style.textContent = '@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } } @keyframes slideOut { from { transform: translateX(0); opacity: 1; } to { transform: translateX(100%); opacity: 0; } }';
  document.head.appendChild(style);
}

// Backup function
async function makeBackup() {
  try {
    const response = await fetch('<c:url value="/admin/teamnotify/backup.html"/>');
    
    if (!response.ok) {
      throw new Error('Backup failed');
    }
    
    // Get filename from Content-Disposition header
    const contentDisposition = response.headers.get('Content-Disposition');
    const filename = contentDisposition 
      ? contentDisposition.split('filename="')[1].split('"')[0]
      : 'teamnotify_backup.json';
    
    // Download the file
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    
    showToast('Backup created successfully!', 'success');
  } catch (error) {
    showToast('Failed to create backup: ' + error.message, 'error');
  }
}

// Restore function
async function restoreBackup(input) {
  const file = input.files[0];
  if (!file) return;
  
  if (!file.name.endsWith('.json')) {
    showToast('Please select a valid JSON backup file', 'error');
    input.value = '';
    return;
  }
  
  try {
    const fileContent = await file.text();
    
    // Validate JSON
    let backupData;
    try {
      backupData = JSON.parse(fileContent);
    } catch (e) {
      showToast('Invalid backup file format', 'error');
      input.value = '';
      return;
    }
    
    // Confirm restore
    const webhookCount = backupData.webhooks ? backupData.webhooks.length : 0;
    if (!confirm('This will restore ' + webhookCount + ' webhook(s). Existing webhooks with the same URLs will be skipped. Continue?')) {
      input.value = '';
      return;
    }
    
    // Send restore request
    const response = await fetch('<c:url value="/admin/teamnotify/restore.html"/>', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: fileContent
    });
    
    const result = await response.json();
    
    if (result.success) {
      let message = 'Restore completed: ' + result.restored + ' webhook(s) restored';
      if (result.skipped > 0) {
        message += ', ' + result.skipped + ' skipped (duplicates or missing projects)';
      }
      showToast(message, 'success');
      
      // Reload page after successful restore
      setTimeout(() => location.reload(), 2000);
    } else {
      showToast('Restore failed: ' + (result.error || 'Unknown error'), 'error');
    }
    
  } catch (error) {
    showToast('Failed to restore backup: ' + error.message, 'error');
  } finally {
    input.value = '';
  }
}

// Delete webhook function
async function deleteWebhook(webhookUrl, projectId, buildTypeId) {
  if (!confirm('Are you sure you want to delete this webhook?')) {
    return;
  }
  
  try {
    const params = new URLSearchParams();
    params.set('action', 'delete');
    params.set('webhookUrl', webhookUrl);
    if (projectId && projectId !== 'null') params.set('projectId', projectId);
    if (buildTypeId && buildTypeId !== 'null') params.set('buildTypeId', buildTypeId);
    
    const response = await fetch('<c:url value="/admin/teamnotify/api.html"/>', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: params
    });
    
    const result = await response.json();
    
    if (result.success) {
      showToast('Webhook deleted successfully', 'success');
      setTimeout(() => location.reload(), 1000);
    } else {
      showToast('Failed to delete webhook: ' + (result.error || 'Unknown error'), 'error');
    }
  } catch (error) {
    showToast('Error deleting webhook: ' + error.message, 'error');
  }
}

// Toggle webhook function
async function toggleWebhook(webhookUrl, projectId, buildTypeId, currentStatus) {
  try {
    const params = new URLSearchParams();
    params.set('action', 'toggle');
    params.set('webhookUrl', webhookUrl);
    if (projectId && projectId !== 'null') params.set('projectId', projectId);
    if (buildTypeId && buildTypeId !== 'null') params.set('buildTypeId', buildTypeId);
    
    const response = await fetch('<c:url value="/admin/teamnotify/api.html"/>', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: params
    });
    
    const result = await response.json();
    
    if (result.success) {
      const newStatus = result.enabled ? 'enabled' : 'disabled';
      showToast('Webhook ' + newStatus + ' successfully', 'success');
      setTimeout(() => location.reload(), 1000);
    } else {
      showToast('Failed to toggle webhook: ' + (result.error || 'Unknown error'), 'error');
    }
  } catch (error) {
    showToast('Error toggling webhook: ' + error.message, 'error');
  }
}
</script>

<style>
  .tn-admin-container {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    max-width: 100%;
    margin: 0;
    background: #f8f9fa;
    border-radius: 8px;
    overflow: hidden;
  }

  /* Header */
  .tn-admin-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 30px;
    color: white;
  }

  .tn-admin-header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .tn-admin-header-title {
    display: flex;
    align-items: center;
    gap: 15px;
  }

  .tn-admin-header-title h2 {
    margin: 0;
    font-size: 28px;
    font-weight: 600;
    color: white;
  }

  .tn-icon-webhook {
    width: 32px;
    height: 32px;
  }

  /* Stats Cards */
  .tn-admin-stats {
    display: flex;
    gap: 20px;
    align-items: center;
  }
  
  /* Admin Actions */
  .tn-admin-actions {
    display: flex;
    gap: 10px;
    margin-left: auto;
  }
  
  .tn-backup-btn,
  .tn-restore-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);
    border: 2px solid rgba(255, 255, 255, 0.3);
    color: white;
    font-size: 14px;
    font-weight: 600;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .tn-backup-btn:hover,
  .tn-restore-btn:hover {
    background: rgba(255, 255, 255, 0.3);
    border-color: rgba(255, 255, 255, 0.5);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
  
  .tn-backup-btn:active,
  .tn-restore-btn:active {
    transform: translateY(0);
  }

  .tn-stat-card {
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);
    padding: 15px 25px;
    border-radius: 12px;
    text-align: center;
    min-width: 120px;
  }

  .tn-stat-value {
    font-size: 32px;
    font-weight: bold;
    color: white;
    line-height: 1;
  }

  .tn-stat-label {
    font-size: 12px;
    margin-top: 5px;
    opacity: 0.9;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  /* Empty State */
  .tn-admin-empty {
    padding: 80px 20px;
    text-align: center;
    background: white;
    margin: 20px;
    border-radius: 12px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.06);
  }

  .tn-empty-icon {
    width: 64px;
    height: 64px;
    margin: 0 auto 20px;
    color: #cbd5e0;
  }

  .tn-admin-empty h3 {
    color: #2d3748;
    font-size: 24px;
    margin: 0 0 10px;
  }

  .tn-admin-empty p {
    color: #718096;
    font-size: 14px;
    margin: 5px 0;
  }

  /* Table */
  .tn-admin-table-container {
    margin: 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.06);
    overflow: hidden;
  }

  .tn-admin-table {
    width: 100%;
    border-collapse: collapse;
  }

  .tn-admin-table thead {
    background: linear-gradient(135deg, #f6f8fb 0%, #e9ecef 100%);
  }

  .tn-admin-table th {
    padding: 16px 20px;
    text-align: left;
    font-weight: 600;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: #4a5568;
    border-bottom: 2px solid #e2e8f0;
  }

  .tn-admin-row {
    transition: background-color 0.2s;
    border-bottom: 1px solid #e2e8f0;
  }

  .tn-admin-row:hover {
    background-color: #f7fafc;
  }
  
  .tn-admin-row.tn-webhook-disabled {
    opacity: 0.6;
  }
  
  .tn-admin-row.tn-webhook-disabled .tn-webhook-url {
    text-decoration: line-through;
  }

  .tn-admin-table td {
    padding: 16px 20px;
    vertical-align: middle;
  }

  /* Project Cell */
  .tn-project-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .tn-project-name {
    font-weight: 600;
    color: #2d3748;
    font-size: 14px;
  }

  .tn-project-id {
    font-size: 11px;
    color: #718096;
  }

  /* Webhook Cell */
  .tn-webhook-url {
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 12px;
    color: #4a5568;
    word-break: break-all;
  }

  /* Platform Badge */
  .tn-platform-badge {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .tn-platform-slack {
    background: linear-gradient(135deg, #4a154b 0%, #611f69 100%);
    color: white;
  }

  .tn-platform-teams {
    background: linear-gradient(135deg, #5059c9 0%, #7b83eb 100%);
    color: white;
  }

  .tn-platform-discord {
    background: linear-gradient(135deg, #5865f2 0%, #7289da 100%);
    color: white;
  }

  /* Triggers */
  .tn-triggers-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-bottom: 4px;
  }

  .tn-trigger-tag {
    display: inline-block;
    padding: 3px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 500;
    background: #e2e8f0;
    color: #4a5568;
  }

  .tn-trigger-tag-success {
    background: #c6f6d5;
    color: #22543d;
  }

  .tn-trigger-tag-failure {
    background: #fed7d7;
    color: #742a2a;
  }

  .tn-trigger-tag-warning {
    background: #feebc8;
    color: #744210;
  }

  .tn-trigger-tag-info {
    background: #bee3f8;
    color: #2c5282;
  }

  .tn-trigger-tag-error {
    background: #fc8181;
    color: white;
  }

  .tn-trigger-tag-fixed {
    background: #d6f5d6;
    color: #276749;
  }

  .tn-branch-filter {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 4px;
    font-size: 11px;
    color: #718096;
    font-style: italic;
  }

  .tn-branch-filter svg {
    opacity: 0.6;
  }

  /* Action Buttons */
  .tn-actions-group {
    display: flex;
    gap: 8px;
    align-items: center;
  }
  
  .tn-toggle-btn,
  .tn-delete-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .tn-toggle-btn.tn-toggle-enabled {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
  }
  
  .tn-toggle-btn.tn-toggle-enabled:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(16, 185, 129, 0.3);
  }
  
  .tn-toggle-btn.tn-toggle-disabled {
    background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
    color: white;
  }
  
  .tn-toggle-btn.tn-toggle-disabled:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(107, 114, 128, 0.3);
  }
  
  .tn-delete-btn {
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    color: white;
  }
  
  .tn-delete-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(239, 68, 68, 0.3);
  }
  
  .tn-toggle-btn:active,
  .tn-delete-btn:active {
    transform: translateY(0);
  }

  /* Responsive */
  @media (max-width: 1200px) {
    .tn-admin-header-content {
      flex-direction: column;
      gap: 20px;
      align-items: flex-start;
    }
  }

  @media (max-width: 768px) {
    .tn-admin-table {
      font-size: 12px;
    }
    
    .tn-admin-table th,
    .tn-admin-table td {
      padding: 10px;
    }
    
    .tn-trigger-tag {
      font-size: 10px;
      padding: 2px 6px;
    }
  }
</style>