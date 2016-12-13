/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.task.management.controller;

import javax.inject.Inject;

import java.util.*;

import juzu.MimeType;
import juzu.Resource;
import juzu.Response;
import juzu.impl.common.Tools;
import juzu.request.SecurityContext;

import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.domain.Label;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.model.User;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;
import org.exoplatform.task.exception.UnAuthorizedOperationException;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.service.UserService;
import org.exoplatform.task.util.ListUtil;
import org.exoplatform.task.util.UserUtil;
import org.gatein.common.text.EntityEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class UserController extends AbstractController {
    @Inject
    OrganizationService orgService;

    @Inject
    private UserService userService;

    @Inject
    private TaskService taskService;


  private List<User> loadUserFromList(ListAccess<User> list, int limit, Map<String, User> loadedUsers) {
    int size;
    List<User> result = new LinkedList<>();
    if (list == null || (size = ListUtil.getSize(list)) == 0) {
      return result;
    }

    int start = 0;
    while(result.size() < limit && start < size) {
      for (User u : ListUtil.load(list, start, limit)) {
        if (loadedUsers == null || !loadedUsers.containsKey(u.getUsername())) {
          result.add(u);
        }
      }
      start += limit;
    }
    return result;
  }

  private Collection<User> searchUserByTask(Long taskId, String query, int limit) throws EntityNotFoundException {
    // In this method we only search by task
    if (taskId == null || taskId <= 0) {
      return Collections.emptyList();
    }

    Task task = taskService.getTask(taskId);

    if (task.getStatus() == null) {
      // Personal task: we only return creator if match with query
      return loadUserFromList(userService.findByMembership(Arrays.asList(task.getCreatedBy()), query), 1, null);

    } else {
      //
      Project project = task.getStatus().getProject();
      Map<String, User> map = new LinkedHashMap<>();

      // We search in participants first
      ListAccess<User> participants = userService.findByMembership(project.getParticipator(), query);
      try {
        int size = participants.getSize();
        if (size < limit)
          for (User u : loadUserFromList(participants, size, null)) {
            map.put(u.getUsername(), u);
          }
        // If not enough, continue search in project's manager
        if (map.size() < limit) {
          ListAccess<User> managers = userService.findByMembership(project.getManager(), query);
          for (User u : loadUserFromList(managers, limit - map.size(), map)) {
            map.put(u.getUsername(), u);
          }
        }
      } catch (Exception e) {
        LOG.error("Exception when trying to retrieve the size of the participants list");
      }
      return map.values();
    }
  }

  /**
   * This method is used to find user in assignement
   * @param query
   * @return
   * @throws Exception
   */


    @Resource
    @Ajax
    @MimeType.JSON
  public Response findUser(Long taskId, String query) throws EntityNotFoundException, JSONException {
      Collection<User> users = searchUserByTask(taskId, query, UserUtil.SEARCH_LIMIT);
      JSONArray array = new JSONArray();
      for(User u : users) {
        JSONObject json = new JSONObject();
        json.put("id", u.getUsername());
        json.put("text", u.getDisplayName());
        json.put("avatar", u.getAvatar());
        array.put(json);
      }
      return Response.ok(array.toString());
    }

    @Resource
    @Ajax
    @MimeType.JSON
  public Response findUsersToMention(Long taskId, String query) throws EntityNotFoundException, JSONException {
      Collection<User> users = searchUserByTask(taskId, query, UserUtil.SEARCH_LIMIT);
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();
      JSONArray array = new JSONArray();
      for(User u : users) {
        JSONObject json = new JSONObject();
        json.put("id", "@" + u.getUsername());
        json.put("name", u.getDisplayName());
        json.put("avatar", u.getAvatar());
        json.put("type", "contact");
        array.put(json);
      }
      return Response.ok(array.toString());
    }

    @Resource
    @Ajax
    @MimeType.JSON
    public Response getDisplayNameOfUser(String usernames) throws JSONException {
      if(usernames != null) {
        JSONArray array = new JSONArray();
        for(String username : usernames.split(",")) {
          org.exoplatform.task.model.User user = userService.loadUser(username);
          JSONObject json = new JSONObject();
          json.put("id", user.getUsername());
          json.put("text", user.getDisplayName());
          json.put("avatar", user.getAvatar());
          json.put("deleted", user.isDeleted());
          json.put("enable", user.isEnable());
          array.put(json);
        }
        return Response.ok(array.toString()).withCharset(Tools.UTF_8);

      } else {
        return Response.ok("[]");
      }
    }

    @Resource
    @Ajax
    @MimeType("text/plain")
    public Response showHiddenProject(Boolean show, SecurityContext securityContext) {
        userService.showHiddenProject(securityContext.getRemoteUser(), show);
        return Response.ok("Update successfully");
    }

    @Resource
    @Ajax
    @MimeType("text/plain")
    public Response hideProject(Long projectId, Boolean hide) throws EntityNotFoundException, NotAllowedOperationOnEntityException, UnAuthorizedOperationException {
      Identity identity = ConversationState.getCurrent().getIdentity();
      try {
        userService.hideProject(identity, projectId, hide);
      } catch (NotAllowedOperationOnEntityException ex) {
        throw new UnAuthorizedOperationException(projectId, Project.class, getNoPermissionMsg());
      }
      return Response.ok("Hide project successfully");
    }

    @Resource
    @Ajax
    @MimeType.JSON
    public Response findLabel(SecurityContext securityContext) throws JSONException {
      String username = securityContext.getRemoteUser();      
      ListAccess<Label> tmp = taskService.findLabelsByUser(username);
      List<Label> labels = Arrays.asList(ListUtil.load(tmp, 0, -1));

      JSONArray array = new JSONArray();
      if (labels != null) {
        for (Label label : labels) {
          JSONObject json = new JSONObject();
          json.put("id", label.getId());
          json.put("text", label.getName());
          json.put("name", label.getName());
          json.put("color", label.getColor());

          array.put(json);
        }
      }
      return Response.ok(array.toString()).withCharset(Tools.UTF_8);
    }
}
