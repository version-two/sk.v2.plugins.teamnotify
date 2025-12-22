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
      <div class="tn-admin-header-left">
        <div class="tn-admin-logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
        </div>
        <div class="tn-admin-header-text">
          <h1>TeamNotify</h1>
          <p>Webhook notifications for Slack, Teams & Discord</p>
        </div>
      </div>
      <div class="tn-admin-header-right">
        <div class="tn-admin-stats">
          <div class="tn-stat-card">
            <span class="tn-stat-value">${totalWebhooks}</span>
            <span class="tn-stat-label">Total Webhooks</span>
          </div>
          <div class="tn-stat-card">
            <span class="tn-stat-value">${fn:length(allWebhooks)}</span>
            <span class="tn-stat-label">Projects</span>
          </div>
        </div>
        <div class="tn-admin-actions">
          <button class="tn-action-btn tn-action-btn-secondary" onclick="makeBackup()" title="Download backup">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
            <span>Backup</span>
          </button>
          <button class="tn-action-btn tn-action-btn-secondary" onclick="document.getElementById('restoreFile').click()" title="Restore from backup">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
            <span>Restore</span>
          </button>
          <input type="file" id="restoreFile" accept=".json" style="display: none;" onchange="restoreBackup(this)">
        </div>
      </div>
    </div>
  </div>

  <!-- Main Content -->
  <div class="tn-admin-content">
    <c:choose>
      <c:when test="${totalWebhooks == 0}">
        <div class="tn-empty-state">
          <div class="tn-empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              <line x1="12" y1="2" x2="12" y2="4"/>
            </svg>
          </div>
          <h2>No Webhooks Configured</h2>
          <p>Get started by adding webhook notifications to your projects or build configurations.</p>
          <div class="tn-empty-hint">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/>
            </svg>
            Navigate to a Project or Build Configuration and open the TeamNotify tab to add webhooks.
          </div>
        </div>
      </c:when>
      <c:otherwise>
        <!-- Webhooks Grid -->
        <div class="tn-webhooks-grid">
          <c:forEach var="webhookInfo" items="${allWebhooks}" varStatus="status">
            <div class="tn-webhook-card ${!webhookInfo.webhook.enabled ? 'tn-webhook-card-disabled' : ''}">
              <!-- Card Header -->
              <div class="tn-webhook-card-header">
                <div class="tn-webhook-platform-badge tn-platform-${fn:toLowerCase(webhookInfo.webhook.platform)}">
                  <c:choose>
                    <c:when test="${webhookInfo.webhook.platform == 'SLACK'}">
                      <svg viewBox="0 0 24 24">
                        <path fill="currentColor" d="M5.042 15.165a2.528 2.528 0 0 1-2.52 2.523A2.528 2.528 0 0 1 0 15.165a2.527 2.527 0 0 1 2.522-2.52h2.52v2.52zM6.313 15.165a2.527 2.527 0 0 1 2.521-2.52 2.527 2.527 0 0 1 2.521 2.52v6.313A2.528 2.528 0 0 1 8.834 24a2.528 2.528 0 0 1-2.521-2.522v-6.313zM8.834 5.042a2.528 2.528 0 0 1-2.521-2.52A2.528 2.528 0 0 1 8.834 0a2.528 2.528 0 0 1 2.521 2.522v2.52H8.834zM8.834 6.313a2.528 2.528 0 0 1 2.521 2.521 2.528 2.528 0 0 1-2.521 2.521H2.522A2.528 2.528 0 0 1 0 8.834a2.528 2.528 0 0 1 2.522-2.521h6.312zM18.956 8.834a2.528 2.528 0 0 1 2.522-2.521A2.528 2.528 0 0 1 24 8.834a2.528 2.528 0 0 1-2.522 2.521h-2.522V8.834zM17.688 8.834a2.528 2.528 0 0 1-2.523 2.521 2.527 2.527 0 0 1-2.52-2.521V2.522A2.527 2.527 0 0 1 15.165 0a2.528 2.528 0 0 1 2.523 2.522v6.312zM15.165 18.956a2.528 2.528 0 0 1 2.523 2.522A2.528 2.528 0 0 1 15.165 24a2.527 2.527 0 0 1-2.52-2.522v-2.522h2.52zM15.165 17.688a2.527 2.527 0 0 1-2.52-2.523 2.526 2.526 0 0 1 2.52-2.52h6.313A2.527 2.527 0 0 1 24 15.165a2.528 2.528 0 0 1-2.522 2.523h-6.313z"/>
                      </svg>
                    </c:when>
                    <c:when test="${webhookInfo.webhook.platform == 'TEAMS'}">
                      <svg viewBox="0 0 16 16">
                        <path fill="currentColor" d="M9.186 4.797a2.42 2.42 0 1 0-2.86-2.448h1.178c.929 0 1.682.753 1.682 1.682zm-4.295 7.738h2.613c.929 0 1.682-.753 1.682-1.682V5.58h2.783a.7.7 0 0 1 .682.716v4.294a4.197 4.197 0 0 1-4.093 4.293c-1.618-.04-3-.99-3.667-2.35Zm10.737-9.372a1.674 1.674 0 1 1-3.349 0 1.674 1.674 0 0 1 3.349 0m-2.238 9.488-.12-.002a5.2 5.2 0 0 0 .381-2.07V6.306a1.7 1.7 0 0 0-.15-.725h1.792c.39 0 .707.317.707.707v3.765a2.6 2.6 0 0 1-2.598 2.598z"/>
                        <path fill="currentColor" d="M.682 3.349h6.822c.377 0 .682.305.682.682v6.822a.68.68 0 0 1-.682.682H.682A.68.68 0 0 1 0 10.853V4.03c0-.377.305-.682.682-.682Zm5.206 2.596v-.72h-3.59v.72h1.357V9.66h.87V5.945z"/>
                      </svg>
                    </c:when>
                    <c:otherwise>
                      <svg viewBox="0 0 24 24">
                        <path fill="currentColor" d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z"/>
                      </svg>
                    </c:otherwise>
                  </c:choose>
                  <span>${webhookInfo.webhook.platform}</span>
                </div>
                <c:if test="${!webhookInfo.webhook.enabled}">
                  <span class="tn-disabled-badge">Disabled</span>
                </c:if>
              </div>

              <!-- Project Info -->
              <div class="tn-webhook-project">
                <div class="tn-project-icon">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z"/>
                  </svg>
                </div>
                <div class="tn-project-details">
                  <span class="tn-project-name">${fn:escapeXml(webhookInfo.projectName)}</span>
                  <span class="tn-project-id">${fn:escapeXml(webhookInfo.projectId)}</span>
                </div>
              </div>

              <!-- Triggers Section -->
              <div class="tn-webhook-triggers-section">
                <span class="tn-triggers-label">Triggers</span>
                <div class="tn-triggers-list">
                  <c:if test="${webhookInfo.webhook.onStart}">
                    <span class="tn-trigger tn-trigger-neutral">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd"/></svg>
                      Start
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onSuccess}">
                    <span class="tn-trigger tn-trigger-success">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
                      Success
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onFailure}">
                    <span class="tn-trigger tn-trigger-failure">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/></svg>
                      Failure
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onCancel}">
                    <span class="tn-trigger tn-trigger-cancel">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/></svg>
                      Cancel
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onStall}">
                    <span class="tn-trigger tn-trigger-warning">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/></svg>
                      Stall
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onFirstFailure}">
                    <span class="tn-trigger tn-trigger-error">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/></svg>
                      1st Fail
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onBuildFixed}">
                    <span class="tn-trigger tn-trigger-fixed">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
                      Fixed
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.buildLongerThan != null}">
                    <span class="tn-trigger tn-trigger-info">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/></svg>
                      &gt;${webhookInfo.webhook.buildLongerThan}s
                    </span>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.buildLongerThanAverage}">
                    <span class="tn-trigger tn-trigger-info">
                      <svg viewBox="0 0 20 20" fill="currentColor"><path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/></svg>
                      &gt;Avg
                    </span>
                  </c:if>
                </div>
                <c:if test="${webhookInfo.webhook.branchFilter != null}">
                  <div class="tn-branch-filter">
                    <svg viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M7.707 3.293a1 1 0 010 1.414L5.414 7H11a7 7 0 017 7v2a1 1 0 11-2 0v-2a5 5 0 00-5-5H5.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                    <span title="${fn:escapeXml(webhookInfo.webhook.branchFilter)}">${fn:escapeXml(webhookInfo.webhook.branchFilter)}</span>
                  </div>
                </c:if>
              </div>

              <!-- Card Actions -->
              <div class="tn-webhook-card-actions">
                <button onclick="toggleWebhook(${status.index}, '${fn:escapeXml(webhookInfo.projectId)}', '${fn:escapeXml(webhookInfo.buildTypeId)}', ${webhookInfo.webhook.enabled})"
                        class="tn-card-action-btn ${webhookInfo.webhook.enabled ? 'tn-action-enabled' : 'tn-action-disabled'}"
                        title="${webhookInfo.webhook.enabled ? 'Disable' : 'Enable'} webhook">
                  <c:choose>
                    <c:when test="${webhookInfo.webhook.enabled}">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z"/>
                      </svg>
                    </c:when>
                    <c:otherwise>
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clip-rule="evenodd"/>
                      </svg>
                    </c:otherwise>
                  </c:choose>
                </button>
                <button onclick="deleteWebhook(${status.index}, '${fn:escapeXml(webhookInfo.projectId)}', '${fn:escapeXml(webhookInfo.buildTypeId)}')"
                        class="tn-card-action-btn tn-action-delete"
                        title="Delete webhook">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
                  </svg>
                </button>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<!-- Toast Notification Container -->
<div id="tn-toast-container"></div>

<script>
// Toast notification system
function showToast(message, type) {
  type = type || 'info';
  var container = document.getElementById('tn-toast-container');
  var toast = document.createElement('div');
  toast.className = 'tn-toast tn-toast-' + type;

  var icons = {
    success: '<svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>',
    error: '<svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/></svg>',
    warning: '<svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/></svg>',
    info: '<svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/></svg>'
  };

  toast.innerHTML = (icons[type] || icons.info) + '<span>' + message + '</span>';
  container.appendChild(toast);

  setTimeout(function() {
    toast.classList.add('tn-toast-out');
    setTimeout(function() { toast.remove(); }, 300);
  }, 5000);
}

// Backup function
async function makeBackup() {
  try {
    var response = await fetch('<c:url value="/admin/teamnotify/backup.html"/>');
    if (!response.ok) throw new Error('Backup failed');

    var contentDisposition = response.headers.get('Content-Disposition');
    var filename = contentDisposition
      ? contentDisposition.split('filename="')[1].split('"')[0]
      : 'teamnotify_backup.json';

    var blob = await response.blob();
    var url = window.URL.createObjectURL(blob);
    var a = document.createElement('a');
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
  var file = input.files[0];
  if (!file) return;

  if (!file.name.endsWith('.json')) {
    showToast('Please select a valid JSON backup file', 'error');
    input.value = '';
    return;
  }

  try {
    var fileContent = await file.text();
    var backupData;
    try {
      backupData = JSON.parse(fileContent);
    } catch (e) {
      showToast('Invalid backup file format', 'error');
      input.value = '';
      return;
    }

    var webhookCount = backupData.webhooks ? backupData.webhooks.length : 0;
    if (!confirm('This will restore ' + webhookCount + ' webhook(s). Existing webhooks with the same URLs will be skipped. Continue?')) {
      input.value = '';
      return;
    }

    var response = await fetch('<c:url value="/admin/teamnotify/restore.html"/>', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-TC-CSRF-Token': getCsrfToken()
      },
      body: fileContent
    });

    var result = await response.json();

    if (result.success) {
      var message = 'Restore completed: ' + result.restored + ' webhook(s) restored';
      if (result.skipped > 0) {
        message += ', ' + result.skipped + ' skipped';
      }
      showToast(message, 'success');
      setTimeout(function() { location.reload(); }, 2000);
    } else {
      showToast('Restore failed: ' + (result.error || 'Unknown error'), 'error');
    }
  } catch (error) {
    showToast('Failed to restore backup: ' + error.message, 'error');
  } finally {
    input.value = '';
  }
}

// Get CSRF token
function getCsrfToken() {
  var tokenMeta = document.querySelector('meta[name="tc-csrf-token"]');
  if (tokenMeta) return tokenMeta.getAttribute('content');
  var tokenInput = document.querySelector('input[name="tc-csrf-token"]');
  if (tokenInput) return tokenInput.value;
  if (window.BS && window.BS.CSRFToken) return window.BS.CSRFToken;
  var forms = document.querySelectorAll('form');
  for (var i = 0; i < forms.length; i++) {
    var input = forms[i].querySelector('input[name="tc-csrf-token"]');
    if (input) return input.value;
  }
  return '';
}

// Delete webhook - uses index only, URL resolved server-side
async function deleteWebhook(webhookIndex, projectId, buildTypeId) {
  if (!confirm('Are you sure you want to delete this webhook?')) return;

  try {
    var params = new URLSearchParams();
    params.set('action', 'delete');
    params.set('webhookIndex', webhookIndex);
    if (projectId && projectId !== 'null') params.set('projectId', projectId);
    if (buildTypeId && buildTypeId !== 'null') params.set('buildTypeId', buildTypeId);
    params.set('tc-csrf-token', getCsrfToken());

    var response = await fetch('<c:url value="/admin/teamnotify/api.html"/>', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    });

    var result = await response.json();

    if (result.success) {
      showToast('Webhook deleted successfully', 'success');
      setTimeout(function() { location.reload(); }, 1000);
    } else {
      showToast('Failed to delete webhook: ' + (result.error || 'Unknown error'), 'error');
    }
  } catch (error) {
    showToast('Error deleting webhook: ' + error.message, 'error');
  }
}

// Toggle webhook - uses index only, URL resolved server-side
async function toggleWebhook(webhookIndex, projectId, buildTypeId, currentStatus) {
  try {
    var params = new URLSearchParams();
    params.set('action', 'toggle');
    params.set('webhookIndex', webhookIndex);
    if (projectId && projectId !== 'null') params.set('projectId', projectId);
    if (buildTypeId && buildTypeId !== 'null') params.set('buildTypeId', buildTypeId);
    params.set('tc-csrf-token', getCsrfToken());

    var response = await fetch('<c:url value="/admin/teamnotify/api.html"/>', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    });

    var result = await response.json();

    if (result.success) {
      var newStatus = result.enabled ? 'enabled' : 'disabled';
      showToast('Webhook ' + newStatus + ' successfully', 'success');
      setTimeout(function() { location.reload(); }, 1000);
    } else {
      showToast('Failed to toggle webhook: ' + (result.error || 'Unknown error'), 'error');
    }
  } catch (error) {
    showToast('Error toggling webhook: ' + error.message, 'error');
  }
}
</script>

<style>
/* Reset & Base */
.tn-admin-container {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f3f4f6;
  min-height: 100vh;
  padding: 24px;
  box-sizing: border-box;
}

.tn-admin-container * {
  box-sizing: border-box;
}

/* Header */
.tn-admin-header {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%);
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 32px;
  box-shadow: 0 10px 40px -10px rgba(99, 102, 241, 0.5);
}

.tn-admin-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
}

.tn-admin-header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.tn-admin-logo {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.tn-admin-logo svg {
  width: 32px;
  height: 32px;
  color: white;
}

.tn-admin-header-text h1 {
  margin: 0 0 4px 0;
  font-size: 28px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.5px;
}

.tn-admin-header-text p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.tn-admin-header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.tn-admin-stats {
  display: flex;
  gap: 16px;
}

.tn-stat-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 16px 24px;
  text-align: center;
  min-width: 100px;
}

.tn-stat-value {
  display: block;
  font-size: 32px;
  font-weight: 700;
  color: white;
  line-height: 1;
}

.tn-stat-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tn-admin-actions {
  display: flex;
  gap: 12px;
}

.tn-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.tn-action-btn svg {
  width: 18px;
  height: 18px;
}

.tn-action-btn-secondary {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
}

.tn-action-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* Content */
.tn-admin-content {
  max-width: 1400px;
  margin: 0 auto;
}

/* Empty State */
.tn-empty-state {
  background: white;
  border-radius: 16px;
  padding: 80px 40px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tn-empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tn-empty-icon svg {
  width: 40px;
  height: 40px;
  color: #9ca3af;
}

.tn-empty-state h2 {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.tn-empty-state p {
  margin: 0 0 24px 0;
  font-size: 16px;
  color: #6b7280;
}

.tn-empty-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f3f4f6;
  border-radius: 8px;
  font-size: 14px;
  color: #4b5563;
}

.tn-empty-hint svg {
  width: 18px;
  height: 18px;
  color: #6366f1;
  flex-shrink: 0;
}

/* Webhooks Grid */
.tn-webhooks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}

/* Webhook Card */
.tn-webhook-card {
  background: white;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tn-webhook-card:hover {
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.tn-webhook-card-disabled {
  opacity: 0.6;
  background: #f9fafb;
}

/* Card Header */
.tn-webhook-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tn-webhook-platform-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tn-webhook-platform-badge svg {
  width: 18px;
  height: 18px;
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

.tn-disabled-badge {
  padding: 4px 10px;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Project Info */
.tn-webhook-project {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 10px;
}

.tn-project-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tn-project-icon svg {
  width: 20px;
  height: 20px;
  color: white;
}

.tn-project-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.tn-project-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tn-project-id {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Triggers Section */
.tn-webhook-triggers-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tn-triggers-label {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tn-triggers-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tn-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.tn-trigger svg {
  width: 14px;
  height: 14px;
}

.tn-trigger-neutral {
  background: #f3f4f6;
  color: #4b5563;
}

.tn-trigger-success {
  background: #d1fae5;
  color: #065f46;
}

.tn-trigger-failure {
  background: #fee2e2;
  color: #dc2626;
}

.tn-trigger-cancel {
  background: #fef3c7;
  color: #d97706;
}

.tn-trigger-warning {
  background: #ffedd5;
  color: #c2410c;
}

.tn-trigger-error {
  background: #fce7f3;
  color: #be185d;
}

.tn-trigger-fixed {
  background: #dcfce7;
  color: #16a34a;
}

.tn-trigger-info {
  background: #dbeafe;
  color: #1d4ed8;
}

.tn-branch-filter {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f5f3ff;
  border-radius: 6px;
  font-size: 12px;
  color: #6d28d9;
}

.tn-branch-filter svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.tn-branch-filter span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Card Actions */
.tn-webhook-card-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.tn-card-action-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tn-card-action-btn svg {
  width: 18px;
  height: 18px;
}

.tn-action-enabled {
  background: #d1fae5;
  color: #059669;
}

.tn-action-enabled:hover {
  background: #a7f3d0;
}

.tn-action-disabled {
  background: #f3f4f6;
  color: #6b7280;
}

.tn-action-disabled:hover {
  background: #e5e7eb;
}

.tn-action-delete {
  background: #fee2e2;
  color: #dc2626;
}

.tn-action-delete:hover {
  background: #fecaca;
}

/* Toast Notifications */
#tn-toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tn-toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.2);
  font-size: 14px;
  font-weight: 500;
  color: white;
  min-width: 320px;
  max-width: 480px;
  animation: toastIn 0.3s ease;
}

.tn-toast svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.tn-toast-success { background: linear-gradient(135deg, #10b981 0%, #059669 100%); }
.tn-toast-error { background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); }
.tn-toast-warning { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); }
.tn-toast-info { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); }

.tn-toast-out {
  animation: toastOut 0.3s ease forwards;
}

@keyframes toastIn {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

@keyframes toastOut {
  from { transform: translateX(0); opacity: 1; }
  to { transform: translateX(100%); opacity: 0; }
}

/* Responsive */
@media (max-width: 768px) {
  .tn-admin-container {
    padding: 16px;
  }

  .tn-admin-header {
    padding: 20px;
  }

  .tn-admin-header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .tn-admin-header-right {
    flex-direction: column;
    align-items: stretch;
  }

  .tn-admin-stats {
    justify-content: center;
  }

  .tn-admin-actions {
    justify-content: center;
  }

  .tn-webhooks-grid {
    grid-template-columns: 1fr;
  }

  .tn-action-btn span {
    display: none;
  }
}
</style>
