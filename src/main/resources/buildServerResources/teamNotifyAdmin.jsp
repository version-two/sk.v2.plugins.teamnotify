<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>

<jsp:useBean id="allWebhooks" scope="request" type="java.util.List"/>
<jsp:useBean id="totalWebhooks" scope="request" type="java.lang.Integer"/>

<div class="section noMargin">
  <h2 class="noBorder">TeamNotify - Global Webhook Overview</h2>
  
  <div class="grayNote">
    This page shows all webhook notifications configured across all projects and build configurations in this TeamCity instance.
  </div>
  
  <div style="margin: 15px 0;">
    <strong>Total Webhooks Configured: ${totalWebhooks}</strong>
  </div>

  <c:choose>
    <c:when test="${totalWebhooks == 0}">
      <div class="attentionComment">
        No webhook notifications are currently configured in any project.
      </div>
    </c:when>
    <c:otherwise>
      <table class="settings parametersTable">
        <thead>
          <tr>
            <th class="name">Project</th>
            <th class="name">Webhook URL</th>
            <th class="name">Platform</th>
            <th class="name">Trigger Conditions</th>
            <th class="name">Actions</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="webhookInfo" items="${allWebhooks}">
            <tr>
              <td class="name">
                <strong>${webhookInfo.projectName}</strong><br/>
                <span class="smallNote">${webhookInfo.projectId}</span>
              </td>
              <td class="value">
                <div style="word-break: break-all; max-width: 300px;">
                  ${webhookInfo.webhook.url}
                </div>
              </td>
              <td class="name">
                <span class="badge">${webhookInfo.webhook.platform}</span>
              </td>
              <td class="value">
                <div class="conditions">
                  <c:if test="${webhookInfo.webhook.onSuccess}">
                    <span class="condition success">✓ On Success</span><br/>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onFailure}">
                    <span class="condition failure">✗ On Failure</span><br/>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onStall}">
                    <span class="condition stall">⏸ On Stall</span><br/>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.buildLongerThan != null}">
                    <span class="condition duration">⏱ Longer than ${webhookInfo.webhook.buildLongerThan}s</span><br/>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.buildLongerThanAverage}">
                    <span class="condition duration">📊 Longer than average</span><br/>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onFirstFailure}">
                    <span class="condition failure">🚨 First Failure</span><br/>
                  </c:if>
                  <c:if test="${webhookInfo.webhook.onBuildFixed}">
                    <span class="condition success">🔧 Build Fixed</span><br/>
                  </c:if>
                </div>
              </td>
              <td class="edit">
                <a href="/notifier/settings.html?projectId=${webhookInfo.projectId}" class="btn btn_mini">
                  Configure
                </a>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</div>

<style>
  .conditions {
    font-size: 12px;
    line-height: 1.4;
  }
  .condition {
    display: inline-block;
    padding: 2px 6px;
    margin: 1px 2px;
    border-radius: 3px;
    font-size: 11px;
  }
  .condition.success {
    background-color: #d4edda;
    color: #155724;
  }
  .condition.failure {
    background-color: #f8d7da;
    color: #721c24;
  }
  .condition.stall {
    background-color: #fff3cd;
    color: #856404;
  }
  .condition.duration {
    background-color: #d1ecf1;
    color: #0c5460;
  }
  .badge {
    background-color: #007cba;
    color: white;
    padding: 3px 8px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: bold;
    text-transform: uppercase;
  }
  .btn_mini {
    font-size: 11px;
    padding: 4px 8px;
  }
</style>
