<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="editBuildFeatures">
  <h2>Webhook Notifier Settings</h2>

  <c:if test="${param.message != null}">
    <div class="success-message" style="color: green; margin-bottom: 10px;">${param.message}</div>
  </c:if>

  <form id="webhookSettingsForm" action="/notifier/settings.html" method="post">
    <input type="hidden" name="projectId" value="${projectId}"/>
    <c:if test="${not empty buildTypeId}">
      <input type="hidden" name="buildTypeId" value="${buildTypeId}"/>
    </c:if>
    <input type="hidden" name="action" value="add"/>
    <div class="settings-group">
      <h3>Add New Webhook</h3>
      <div class="field-group">
        <label for="webhookUrl">Webhook URL:</label>
        <input type="text" id="webhookUrl" name="webhookUrl" class="long-field" />
      </div>
      <div class="field-group">
        <label for="conditions">Conditions:</label>
        <input type="checkbox" id="onStart" name="onStart" />
        <label for="onStart">On Start</label>
        <input type="checkbox" id="onSuccess" name="onSuccess" />
        <label for="onSuccess">On Success</label>
        <input type="checkbox" id="onFailure" name="onFailure" />
        <label for="onFailure">On Failure</label>
        <input type="checkbox" id="onStall" name="onStall" />
        <label for="onStall">On Stall</label>
        <input type="checkbox" id="buildLongerThanAverage" name="buildLongerThanAverage" />
        <label for="buildLongerThanAverage">Build longer than average</label>
      </div>
      <div class="field-group">
        <label for="buildLongerThan">Build longer than (seconds):</label>
        <input type="text" id="buildLongerThan" name="buildLongerThan" class="long-field" />
      </div>
      <div class="field-group">
        <input type="submit" value="Add Webhook" />
      </div>
    </div>

    <div class="settings-group">
      <h3>Existing Webhooks</h3>
      <table class="settings-table">
        <thead>
          <tr>
            <th>Webhook URL</th>
            <th>Platform</th>
            <th>Conditions</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="webhook" items="${webhooks}">
            <tr>
              <td>${webhook.url}</td>
              <td>${webhook.platform}</td>
              <td>
                <c:if test="${webhook.onStart}">On Start</c:if>
                <c:if test="${webhook.onSuccess}">On Success</c:if>
                <c:if test="${webhook.onFailure}">On Failure</c:if>
                <c:if test="${webhook.onStall}">On Stall</c:if>
                <c:if test="${webhook.buildLongerThan != null}">Longer than ${webhook.buildLongerThan}s</c:if>
                <c:if test="${webhook.buildLongerThanAverage}">Longer than average</c:if>
                <c:if test="${webhook.onFirstFailure}">On First Failure</c:if>
                <c:if test="${webhook.onBuildFixed}">On Build Fixed</c:if>
              </td>
              <td>
                <form action="/notifier/settings.html" method="post" style="display:inline;">
                  <input type="hidden" name="projectId" value="${projectId}"/>
                  <c:if test="${not empty buildTypeId}">
                    <input type="hidden" name="buildTypeId" value="${buildTypeId}"/>
                  </c:if>
                  <input type="hidden" name="action" value="delete"/>
                  <input type="hidden" name="webhookUrlToDelete" value="${webhook.url}"/>
                  <input type="submit" value="Delete" />
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>

    <!-- No global save needed; actions handled above -->
  </form>
</div>
