/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.task.integration.notification;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.DigestTemplate.ElementType;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.task.service.UserService;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = TaskAssignPlugin.ID, template = "war:/notification/templates/mail/TaskAssignPlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskCoworkerPlugin.ID, template = "war:/notification/templates/mail/TaskCoworkerPlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskDueDatePlugin.ID, template = "war:/notification/templates/mail/TaskDueDatePlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskCompletedPlugin.ID, template = "war:/notification/templates/mail/TaskCompletedPlugin.gtmpl")})
public class MailTemplateProvider extends TemplateProvider {

  public MailTemplateProvider(InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(TaskAssignPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskCoworkerPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskDueDatePlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskCompletedPlugin.ID), new TemplateBuilder());
  }
  
  private class TemplateBuilder extends AbstractTemplateBuilder {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {     
      NotificationInfo notification = ctx.getNotificationInfo();
      String language = getLanguage(notification);
      String creator = notification.getValueOwnerParameter(NotificationUtils.CREATOR.getKey());
      String projectName = notification.getValueOwnerParameter(NotificationUtils.PROJECT_NAME);
      String taskTitle = notification.getValueOwnerParameter(NotificationUtils.TASK_TITLE);
      String taskDesc = notification.getValueOwnerParameter(NotificationUtils.TASK_DESCRIPTION);
      String taskUrl = notification.getValueOwnerParameter(NotificationUtils.TASK_URL);
      String projectUrl = notification.getValueOwnerParameter(NotificationUtils.PROJECT_URL);

      TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
      Identity author = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, creator, true);
      Profile profile = author.getProfile();
      //creator
      templateContext.put("USER", profile.getFullName());
      templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", author.getRemoteId()));
      //receiver
      Identity receiver = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getTo(), true);
      templateContext.put("FIRST_NAME", receiver.getProfile().getProperty(Profile.FIRST_NAME));
      //
      templateContext.put("PROJECT_NAME", projectName == null ? "" : projectName);
      templateContext.put("TASK_TITLE", taskTitle);
      templateContext.put("TASK_DESCRIPTION", getExcerpt(taskDesc, 130));
      templateContext.put("DUE_DATE", getDueDate(notification));
      templateContext.put("TASK_URL", taskUrl);
      templateContext.put("PROJECT_URL", projectUrl);
      //
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));
      String subject = TemplateUtils.processSubject(templateContext);      

      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(subject).body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      List<NotificationInfo> notifications = ctx.getNotificationInfos();
      NotificationInfo first = notifications.get(0);

      String language = getLanguage(first);
      TemplateContext templateContext = new TemplateContext(first.getKey().getId(), language);
      //
      Identity receiver = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, first.getTo(), true);
      templateContext.put("FIRST_NAME", receiver.getProfile().getProperty(Profile.FIRST_NAME));
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));
      
      try {
        writer.append(buildDigestMsg(notifications, templateContext));
      } catch (IOException e) {
        ctx.setException(e);
        return false;
      }
      return true;
    }

    private String buildDigestMsg(List<NotificationInfo> notifications, TemplateContext templateContext) {      
      Map<String, List<NotificationInfo>> map = new HashMap<String, List<NotificationInfo>>();
      for (NotificationInfo notif : notifications) {
        String activityID = notif.getValueOwnerParameter(NotificationUtils.ACTIVITY_ID);
        List<NotificationInfo> tmp = map.get(activityID);
        if (tmp == null) {
          tmp = new LinkedList<NotificationInfo>();
          map.put(activityID, tmp);
        }
        tmp.add(notif);
      }
      
      StringBuilder sb = new StringBuilder();
      for (String activityID : map.keySet()) {
        List<NotificationInfo> notifs = map.get(activityID);
        NotificationInfo first = notifs.get(0);
        
        String projectName = first.getValueOwnerParameter(NotificationUtils.PROJECT_NAME);
        if (projectName != null) {
          PluginConfig config = CommonsUtils.getService(PluginSettingService.class).getPluginConfig(templateContext.getPluginId());
          String resourcePath = config.getBundlePath();
          Locale locale = org.exoplatform.commons.notification.NotificationUtils.getLocale(templateContext.getLanguage());
          String inProject = TemplateUtils.getResourceBundle("Notification.message.inProject", locale, resourcePath);
          templateContext.put("PROJECT_NAME", inProject.replace("{0}", "<a href=\"#\" style=\"text-decoration: none; color: #2f5e92; font-family: 'HelveticaNeue Bold', Helvetica, Arial, sans-serif\"><strong>" + 
              projectName + "</strong></a>"));
        } else {
          templateContext.put("PROJECT_NAME", "");
        }
        
        String taskTitle = "";
        if (notifs.size() == 1) {
          taskTitle = first.getValueOwnerParameter(NotificationUtils.TASK_TITLE);
          templateContext.digestType(ElementType.DIGEST_ONE.getValue());
        } else {
          templateContext.digestType(ElementType.DIGEST_MORE.getValue());
        }
        templateContext.put("TASK_TITLE", "<a href=\"#\" style=\"text-decoration: none; color: #2f5e92; font-family: 'HelveticaNeue Bold', Helvetica, Arial, sans-serif\">" + getExcerpt(taskTitle, 30) + "</a>");
        templateContext.put("COUNT", "<a href=\"#\" style=\"text-decoration: none; color: #2f5e92; font-family: 'HelveticaNeue Bold', Helvetica, Arial, sans-serif\">" + String.valueOf(notifs.size()) + "</a>");
        templateContext.put("DUE_DATE", getDueDate(first));
        
        sb.append("<li style=\"color: #2f5e92; margin: 0 0 10px\"><div style=\"color: #333;\">");
        String digester = TemplateUtils.processDigest(templateContext);
        sb.append(digester);
        sb.append("</div></li>");
      }

      return sb.toString();
    }

    private String getDueDate(NotificationInfo notification) {
      String dueDate = notification.getValueOwnerParameter(NotificationUtils.DUE_DATE);
      if (dueDate != null) {
        Date date = new Date(Long.parseLong(dueDate));
        UserService userService = CommonsUtils.getService(UserService.class);
        return org.exoplatform.task.integration.notification.TemplateUtils.format(date, userService.getUserTimezone(notification.getTo()));        
      } else {
        return "";
      }
    }
  };

  public static String getExcerpt(String str, int len) {
    if (str == null) {
      return "";
    } else if (str.length() > len) {
      str = str.substring(0, len);
      int lastSpace = str.lastIndexOf(" ");
      return ((lastSpace > 0) ? str.substring(0, lastSpace) : str) + "...";
    } else {
      return str;      
    }
  }

}