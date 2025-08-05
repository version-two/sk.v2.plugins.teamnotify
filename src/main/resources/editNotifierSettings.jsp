<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<div class="editBuildFeatures">
  <h2>Webhook Notifier Settings</h2>

  <c:if test="${param.message != null}">
    <div class="success-message" style="color: green; margin-bottom: 10px;">${param.message}</div>
  </c:if>

  <form id="webhookSettingsForm" action="" method="post">
    <input type="hidden" name="projectId" value="${projectId}"/>
    <div class="settings-group">
      <h3>Add New Webhook</h3>
      <div class="field-group">
        <label for="webhookUrl">Webhook URL:</label>
        <props:textProperty name="webhookUrl" className="long-field" />
      </div>
      <div class="field-group">
        <label for="conditions">Conditions:</label>
        <props:checkbox name="onSuccess" label="On Success" />
        <props:checkbox name="onFailure" label="On Failure" />
        <props:checkbox name="onStall" label="On Stall" />
        <props:checkbox name="buildLongerThanAverage" label="Build longer than average" />
      </div>
      <div class="field-group">
        <label for="buildLongerThan">Build longer than (seconds):</label>
        <props:textProperty name="buildLongerThan" className="long-field" />
      </div>
      <div class="field-group">
        <forms:submit label="Add Webhook" />
      </div>
    </div>

    <div class="settings-group">
      <h3>Existing Webhooks</h3>
      <table class="settings-table">
        <thead>
          <tr>
            <th>Webhook URL</th>
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
                <c:if test="${webhook.onSuccess}">On Success</c:if>
                <c:if test="${webhook.onFailure}">On Failure</c:if>
                <c:if test="${webhook.onStall}">On Stall</c:if>
                <c:if test="${webhook.buildLongerThan != null}">Longer than ${webhook.buildLongerThan}s</c:if>
                <c:if test="${webhook.buildLongerThanAverage}">Longer than average</c:if>
                <c:if test="${webhook.onFirstFailure}">On First Failure</c:if>
                <c:if test="${webhook.onBuildFixed}">On Build Fixed</c:if>
              </td>
              <td>
                <form action="" method="post" style="display:inline;">
                  <input type="hidden" name="projectId" value="${projectId}"/>
                  <input type="hidden" name="action" value="delete"/>
                  <input type="hidden" name="webhookUrlToDelete" value="${webhook.url}"/>
                  <forms:submit label="Delete" />
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>

    <forms:saving id="saving" />
    <forms:submit label="Save" />
  </form>
</div>
