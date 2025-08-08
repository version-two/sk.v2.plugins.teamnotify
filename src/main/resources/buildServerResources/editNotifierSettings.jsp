<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div class="section noMargin">
  <h2 class="noBorder">TeamNotify - Webhook Settings</h2>

  <c:if test="${param.message != null}">
    <div class="successMessage">${param.message}</div>
  </c:if>
  <c:if test="${not empty validationErrors}">
    <div class="errorMessage" style="margin-bottom:10px;">
      <ul style="margin:0 0 0 18px;">
        <c:forEach var="e" items="${validationErrors}"><li>${e}</li></c:forEach>
      </ul>
    </div>
  </c:if>

  <form id="webhookSettingsForm" action="<c:url value='/notifier/settings.html'/>" method="post">
    <input type="hidden" name="projectId" value="${projectId}"/>
    <c:if test="${not empty buildTypeId}">
      <input type="hidden" name="buildTypeId" value="${buildTypeId}"/>
    </c:if>
    <input type="hidden" name="action" value="add"/>
    
    <h3>Add New Webhook</h3>
    <table class="settings parametersTable">
      <tbody>
        <tr>
          <th class="name"><label for="platform">Platform</label></th>
          <td class="value">
            <c:set var="selPlatform" value="${empty formPlatform ? 'SLACK' : formPlatform}"/>
            <select id="platform" name="platform" class="longField">
              <option value="SLACK" <c:if test="${selPlatform == 'SLACK'}">selected="selected"</c:if>>Slack</option>
              <option value="TEAMS" <c:if test="${selPlatform == 'TEAMS'}">selected="selected"</c:if>>Teams</option>
              <option value="DISCORD" <c:if test="${selPlatform == 'DISCORD'}">selected="selected"</c:if>>Discord</option>
            </select>
          </td>
        </tr>
        <tr>
          <th class="name"><label for="webhookUrl">Webhook URL</label> <span id="webhookInfoIcon" class="infoIcon" title="How to obtain the webhook URL">i</span></th>
          <td class="value">
            <div style="display:flex; gap:8px; align-items:center;">
              <input type="text" id="webhookUrl" name="webhookUrl" class="textField longField" placeholder="https://hooks.slack.com/services/..." value="${formUrl}" />
              <button type="button" id="testWebhookBtn" class="btn">Test</button>
              <span id="testWebhookResult" style="font-size:12px;"></span>
            </div>
            <div id="urlError" class="errorMessage" style="display:none; margin-top:4px;">Please enter a valid webhook URL for the selected platform.</div>
            <!-- inline help moved to the bottom detailed tutorials -->
          </td>
        </tr>
        <tr>
          <th class="name">Conditions</th>
          <td class="value">
            <div class="checkboxGroup">
              <label><input type="checkbox" id="onStart" name="onStart" <c:if test="${formOnStart}">checked</c:if> /> On Start</label>
              <label><input type="checkbox" id="onSuccess" name="onSuccess" <c:if test="${formOnSuccess}">checked</c:if> /> On Success</label>
              <label><input type="checkbox" id="onFailure" name="onFailure" <c:if test="${formOnFailure}">checked</c:if> /> On Failure</label>
              <label><input type="checkbox" id="onStall" name="onStall" <c:if test="${formOnStall}">checked</c:if> /> On Stall</label>
              <label><input type="checkbox" id="buildLongerThanAverage" name="buildLongerThanAverage" <c:if test="${formBuildLongerThanAverage}">checked</c:if> /> Longer than average</label>
              <label><input type="checkbox" id="useBuildLongerThan" /> Longer than threshold</label>
            </div>
          </td>
        </tr>
        <tr id="thresholdRow" style="display:none;">
          <th class="name"><label for="buildLongerThan">Duration threshold</label></th>
          <td class="value">
            <input type="number" min="1" step="1" id="buildLongerThan" name="buildLongerThan" class="textField shortField" placeholder="seconds" value="${formBuildLongerThan}" disabled />
            <span class="smallNote">Send when build duration exceeds this threshold.</span>
            <div id="thresholdError" class="errorMessage" style="display:none; margin-top:4px;">Please enter a positive threshold in seconds.</div>
          </td>
        </tr>
        <tr>
          <th class="name"></th>
          <td class="value">
            <button type="submit" class="btn btn_primary">Add Webhook</button>
          </td>
        </tr>
      </tbody>
    </table>

    <h3>Existing Webhooks</h3>
    <table class="settings parametersTable webhooksTable">
      <thead>
        <tr>
          <th class="name col-url">Webhook URL</th>
          <th class="name col-platform">Platform</th>
          <th class="name">Trigger Conditions</th>
          <th class="name"></th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="webhook" items="${webhooks}">
          <tr>
            <td class="value"><div class="urlCell">
              <c:set var="u" value="${webhook.url}"/>
              <c:set var="len" value="${fn:length(u)}"/>
              <c:choose>
                <c:when test="${len > 48}">
                  <c:set var="head" value="${fn:substring(u, 0, 28)}"/>
                  <c:set var="tail" value="${fn:substring(u, len - 8, len)}"/>
                  ${head}&hellip;${tail}
                </c:when>
                <c:otherwise>
                  ${u}
                </c:otherwise>
              </c:choose>
            </div></td>
            <td class="value col-platform"><span class="badge">${webhook.platform}</span></td>
            <td class="value">
              <div class="conditions">
                <c:if test="${webhook.onStart}"><span class="condition start">&#9654; On Start</span></c:if>
                <c:if test="${webhook.onSuccess}"><span class="condition success">&#10003; On Success</span></c:if>
                <c:if test="${webhook.onFailure}"><span class="condition failure">&#10007; On Failure</span></c:if>
                <c:if test="${webhook.onStall}"><span class="condition stall">&#9208; On Stall</span></c:if>
                <c:if test="${webhook.buildLongerThan != null}"><span class="condition duration">&#9201; Longer than ${webhook.buildLongerThan}s</span></c:if>
                <c:if test="${webhook.buildLongerThanAverage}"><span class="condition duration">Avg+ Longer than average</span></c:if>
                <c:if test="${webhook.onFirstFailure}"><span class="condition failure">! First Failure</span></c:if>
                <c:if test="${webhook.onBuildFixed}"><span class="condition success">Fix Build Fixed</span></c:if>
              </div>
            </td>
            <td class="edit">
              <form action="<c:url value='/notifier/settings.html'/>" method="post" style="display:inline;">
                <input type="hidden" name="projectId" value="${projectId}"/>
                <c:if test="${not empty buildTypeId}">
                  <input type="hidden" name="buildTypeId" value="${buildTypeId}"/>
                </c:if>
                <input type="hidden" name="action" value="delete"/>
                <input type="hidden" name="webhookUrlToDelete" value="${webhook.url}"/>
                <button type="submit" class="btn btn_mini danger">Delete</button>
              </form>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>

    <div id="webhookGuides" class="webhookGuides">
      <h3>How to obtain a Webhook URL</h3>
      <div class="guides">
        <div class="guide">
          <h4>Slack</h4>
          <ol>
            <li>In Slack, open the workspace where you want to receive notifications.</li>
            <li>Go to Apps and search for “Incoming Webhooks”.</li>
            <li>Click “Add to Slack” and choose the channel.</li>
            <li>Slack will generate a Webhook URL beginning with <code>https://hooks.slack.com/</code>.</li>
            <li>Copy the URL and paste it into the Webhook URL field above.</li>
          </ol>
          <p class="tip">Tip: You can create multiple webhooks for different channels.</p>
        </div>
        <div class="guide">
          <h4>Microsoft Teams</h4>
          <ol>
            <li>In Teams, open the team and channel where you want notifications.</li>
            <li>Click the channel’s ••• menu and select “Connectors”.</li>
            <li>Find “Incoming Webhook” and click “Configure”.</li>
            <li>Name the webhook and upload an icon if you wish, then click “Create”.</li>
            <li>Copy the generated URL (it looks like <code>https://&lt;tenant&gt;.webhook.office.com/...</code>) and paste it above.</li>
          </ol>
          <p class="tip">Note: In some tenants, URLs may start with <code>https://outlook.office.com/</code>.</p>
        </div>
        <div class="guide">
          <h4>Discord</h4>
          <ol>
            <li>In Discord, open your server, go to Server Settings → Integrations → Webhooks.</li>
            <li>Click “New Webhook”, choose the channel and customize the name/icon.</li>
            <li>Click “Copy Webhook URL”. It will look like <code>https://discord.com/api/webhooks/&lt;id&gt;/&lt;token&gt;</code>.</li>
            <li>Paste the URL into the Webhook URL field above.</li>
          </ol>
          <p class="tip">Security: Treat your Discord webhook like a secret. Anyone with it can post to your channel.</p>
        </div>
      </div>
    </div>

    <!-- No global save needed; actions handled above -->
  </form>
  <script>
    (function(){
      var platformEl = document.getElementById('platform');
      var urlEl = document.getElementById('webhookUrl');
      var urlError = document.getElementById('urlError');
      var infoIcon = document.getElementById('webhookInfoIcon');
      var form = document.getElementById('webhookSettingsForm');
      var useThreshold = document.getElementById('useBuildLongerThan');
      var thresholdRow = document.getElementById('thresholdRow');
      var thresholdInput = document.getElementById('buildLongerThan');
      var thresholdError = document.getElementById('thresholdError');
      var testBtn = document.getElementById('testWebhookBtn');
      var testResult = document.getElementById('testWebhookResult');

      function getCookie(name){
        var m = document.cookie.match(new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()\[\]\\\/\+^])/g, '\\$1') + '=([^;]*)'));
        return m ? decodeURIComponent(m[1]) : '';
      }

      function getCsrfToken(){
        var el = document.querySelector('input[name="tc-csrf-token"]');
        if (el && el.value) return el.value;
        if (window.BS && (BS.csrfToken || BS.CSRF_TOKEN)) return BS.csrfToken || BS.CSRF_TOKEN;
        var meta = document.querySelector('meta[name="tc-csrf-token"]');
        if (meta && meta.content) return meta.content;
        var cookie = getCookie('TC-CSRF-Token') || getCookie('TC-CSRF-TOKEN') || getCookie('tc-csrf-token');
        return cookie || '';
      }

      function updatePlaceholder(){
        var p = platformEl.value;
        var placeholder = '';
        if (p === 'SLACK') {
          placeholder = 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXX';
        } else if (p === 'TEAMS') {
          placeholder = 'https://{tenant}.webhook.office.com/webhookb2/...';
        } else if (p === 'DISCORD') {
          placeholder = 'https://discord.com/api/webhooks/{id}/{token}';
        }
        urlEl.placeholder = placeholder;
      }

      function validateUrl(){
        var val = (urlEl.value || '').trim();
        var p = platformEl.value;
        var ok = false;
        if (p === 'SLACK') ok = /^https:\/\/hooks\.slack\.com\//.test(val);
        else if (p === 'TEAMS') ok = /^https:\/\/.*webhook\.office\.com\//.test(val) || /^https:\/\/outlook\.office\.com\//.test(val);
        else if (p === 'DISCORD') ok = /^https:\/\/discord(?:app)?\.com\/api\/webhooks\//.test(val);
        urlError.style.display = ok ? 'none' : 'block';
        return ok;
      }

      function scrollToGuides(){
        var el = document.getElementById('webhookGuides');
        if (el && el.scrollIntoView) el.scrollIntoView({behavior: 'smooth'});
        location.hash = '#webhookGuides';
      }

      function updateThresholdVisibility(){
        var show = !!useThreshold.checked;
        thresholdRow.style.display = show ? '' : 'none';
        thresholdInput.disabled = !show;
        if (!show) { thresholdError.style.display = 'none'; thresholdInput.value = ''; }
      }

      function validateThreshold(){
        if (!useThreshold.checked) return true;
        var v = parseInt(thresholdInput.value, 10);
        var ok = Number.isFinite(v) && v > 0;
        thresholdError.style.display = ok ? 'none' : 'block';
        return ok;
      }

      function testWebhook(){
        testResult.textContent = '';
        if (!validateUrl()) {
          urlEl.focus();
          return;
        }
        testBtn.disabled = true;
        testResult.style.color = '#555';
        testResult.textContent = 'Testing…';
        var params = new URLSearchParams();
        params.set('platform', platformEl.value);
        params.set('webhookUrl', (urlEl.value || '').trim());
        var csrf = getCsrfToken();
        if (csrf) params.set('tc-csrf-token', csrf);
        fetch('<c:url value="/notifier/testWebhook.html"/>', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 'X-TC-CSRF-Token': csrf },
          body: params.toString()
        }).then(function(r){
          return r.json().catch(function(){ return { success:false, status:r.status, message:'Invalid response' }; });
        }).then(function(data){
          if (data.success) {
            testResult.style.color = '#2e7d32';
            testResult.textContent = 'Success (HTTP ' + (data.status || 200) + ')';
          } else {
            testResult.style.color = '#d32f2f';
            testResult.textContent = 'Failed (HTTP ' + (data.status || 500) + '): ' + (data.message || '');
          }
        }).catch(function(err){
          testResult.style.color = '#d32f2f';
          testResult.textContent = 'Error: ' + (err && err.message ? err.message : err);
        }).finally(function(){
          testBtn.disabled = false;
        });
      }

      platformEl.addEventListener('change', function(){ updatePlaceholder(); validateUrl(); });
      urlEl.addEventListener('input', validateUrl);
      infoIcon.addEventListener('click', function(e){ e.preventDefault(); scrollToGuides(); });
      useThreshold.addEventListener('change', updateThresholdVisibility);
      thresholdInput.addEventListener('input', validateThreshold);
      testBtn.addEventListener('click', testWebhook);

      form.addEventListener('submit', function(e){
        var ok = validateUrl() && validateThreshold();
        if (!ok) { e.preventDefault(); return; }
        // ensure CSRF token is present as hidden input
        var csrf = getCsrfToken();
        if (csrf && !form.querySelector('input[name="tc-csrf-token"]')){
          var hidden = document.createElement('input');
          hidden.type = 'hidden'; hidden.name = 'tc-csrf-token'; hidden.value = csrf;
          form.appendChild(hidden);
        }
      });

      // init
      updatePlaceholder();
      // re-check useThreshold if a value is present (postback)
      useThreshold.checked = !!thresholdInput.value;
      updateThresholdVisibility();

      // Pre-inject CSRF token into all POST forms targeting our controller (add & delete)
      var csrfInit = getCsrfToken();
      if (csrfInit) {
        var formsToPatch = document.querySelectorAll('form[action$="/notifier/settings.html"][method="post"], form[action$="/notifier/settings.html"]');
        Array.prototype.forEach.call(formsToPatch, function(f){
          if (!f.querySelector('input[name="tc-csrf-token"]')){
            var hidden = document.createElement('input');
            hidden.type = 'hidden'; hidden.name = 'tc-csrf-token'; hidden.value = csrfInit;
            f.appendChild(hidden);
          }
        });
      }
    })();
  </script>
  <style>
    .conditions { font-size: 12px; line-height: 1.4; display:flex; flex-wrap: wrap; gap:4px; }
    .condition { display: inline-block; padding: 2px 6px; margin: 1px 2px; border-radius: 3px; font-size: 11px; }
    .condition.success { background-color: #d4edda; color: #155724; }
    .condition.start { background-color: #e2e3ff; color: #383d7c; }
    .condition.failure { background-color: #f8d7da; color: #721c24; }
    .condition.stall { background-color: #fff3cd; color: #856404; }
    .condition.duration { background-color: #d1ecf1; color: #0c5460; }
    .badge { background-color: #007cba; color: #fff; padding: 3px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; text-transform: uppercase; }
    .btn_primary { background-color: #007cba; color: #fff; border: none; padding: 6px 12px; cursor: pointer; }
    .btn { background-color: #f1f1f1; color: #333; border: 1px solid #ccc; padding: 6px 10px; cursor: pointer; }
    .btn_mini.danger { background-color: #f44336; color: #fff; border: none; padding: 4px 8px; cursor: pointer; }
    .checkboxGroup label { margin-right: 10px; }
    .infoIcon { cursor: pointer; display: inline-block; margin-left: 6px; color: #007cba; border: 1px solid #007cba; border-radius: 50%; width: 16px; height: 16px; line-height: 16px; text-align: center; font-size: 12px; font-weight: bold; }
    .errorMessage { color: #d32f2f; }
    .helpText { color: #555; }
    .webhooksTable .col-platform { width: 110px; white-space: nowrap; }
    .webhooksTable .urlCell { word-break: break-all; max-width: 420px; }
    .webhookGuides { margin-top: 18px; }
    .webhookGuides .guides { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
    .webhookGuides .guide h4 { margin: 8px 0; }
    .webhookGuides .tip { color: #555; font-style: italic; }
  </style>
</div>
