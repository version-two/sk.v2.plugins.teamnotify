<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<div class="tn-container">
  <!-- Header Section -->
  <div class="tn-header">
    <div class="tn-header-content">
      <div class="tn-header-title">
        <svg class="tn-icon tn-icon-webhook" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M10 16l-6-6 6-6M14 8l6 6-6 6M8 12h8"/>
        </svg>
        <h2>Webhook Notifications</h2>
      </div>
      <div class="tn-scope-badge">
        <c:choose>
          <c:when test="${not empty buildTypeId}">
            <span class="tn-badge tn-badge-build">
              <svg class="tn-badge-icon" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/>
              </svg>
              Build Configuration
            </span>
            <div class="tn-scope-hint">Webhooks trigger only for this build configuration</div>
          </c:when>
          <c:otherwise>
            <span class="tn-badge tn-badge-project">
              <svg class="tn-badge-icon" viewBox="0 0 20 20" fill="currentColor">
                <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z"/>
              </svg>
              Project Level
            </span>
            <div class="tn-scope-hint">Webhooks trigger for all builds in this project</div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>

  <!-- Versioned Settings Warning -->
  <c:if test="${versionedSettingsReadOnly}">
    <div class="tn-alert tn-alert-warning">
      <svg class="tn-alert-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"/>
      </svg>
      <div class="tn-alert-message">
        <strong>Editing of the project settings is disabled</strong><br>
        Reason: Editing of the settings is disabled in the versioned settings configuration. 
        Configure webhooks in your <code>.teamcity/settings.kts</code> file instead.
        <a href="${pageContext.request.contextPath}/admin/editProject.html?projectId=${projectId}&tab=versionedSettings" style="margin-left: 10px; color: #fff; text-decoration: underline;">View Versioned Settings</a>
      </div>
    </div>
  </c:if>

  <!-- Alert Messages -->
  <div id="messageArea" class="tn-alert tn-alert-success" style="display:none;">
    <svg class="tn-alert-icon" viewBox="0 0 20 20" fill="currentColor">
      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/>
    </svg>
    <div class="tn-alert-message"></div>
    <button class="tn-alert-close" onclick="this.parentElement.style.display='none'">×</button>
  </div>
  
  <div id="errorArea" class="tn-alert tn-alert-error" style="display:none;">
    <svg class="tn-alert-icon" viewBox="0 0 20 20" fill="currentColor">
      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"/>
    </svg>
    <div class="tn-alert-message"></div>
    <button class="tn-alert-close" onclick="this.parentElement.style.display='none'">×</button>
  </div>

  <input type="hidden" id="projectId" value="${projectId}"/>
  <c:if test="${not empty buildTypeId}">
    <input type="hidden" id="buildTypeId" value="${buildTypeId}"/>
  </c:if>

  <!-- Add Webhook Form -->
  <c:if test="${!versionedSettingsReadOnly}">
  <div class="tn-card">
    <div class="tn-card-header">
      <h3 class="tn-card-title">Add New Webhook</h3>
      <button class="tn-help-btn" onclick="document.getElementById('webhookGuides').scrollIntoView({behavior: 'smooth'})">
        <svg viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z"/>
        </svg>
        Help
      </button>
    </div>
    
    <div class="tn-card-body">
      <div class="tn-form-grid">
        <!-- Platform Selection -->
        <div class="tn-form-group">
          <label class="tn-label" for="platform">
            Platform
            <span class="tn-required">*</span>
          </label>
          <div class="tn-platform-selector">
            <input type="radio" id="platform-slack" name="platform-radio" value="SLACK" checked>
            <label for="platform-slack" class="tn-platform-option">
              <svg viewBox="0 0 24 24" class="tn-platform-icon">
                <path fill="#4A154B" d="M5.042 15.165a2.528 2.528 0 0 1-2.52 2.523A2.528 2.528 0 0 1 0 15.165a2.527 2.527 0 0 1 2.522-2.52h2.52v2.52zM6.313 15.165a2.527 2.527 0 0 1 2.521-2.52 2.527 2.527 0 0 1 2.521 2.52v6.313A2.528 2.528 0 0 1 8.834 24a2.528 2.528 0 0 1-2.521-2.522v-6.313zM8.834 5.042a2.528 2.528 0 0 1-2.521-2.52A2.528 2.528 0 0 1 8.834 0a2.528 2.528 0 0 1 2.521 2.522v2.52H8.834zM8.834 6.313a2.528 2.528 0 0 1 2.521 2.521 2.528 2.528 0 0 1-2.521 2.521H2.522A2.528 2.528 0 0 1 0 8.834a2.528 2.528 0 0 1 2.522-2.521h6.312zM18.956 8.834a2.528 2.528 0 0 1 2.522-2.521A2.528 2.528 0 0 1 24 8.834a2.528 2.528 0 0 1-2.522 2.521h-2.522V8.834zM17.688 8.834a2.528 2.528 0 0 1-2.523 2.521 2.527 2.527 0 0 1-2.52-2.521V2.522A2.527 2.527 0 0 1 15.165 0a2.528 2.528 0 0 1 2.523 2.522v6.312zM15.165 18.956a2.528 2.528 0 0 1 2.523 2.522A2.528 2.528 0 0 1 15.165 24a2.527 2.527 0 0 1-2.52-2.522v-2.522h2.52zM15.165 17.688a2.527 2.527 0 0 1-2.52-2.523 2.526 2.526 0 0 1 2.52-2.52h6.313A2.527 2.527 0 0 1 24 15.165a2.528 2.528 0 0 1-2.522 2.523h-6.313z"/>
              </svg>
              <span>Slack</span>
            </label>
            
            <input type="radio" id="platform-teams" name="platform-radio" value="TEAMS">
            <label for="platform-teams" class="tn-platform-option">
              <svg viewBox="0 0 24 24" class="tn-platform-icon">
                <path fill="#5059C9" d="M20.625 8.127q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 6.052 18 5.502q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 16.798 18 16.248q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm-6.873-10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zM20.625 0q1.138 0 2.125.433.988.433 1.713 1.158.725.725 1.158 1.713.433.987.433 2.125v10.746q0 1.138-.433 2.125-.433.988-1.158 1.713-.725.725-1.713 1.158-.987.433-2.125.433H9.879q-1.138 0-2.125-.433-.988-.433-1.713-1.158-.725-.725-1.158-1.713-.433-.987-.433-2.125V11h5.427v5.248q0 .356.239.595.239.239.595.239h9.914q.356 0 .595-.239.239-.239.239-.595V5.502q0-.356-.239-.595-.239-.239-.595-.239h-9.914q-.356 0-.595.239-.239.239-.239.595V11H4.5V5.502q0-1.138.433-2.125.433-.988 1.158-1.713Q6.816.939 7.804.506 8.79.073 9.929.073h10.696z"/>
              </svg>
              <span>Teams</span>
            </label>
            
            <input type="radio" id="platform-discord" name="platform-radio" value="DISCORD">
            <label for="platform-discord" class="tn-platform-option">
              <svg viewBox="0 0 24 24" class="tn-platform-icon">
                <path fill="#5865F2" d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z"/>
              </svg>
              <span>Discord</span>
            </label>
          </div>
          <select id="platform" style="display:none;">
            <option value="SLACK">Slack</option>
            <option value="TEAMS">Teams</option>
            <option value="DISCORD">Discord</option>
          </select>
        </div>

        <!-- Webhook URL -->
        <div class="tn-form-group tn-form-group-full">
          <label class="tn-label" for="webhookUrl">
            Webhook URL
            <span class="tn-required">*</span>
          </label>
          <div class="tn-input-group">
            <input type="text" 
                   id="webhookUrl" 
                   class="tn-input" 
                   placeholder="https://hooks.slack.com/services/..." 
                   autocomplete="off">
            <button type="button" id="testWebhookBtn" class="tn-btn tn-btn-secondary">
              <svg class="tn-btn-icon" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z"/>
              </svg>
              Test
            </button>
          </div>
          <div id="urlError" class="tn-error-text" style="display:none;">
            Please enter a valid webhook URL for the selected platform
          </div>
          <div id="testWebhookResult" class="tn-test-result"></div>
        </div>

        <!-- Trigger Conditions -->
        <div class="tn-form-group tn-form-group-full">
          <label class="tn-label">
            Trigger Conditions
            <span class="tn-required">*</span>
          </label>
          <div class="tn-triggers-grid">
            <label class="tn-trigger">
              <input type="checkbox" id="onStart" class="tn-trigger-checkbox">
              <div class="tn-trigger-card">
                <svg class="tn-trigger-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z"/>
                </svg>
                <span>On Start</span>
              </div>
            </label>
            
            <label class="tn-trigger">
              <input type="checkbox" id="onSuccess" class="tn-trigger-checkbox">
              <div class="tn-trigger-card">
                <svg class="tn-trigger-icon tn-trigger-success" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/>
                </svg>
                <span>On Success</span>
              </div>
            </label>
            
            <label class="tn-trigger">
              <input type="checkbox" id="onFailure" class="tn-trigger-checkbox">
              <div class="tn-trigger-card">
                <svg class="tn-trigger-icon tn-trigger-failure" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"/>
                </svg>
                <span>On Failure</span>
              </div>
            </label>
            
            <label class="tn-trigger">
              <input type="checkbox" id="onStall" class="tn-trigger-checkbox">
              <div class="tn-trigger-card">
                <svg class="tn-trigger-icon tn-trigger-warning" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"/>
                </svg>
                <span>On Stall</span>
              </div>
            </label>
            
            <label class="tn-trigger">
              <input type="checkbox" id="buildLongerThanAverage" class="tn-trigger-checkbox">
              <div class="tn-trigger-card">
                <svg class="tn-trigger-icon tn-trigger-info" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/>
                </svg>
                <span>Longer than Average</span>
              </div>
            </label>
            
            <label class="tn-trigger">
              <input type="checkbox" id="useBuildLongerThan" class="tn-trigger-checkbox">
              <div class="tn-trigger-card">
                <svg class="tn-trigger-icon tn-trigger-info" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"/>
                </svg>
                <span>Duration Threshold</span>
              </div>
            </label>
          </div>
          
          <div id="thresholdRow" class="tn-threshold-row" style="display:none;">
            <label class="tn-label" for="buildLongerThan">Duration in seconds</label>
            <input type="number" 
                   id="buildLongerThan" 
                   class="tn-input tn-input-small" 
                   min="1" 
                   placeholder="300"
                   disabled>
            <span class="tn-help-text">Trigger when build exceeds this duration</span>
            <div id="thresholdError" class="tn-error-text" style="display:none;">
              Please enter a positive number
            </div>
          </div>
        </div>
      </div>
      
      <div class="tn-form-actions">
        <button type="button" id="addWebhookBtn" class="tn-btn tn-btn-primary">
          <svg class="tn-btn-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"/>
          </svg>
          Add Webhook
        </button>
      </div>
    </div>
  </div>
  </c:if>

  <!-- Existing Webhooks -->
  <div class="tn-card">
    <div class="tn-card-header">
      <h3 class="tn-card-title">Configured Webhooks</h3>
      <span class="tn-webhook-count" id="webhookCount">0 webhooks</span>
    </div>
    
    <div class="tn-card-body">
      <div id="webhooksListContainer">
        <div class="tn-webhooks-list" id="webhooksList">
          <c:forEach var="webhook" items="${webhooks}">
            <div class="tn-webhook-item" data-webhook-url="${fn:escapeXml(webhook.url)}">
              <div class="tn-webhook-platform">
                <c:choose>
                  <c:when test="${webhook.platform == 'SLACK'}">
                    <svg viewBox="0 0 24 24" class="tn-platform-icon-small">
                      <path fill="#4A154B" d="M5.042 15.165a2.528 2.528 0 0 1-2.52 2.523A2.528 2.528 0 0 1 0 15.165a2.527 2.527 0 0 1 2.522-2.52h2.52v2.52zM6.313 15.165a2.527 2.527 0 0 1 2.521-2.52 2.527 2.527 0 0 1 2.521 2.52v6.313A2.528 2.528 0 0 1 8.834 24a2.528 2.528 0 0 1-2.521-2.522v-6.313zM8.834 5.042a2.528 2.528 0 0 1-2.521-2.52A2.528 2.528 0 0 1 8.834 0a2.528 2.528 0 0 1 2.521 2.522v2.52H8.834zM8.834 6.313a2.528 2.528 0 0 1 2.521 2.521 2.528 2.528 0 0 1-2.521 2.521H2.522A2.528 2.528 0 0 1 0 8.834a2.528 2.528 0 0 1 2.522-2.521h6.312zM18.956 8.834a2.528 2.528 0 0 1 2.522-2.521A2.528 2.528 0 0 1 24 8.834a2.528 2.528 0 0 1-2.522 2.521h-2.522V8.834zM17.688 8.834a2.528 2.528 0 0 1-2.523 2.521 2.527 2.527 0 0 1-2.52-2.521V2.522A2.527 2.527 0 0 1 15.165 0a2.528 2.528 0 0 1 2.523 2.522v6.312zM15.165 18.956a2.528 2.528 0 0 1 2.523 2.522A2.528 2.528 0 0 1 15.165 24a2.527 2.527 0 0 1-2.52-2.522v-2.522h2.52zM15.165 17.688a2.527 2.527 0 0 1-2.52-2.523 2.526 2.526 0 0 1 2.52-2.52h6.313A2.527 2.527 0 0 1 24 15.165a2.528 2.528 0 0 1-2.522 2.523h-6.313z"/>
                    </svg>
                  </c:when>
                  <c:when test="${webhook.platform == 'TEAMS'}">
                    <svg viewBox="0 0 24 24" class="tn-platform-icon-small">
                      <path fill="#5059C9" d="M20.625 8.127q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 6.052 18 5.502q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 16.798 18 16.248q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm-6.873-10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zM20.625 0q1.138 0 2.125.433.988.433 1.713 1.158.725.725 1.158 1.713.433.987.433 2.125v10.746q0 1.138-.433 2.125-.433.988-1.158 1.713-.725.725-1.713 1.158-.987.433-2.125.433H9.879q-1.138 0-2.125-.433-.988-.433-1.713-1.158-.725-.725-1.158-1.713-.433-.987-.433-2.125V11h5.427v5.248q0 .356.239.595.239.239.595.239h9.914q.356 0 .595-.239.239-.239.239-.595V5.502q0-.356-.239-.595-.239-.239-.595-.239h-9.914q-.356 0-.595.239-.239.239-.239.595V11H4.5V5.502q0-1.138.433-2.125.433-.988 1.158-1.713Q6.816.939 7.804.506 8.79.073 9.929.073h10.696z"/>
                    </svg>
                  </c:when>
                  <c:otherwise>
                    <svg viewBox="0 0 24 24" class="tn-platform-icon-small">
                      <path fill="#5865F2" d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z"/>
                    </svg>
                  </c:otherwise>
                </c:choose>
              </div>
              
              <div class="tn-webhook-info">
                <div class="tn-webhook-url">
                  <c:set var="u" value="${webhook.url}"/>
                  <c:set var="len" value="${fn:length(u)}"/>
                  <c:choose>
                    <c:when test="${len > 60}">
                      ${fn:substring(u, 0, 40)}...${fn:substring(u, len - 15, len)}
                    </c:when>
                    <c:otherwise>
                      ${u}
                    </c:otherwise>
                  </c:choose>
                </div>
                
                <div class="tn-webhook-triggers">
                  <c:if test="${webhook.onStart}">
                    <span class="tn-trigger-tag">On Start</span>
                  </c:if>
                  <c:if test="${webhook.onSuccess}">
                    <span class="tn-trigger-tag tn-trigger-tag-success">On Success</span>
                  </c:if>
                  <c:if test="${webhook.onFailure}">
                    <span class="tn-trigger-tag tn-trigger-tag-failure">On Failure</span>
                  </c:if>
                  <c:if test="${webhook.onStall}">
                    <span class="tn-trigger-tag tn-trigger-tag-warning">On Stall</span>
                  </c:if>
                  <c:if test="${webhook.buildLongerThan != null}">
                    <span class="tn-trigger-tag tn-trigger-tag-info">&gt; ${webhook.buildLongerThan}s</span>
                  </c:if>
                  <c:if test="${webhook.buildLongerThanAverage}">
                    <span class="tn-trigger-tag tn-trigger-tag-info">&gt; Average</span>
                  </c:if>
                </div>
              </div>
              
              <c:choose>
                <c:when test="${versionedSettingsReadOnly}">
                  <button class="tn-webhook-delete" disabled title="Cannot delete webhooks when versioned settings are enabled">
                    <svg viewBox="0 0 20 20" fill="currentColor" style="opacity: 0.3;">
                      <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"/>
                    </svg>
                  </button>
                </c:when>
                <c:otherwise>
                  <button class="tn-webhook-delete" onclick="deleteWebhook('${fn:escapeXml(fn:replace(webhook.url, "'", "\\'"))}')">
                    <svg viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"/>
                    </svg>
                  </button>
                </c:otherwise>
              </c:choose>
            </div>
          </c:forEach>
        </div>
        
        <div id="emptyState" class="tn-empty-state" style="${not empty webhooks ? 'display:none;' : ''}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <path d="M13 16h-1v-4h1m0-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <p>No webhooks configured yet</p>
          <span>Add your first webhook above to start receiving notifications</span>
        </div>
      </div>
    </div>
  </div>

  <!-- Help Section -->
  <div id="webhookGuides" class="tn-card tn-help-section">
    <div class="tn-card-header">
      <h3 class="tn-card-title">Setup Guides</h3>
    </div>
    <div class="tn-card-body">
      <div class="tn-guides-grid">
        <div class="tn-guide">
          <div class="tn-guide-icon">
            <svg viewBox="0 0 24 24">
              <path fill="#4A154B" d="M5.042 15.165a2.528 2.528 0 0 1-2.52 2.523A2.528 2.528 0 0 1 0 15.165a2.527 2.527 0 0 1 2.522-2.52h2.52v2.52zM6.313 15.165a2.527 2.527 0 0 1 2.521-2.52 2.527 2.527 0 0 1 2.521 2.52v6.313A2.528 2.528 0 0 1 8.834 24a2.528 2.528 0 0 1-2.521-2.522v-6.313zM8.834 5.042a2.528 2.528 0 0 1-2.521-2.52A2.528 2.528 0 0 1 8.834 0a2.528 2.528 0 0 1 2.521 2.522v2.52H8.834zM8.834 6.313a2.528 2.528 0 0 1 2.521 2.521 2.528 2.528 0 0 1-2.521 2.521H2.522A2.528 2.528 0 0 1 0 8.834a2.528 2.528 0 0 1 2.522-2.521h6.312zM18.956 8.834a2.528 2.528 0 0 1 2.522-2.521A2.528 2.528 0 0 1 24 8.834a2.528 2.528 0 0 1-2.522 2.521h-2.522V8.834zM17.688 8.834a2.528 2.528 0 0 1-2.523 2.521 2.527 2.527 0 0 1-2.52-2.521V2.522A2.527 2.527 0 0 1 15.165 0a2.528 2.528 0 0 1 2.523 2.522v6.312zM15.165 18.956a2.528 2.528 0 0 1 2.523 2.522A2.528 2.528 0 0 1 15.165 24a2.527 2.527 0 0 1-2.52-2.522v-2.522h2.52zM15.165 17.688a2.527 2.527 0 0 1-2.52-2.523 2.526 2.526 0 0 1 2.52-2.52h6.313A2.527 2.527 0 0 1 24 15.165a2.528 2.528 0 0 1-2.522 2.523h-6.313z"/>
            </svg>
          </div>
          <h4>Slack Setup</h4>
          <ol>
            <li>Open your Slack workspace</li>
            <li>Go to Apps → Incoming Webhooks</li>
            <li>Click "Add to Slack" and select channel</li>
            <li>Copy the webhook URL</li>
          </ol>
        </div>
        
        <div class="tn-guide">
          <div class="tn-guide-icon">
            <svg viewBox="0 0 24 24">
              <path fill="#5059C9" d="M20.625 8.127q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 6.052 18 5.502q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 16.798 18 16.248q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm-6.873-10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zM20.625 0q1.138 0 2.125.433.988.433 1.713 1.158.725.725 1.158 1.713.433.987.433 2.125v10.746q0 1.138-.433 2.125-.433.988-1.158 1.713-.725.725-1.713 1.158-.987.433-2.125.433H9.879q-1.138 0-2.125-.433-.988-.433-1.713-1.158-.725-.725-1.158-1.713-.433-.987-.433-2.125V11h5.427v5.248q0 .356.239.595.239.239.595.239h9.914q.356 0 .595-.239.239-.239.239-.595V5.502q0-.356-.239-.595-.239-.239-.595-.239h-9.914q-.356 0-.595.239-.239.239-.239.595V11H4.5V5.502q0-1.138.433-2.125.433-.988 1.158-1.713Q6.816.939 7.804.506 8.79.073 9.929.073h10.696z"/>
            </svg>
          </div>
          <h4>Microsoft Teams Setup</h4>
          <ol>
            <li>Open your Teams channel</li>
            <li>Click ••• → Connectors</li>
            <li>Find "Incoming Webhook" → Configure</li>
            <li>Name it and copy the URL</li>
          </ol>
        </div>
        
        <div class="tn-guide">
          <div class="tn-guide-icon">
            <svg viewBox="0 0 24 24">
              <path fill="#5865F2" d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z"/>
            </svg>
          </div>
          <h4>Discord Setup</h4>
          <ol>
            <li>Server Settings → Integrations</li>
            <li>Click "Create Webhook"</li>
            <li>Choose channel and customize</li>
            <li>Copy the webhook URL</li>
          </ol>
        </div>
      </div>
    </div>
  </div>
</div>

<script src="https://unpkg.com/axios@0.27.2/dist/axios.min.js"></script>
<script>
(function() {
  // DOM Elements
  const platformRadios = document.querySelectorAll('input[name="platform-radio"]');
  const platformSelect = document.getElementById('platform');
  const urlEl = document.getElementById('webhookUrl');
  const urlError = document.getElementById('urlError');
  const useThreshold = document.getElementById('useBuildLongerThan');
  const thresholdRow = document.getElementById('thresholdRow');
  const thresholdInput = document.getElementById('buildLongerThan');
  const thresholdError = document.getElementById('thresholdError');
  const testBtn = document.getElementById('testWebhookBtn');
  const testResult = document.getElementById('testWebhookResult');
  const addBtn = document.getElementById('addWebhookBtn');
  const messageArea = document.getElementById('messageArea');
  const errorArea = document.getElementById('errorArea');
  const webhooksList = document.getElementById('webhooksList');
  const emptyState = document.getElementById('emptyState');
  const webhookCount = document.getElementById('webhookCount');

  // CSRF Token
  function getCsrfToken() {
    const el = document.querySelector('input[name="tc-csrf-token"]');
    if (el && el.value) return el.value;
    if (window.BS && (BS.csrfToken || BS.CSRF_TOKEN)) return BS.csrfToken || BS.CSRF_TOKEN;
    const meta = document.querySelector('meta[name="tc-csrf-token"]');
    if (meta && meta.content) return meta.content;
    return '';
  }

  // Platform Selection
  platformRadios.forEach(radio => {
    radio.addEventListener('change', function() {
      platformSelect.value = this.value;
      updatePlaceholder();
      validateUrl();
    });
  });

  function updatePlaceholder() {
    const p = platformSelect.value;
    let placeholder = '';
    if (p === 'SLACK') {
      placeholder = 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXX';
    } else if (p === 'TEAMS') {
      placeholder = 'https://{tenant}.webhook.office.com/webhookb2/...';
    } else if (p === 'DISCORD') {
      placeholder = 'https://discord.com/api/webhooks/{id}/{token}';
    }
    urlEl.placeholder = placeholder;
  }

  // Validation
  function validateUrl() {
    const val = (urlEl.value || '').trim();
    const p = platformSelect.value;
    let ok = false;
    if (p === 'SLACK') ok = /^https:\/\/hooks\.slack\.com\//.test(val);
    else if (p === 'TEAMS') ok = /^https:\/\/.*webhook\.office\.com\//.test(val) || /^https:\/\/outlook\.office\.com\//.test(val);
    else if (p === 'DISCORD') ok = /^https:\/\/discord(?:app)?\.com\/api\/webhooks\//.test(val);
    urlError.style.display = ok || !val ? 'none' : 'block';
    return ok;
  }

  function validateThreshold() {
    if (!useThreshold.checked) return true;
    const v = parseInt(thresholdInput.value, 10);
    const ok = Number.isFinite(v) && v > 0;
    thresholdError.style.display = ok ? 'none' : 'block';
    return ok;
  }

  // Threshold Toggle
  function updateThresholdVisibility() {
    const show = !!useThreshold.checked;
    thresholdRow.style.display = show ? 'block' : 'none';
    thresholdInput.disabled = !show;
    if (!show) {
      thresholdError.style.display = 'none';
      thresholdInput.value = '';
    }
  }

  // Messages
  function showMessage(message, isError) {
    const area = isError ? errorArea : messageArea;
    const otherArea = isError ? messageArea : errorArea;
    
    area.querySelector('.tn-alert-message').textContent = message;
    area.style.display = 'flex';
    otherArea.style.display = 'none';
    
    setTimeout(() => {
      area.style.display = 'none';
    }, 5000);
  }

  // Test Webhook
  async function testWebhook() {
    if (!validateUrl()) {
      urlEl.focus();
      return;
    }
    
    testBtn.disabled = true;
    testResult.className = 'tn-test-result tn-test-pending';
    testResult.textContent = 'Testing connection...';
    
    try {
      const response = await axios.post('<c:url value="/notifier/testWebhook.html"/>', 
        new URLSearchParams({
          platform: platformSelect.value,
          webhookUrl: urlEl.value.trim(),
          'tc-csrf-token': getCsrfToken()
        }), {
          headers: {
            'X-TC-CSRF-Token': getCsrfToken()
          }
        }
      );
      
      if (response.data.success) {
        testResult.className = 'tn-test-result tn-test-success';
        testResult.textContent = 'Connection successful!';
      } else {
        testResult.className = 'tn-test-result tn-test-error';
        testResult.textContent = 'Connection failed: ' + (response.data.message || 'Unknown error');
      }
    } catch (error) {
      testResult.className = 'tn-test-result tn-test-error';
      testResult.textContent = 'Test failed: ' + error.message;
    } finally {
      testBtn.disabled = false;
      setTimeout(() => {
        testResult.className = 'tn-test-result';
        testResult.textContent = '';
      }, 5000);
    }
  }

  // Add Webhook
  async function addWebhook() {
    if (!validateUrl() || !validateThreshold()) {
      return;
    }
    
    // Check if at least one trigger is selected
    const triggers = ['onStart', 'onSuccess', 'onFailure', 'onStall', 'buildLongerThanAverage', 'useBuildLongerThan'];
    const hasSelectedTrigger = triggers.some(id => document.getElementById(id).checked);
    
    if (!hasSelectedTrigger) {
      showMessage('Please select at least one trigger condition', true);
      return;
    }
    
    addBtn.disabled = true;
    addBtn.innerHTML = '<span class="tn-spinner"></span> Adding...';
    
    const params = new URLSearchParams({
      projectId: document.getElementById('projectId').value,
      platform: platformSelect.value,
      webhookUrl: urlEl.value.trim(),
      onStart: document.getElementById('onStart').checked,
      onSuccess: document.getElementById('onSuccess').checked,
      onFailure: document.getElementById('onFailure').checked,
      onStall: document.getElementById('onStall').checked,
      buildLongerThanAverage: document.getElementById('buildLongerThanAverage').checked,
      'tc-csrf-token': getCsrfToken()
    });
    
    const buildTypeId = document.getElementById('buildTypeId')?.value;
    if (buildTypeId) params.set('buildTypeId', buildTypeId);
    
    if (useThreshold.checked && thresholdInput.value) {
      params.set('buildLongerThan', thresholdInput.value);
    }
    
    try {
      const response = await axios.post('<c:url value="/notifier/api/webhooks.html"/>', params, {
        headers: {
          'X-TC-CSRF-Token': getCsrfToken()
        }
      });
      
      if (response.data.success) {
        showMessage('Webhook added successfully!', false);
        refreshWebhooksList();
        resetForm();
      } else {
        showMessage('Failed to add webhook: ' + (response.data.error || 'Unknown error'), true);
      }
    } catch (error) {
      showMessage('Error adding webhook: ' + error.message, true);
    } finally {
      addBtn.disabled = false;
      addBtn.innerHTML = '<svg class="tn-btn-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"/></svg> Add Webhook';
    }
  }

  // Delete Webhook
  window.deleteWebhook = async function(webhookUrl) {
    if (!confirm('Are you sure you want to delete this webhook?')) return;
    
    const params = new URLSearchParams({
      projectId: document.getElementById('projectId').value,
      webhookUrl: webhookUrl,
      'tc-csrf-token': getCsrfToken()
    });
    
    const buildTypeId = document.getElementById('buildTypeId')?.value;
    if (buildTypeId) params.set('buildTypeId', buildTypeId);
    
    try {
      const response = await axios.delete('<c:url value="/notifier/api/webhooks.html"/>?' + params.toString(), {
        headers: {
          'X-TC-CSRF-Token': getCsrfToken()
        }
      });
      
      if (response.data.success) {
        showMessage('Webhook deleted successfully!', false);
        refreshWebhooksList();
      } else {
        showMessage('Failed to delete webhook: ' + (response.data.error || 'Unknown error'), true);
      }
    } catch (error) {
      showMessage('Error deleting webhook: ' + error.message, true);
    }
  }

  // Refresh List
  async function refreshWebhooksList() {
    const params = new URLSearchParams({
      projectId: document.getElementById('projectId').value
    });
    
    const buildTypeId = document.getElementById('buildTypeId')?.value;
    if (buildTypeId) params.set('buildTypeId', buildTypeId);
    
    try {
      const response = await axios.get('<c:url value="/notifier/api/webhooks.html"/>?' + params.toString());
      
      if (response.data.success && response.data.webhooks) {
        const webhooks = response.data.webhooks;
        updateWebhookCount(webhooks.length);
        
        if (webhooks.length === 0) {
          webhooksList.innerHTML = '';
          emptyState.style.display = 'flex';
        } else {
          emptyState.style.display = 'none';
          renderWebhooks(webhooks);
        }
      }
    } catch (error) {
      console.error('Failed to refresh webhooks:', error);
    }
  }

  function updateWebhookCount(count) {
    webhookCount.textContent = count + ' webhook' + (count !== 1 ? 's' : '');
  }

  function renderWebhooks(webhooks) {
    const html = webhooks.map(webhook => {
      const platformIcon = getPlatformIcon(webhook.platform);
      const displayUrl = webhook.url.length > 60 
        ? webhook.url.substring(0, 40) + '...' + webhook.url.substring(webhook.url.length - 15)
        : webhook.url;
      
      const triggers = [];
      if (webhook.onStart) triggers.push('<span class="tn-trigger-tag">On Start</span>');
      if (webhook.onSuccess) triggers.push('<span class="tn-trigger-tag tn-trigger-tag-success">On Success</span>');
      if (webhook.onFailure) triggers.push('<span class="tn-trigger-tag tn-trigger-tag-failure">On Failure</span>');
      if (webhook.onStall) triggers.push('<span class="tn-trigger-tag tn-trigger-tag-warning">On Stall</span>');
      if (webhook.buildLongerThan !== null) triggers.push('<span class="tn-trigger-tag tn-trigger-tag-info">&gt; ' + webhook.buildLongerThan + 's</span>');
      if (webhook.buildLongerThanAverage) triggers.push('<span class="tn-trigger-tag tn-trigger-tag-info">&gt; Average</span>');
      
      return `
        <div class="tn-webhook-item" data-webhook-url="${webhook.url}">
          <div class="tn-webhook-platform">${platformIcon}</div>
          <div class="tn-webhook-info">
            <div class="tn-webhook-url">${displayUrl}</div>
            <div class="tn-webhook-triggers">${triggers.join('')}</div>
          </div>
          <button class="tn-webhook-delete" onclick="deleteWebhook('${webhook.url.replace(/'/g, "\\'")}')">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"/>
            </svg>
          </button>
        </div>
      `;
    }).join('');
    
    webhooksList.innerHTML = html;
  }

  function getPlatformIcon(platform) {
    const icons = {
      SLACK: '<svg viewBox="0 0 24 24" class="tn-platform-icon-small"><path fill="#4A154B" d="M5.042 15.165a2.528 2.528 0 0 1-2.52 2.523A2.528 2.528 0 0 1 0 15.165a2.527 2.527 0 0 1 2.522-2.52h2.52v2.52zM6.313 15.165a2.527 2.527 0 0 1 2.521-2.52 2.527 2.527 0 0 1 2.521 2.52v6.313A2.528 2.528 0 0 1 8.834 24a2.528 2.528 0 0 1-2.521-2.522v-6.313zM8.834 5.042a2.528 2.528 0 0 1-2.521-2.52A2.528 2.528 0 0 1 8.834 0a2.528 2.528 0 0 1 2.521 2.522v2.52H8.834zM8.834 6.313a2.528 2.528 0 0 1 2.521 2.521 2.528 2.528 0 0 1-2.521 2.521H2.522A2.528 2.528 0 0 1 0 8.834a2.528 2.528 0 0 1 2.522-2.521h6.312zM18.956 8.834a2.528 2.528 0 0 1 2.522-2.521A2.528 2.528 0 0 1 24 8.834a2.528 2.528 0 0 1-2.522 2.521h-2.522V8.834zM17.688 8.834a2.528 2.528 0 0 1-2.523 2.521 2.527 2.527 0 0 1-2.52-2.521V2.522A2.527 2.527 0 0 1 15.165 0a2.528 2.528 0 0 1 2.523 2.522v6.312zM15.165 18.956a2.528 2.528 0 0 1 2.523 2.522A2.528 2.528 0 0 1 15.165 24a2.527 2.527 0 0 1-2.52-2.522v-2.522h2.52zM15.165 17.688a2.527 2.527 0 0 1-2.52-2.523 2.526 2.526 0 0 1 2.52-2.52h6.313A2.527 2.527 0 0 1 24 15.165a2.528 2.528 0 0 1-2.522 2.523h-6.313z"/></svg>',
      TEAMS: '<svg viewBox="0 0 24 24" class="tn-platform-icon-small"><path fill="#5059C9" d="M20.625 8.127q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 6.052 18 5.502q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832Q18 16.798 18 16.248q0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm-6.873-10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zm0 10.746q-.55 0-1.025-.205-.475-.205-.832-.563-.358-.357-.563-.832-.205-.475-.205-1.025 0-.55.205-1.025.205-.475.563-.832.357-.358.832-.563.475-.205 1.025-.205.55 0 1.025.205.475.205.832.563.358.357.563.832.205.475.205 1.025 0 .55-.205 1.025-.205.475-.563.832-.357.358-.832.563-.475.205-1.025.205zM20.625 0q1.138 0 2.125.433.988.433 1.713 1.158.725.725 1.158 1.713.433.987.433 2.125v10.746q0 1.138-.433 2.125-.433.988-1.158 1.713-.725.725-1.713 1.158-.987.433-2.125.433H9.879q-1.138 0-2.125-.433-.988-.433-1.713-1.158-.725-.725-1.158-1.713-.433-.987-.433-2.125V11h5.427v5.248q0 .356.239.595.239.239.595.239h9.914q.356 0 .595-.239.239-.239.239-.595V5.502q0-.356-.239-.595-.239-.239-.595-.239h-9.914q-.356 0-.595.239-.239.239-.239.595V11H4.5V5.502q0-1.138.433-2.125.433-.988 1.158-1.713Q6.816.939 7.804.506 8.79.073 9.929.073h10.696z"/></svg>',
      DISCORD: '<svg viewBox="0 0 24 24" class="tn-platform-icon-small"><path fill="#5865F2" d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z"/></svg>'
    };
    return icons[platform] || '';
  }

  function resetForm() {
    urlEl.value = '';
    document.getElementById('onStart').checked = false;
    document.getElementById('onSuccess').checked = false;
    document.getElementById('onFailure').checked = false;
    document.getElementById('onStall').checked = false;
    document.getElementById('buildLongerThanAverage').checked = false;
    useThreshold.checked = false;
    thresholdInput.value = '';
    updateThresholdVisibility();
    validateUrl();
  }

  // Event Listeners
  urlEl.addEventListener('input', validateUrl);
  useThreshold.addEventListener('change', updateThresholdVisibility);
  thresholdInput.addEventListener('input', validateThreshold);
  testBtn.addEventListener('click', testWebhook);
  addBtn.addEventListener('click', addWebhook);

  // Initialize
  updatePlaceholder();
  updateThresholdVisibility();
  updateWebhookCount(document.querySelectorAll('.tn-webhook-item').length);
  
  // Check if we need to show empty state
  if (webhooksList.children.length === 0) {
    emptyState.style.display = 'flex';
  }
})();
</script>

<style>
/* Modern TeamNotify Styles */
.tn-container {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #1a1a1a;
  max-width: 1200px;
  margin: 0 auto;
}

/* Header */
.tn-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.tn-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.tn-header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
}

.tn-header-title h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.tn-icon-webhook {
  width: 32px;
  height: 32px;
}

.tn-scope-badge {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tn-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  background: white;
}

.tn-badge-icon {
  width: 16px;
  height: 16px;
}

.tn-badge-build {
  color: #764ba2;
}

.tn-badge-project {
  color: #667eea;
}

.tn-scope-hint {
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
}

/* Cards */
.tn-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
  margin-bottom: 24px;
  overflow: hidden;
}

.tn-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
}

.tn-card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.tn-card-body {
  padding: 24px;
}

/* Alerts */
.tn-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  animation: slideDown 0.3s ease;
}

.tn-alert-success {
  background: #d1fae5;
  border: 1px solid #6ee7b7;
  color: #065f46;
}

.tn-alert-error {
  background: #fee2e2;
  border: 1px solid #fca5a5;
  color: #991b1b;
}

.tn-alert-warning {
  background: #fef3c7;
  border: 1px solid #fbbf24;
  color: #92400e;
}

.tn-alert-warning a {
  color: #92400e;
  font-weight: 600;
}

.tn-alert-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.tn-alert-message {
  flex: 1;
  font-size: 14px;
}

.tn-alert-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.tn-alert-close:hover {
  opacity: 1;
}

/* Form */
.tn-form-grid {
  display: grid;
  gap: 24px;
}

.tn-form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tn-form-group-full {
  grid-column: 1 / -1;
}

.tn-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.tn-required {
  color: #ef4444;
}

/* Platform Selector */
.tn-platform-selector {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.tn-platform-selector input[type="radio"] {
  display: none;
}

.tn-platform-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.tn-platform-option:hover {
  border-color: #9ca3af;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.tn-platform-selector input[type="radio"]:checked + .tn-platform-option {
  border-color: #667eea;
  background: linear-gradient(to bottom, #f0f9ff, #e0f2fe);
}

.tn-platform-icon {
  width: 32px;
  height: 32px;
}

.tn-platform-icon-small {
  width: 24px;
  height: 24px;
}

/* Input Group */
.tn-input-group {
  display: flex;
  gap: 8px;
}

.tn-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.tn-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.tn-input-small {
  width: 120px;
}

/* Buttons */
.tn-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tn-btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.tn-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.tn-btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.tn-btn-secondary {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
}

.tn-btn-secondary:hover {
  background: #e5e7eb;
}

.tn-btn-icon {
  width: 16px;
  height: 16px;
}

.tn-help-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tn-help-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.tn-help-btn svg {
  width: 16px;
  height: 16px;
}

/* Triggers Grid */
.tn-triggers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.tn-trigger {
  position: relative;
  cursor: pointer;
}

.tn-trigger-checkbox {
  position: absolute;
  opacity: 0;
}

.tn-trigger-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  transition: all 0.2s;
  text-align: center;
  font-size: 13px;
}

.tn-trigger-card:hover {
  border-color: #9ca3af;
  transform: translateY(-2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tn-trigger-checkbox:checked + .tn-trigger-card {
  border-color: #667eea;
  background: linear-gradient(to bottom, #f0f9ff, #e0f2fe);
}

.tn-trigger-icon {
  width: 24px;
  height: 24px;
  color: #6b7280;
}

.tn-trigger-checkbox:checked + .tn-trigger-card .tn-trigger-icon {
  color: #667eea;
}

.tn-trigger-success { color: #10b981; }
.tn-trigger-failure { color: #ef4444; }
.tn-trigger-warning { color: #f59e0b; }
.tn-trigger-info { color: #3b82f6; }

/* Threshold Row */
.tn-threshold-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  margin-top: 12px;
}

/* Test Result */
.tn-test-result {
  font-size: 13px;
  padding: 4px 0;
  transition: all 0.3s;
}

.tn-test-pending { color: #6b7280; }
.tn-test-success { color: #10b981; }
.tn-test-error { color: #ef4444; }

/* Error Text */
.tn-error-text {
  color: #ef4444;
  font-size: 13px;
  margin-top: 4px;
}

/* Help Text */
.tn-help-text {
  color: #6b7280;
  font-size: 13px;
}

/* Form Actions */
.tn-form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

/* Webhooks List */
.tn-webhook-count {
  background: #f3f4f6;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  color: #6b7280;
}

.tn-webhooks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tn-webhook-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.tn-webhook-item:hover {
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.tn-webhook-platform {
  flex-shrink: 0;
}

.tn-webhook-info {
  flex: 1;
  min-width: 0;
}

.tn-webhook-url {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 8px;
  word-break: break-all;
}

.tn-webhook-triggers {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tn-trigger-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  background: #e5e7eb;
  color: #4b5563;
}

.tn-trigger-tag-success {
  background: #d1fae5;
  color: #065f46;
}

.tn-trigger-tag-failure {
  background: #fee2e2;
  color: #991b1b;
}

.tn-trigger-tag-warning {
  background: #fed7aa;
  color: #92400e;
}

.tn-trigger-tag-info {
  background: #dbeafe;
  color: #1e40af;
}

.tn-webhook-delete {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.tn-webhook-delete:hover {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #ef4444;
}

.tn-webhook-delete svg {
  width: 18px;
  height: 18px;
}

/* Empty State */
.tn-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  text-align: center;
  color: #6b7280;
}

.tn-empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.tn-empty-state p {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #4b5563;
}

.tn-empty-state span {
  font-size: 14px;
}

/* Help Section */
.tn-help-section {
  background: linear-gradient(to bottom, #f9fafb, white);
}

.tn-guides-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.tn-guide {
  padding: 20px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.tn-guide-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
}

.tn-guide h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.tn-guide ol {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
}

.tn-guide li {
  margin-bottom: 6px;
}

/* Spinner */
.tn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* Animations */
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Responsive */
@media (max-width: 768px) {
  .tn-platform-selector {
    grid-template-columns: 1fr;
  }
  
  .tn-triggers-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .tn-webhook-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .tn-webhook-delete {
    align-self: flex-end;
    margin-top: -36px;
  }
}
</style>