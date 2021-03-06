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

import juzu.MimeType;
import juzu.Resource;
import juzu.Response;

import juzu.impl.common.Tools;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.exception.AbstractEntityException;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;
import org.exoplatform.task.exception.UnAuthorizedOperationException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.util.ResourceUtil;
import org.gatein.common.text.EntityEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class StatusController extends AbstractController {

  private static final Log LOG = ExoLogger.getExoLogger(StatusController.class);

  @Inject
  ProjectService projectService;

  @Inject
  StatusService statusService;

  @Inject
  ResourceBundle bundle;

  @Resource
  @Ajax
  @MimeType.JSON
  public Response getAllStatus(Long projectId) throws EntityNotFoundException, JSONException {
    JSONArray array = new JSONArray();
    Project project = projectService.getProject(projectId);
    List<Status> statuses = Collections.emptyList();
    if (project.canView(ConversationState.getCurrent().getIdentity())) {
      statuses = new LinkedList<Status>(statusService.getStatuses(projectId));
    }

    Collections.sort(statuses);
    for (Status status : statuses) {
      JSONObject json = new JSONObject();
      json.put("value", status.getId());
      json.put("name", status.getName());
      json.put("text", ResourceUtil.resolveStatus(bundle, status.getName()));
      array.put(json);
    }

    return Response.ok(array.toString()).withCharset(Tools.UTF_8);
  }

  @Resource
  @Ajax
  @MimeType.JSON
  public Response updateStatus(Long id, String name) throws EntityNotFoundException, NotAllowedOperationOnEntityException, JSONException, UnAuthorizedOperationException {
    Status status = statusService.getStatus(id);
    if (!status.getProject().canEdit(ConversationState.getCurrent().getIdentity())) {
      throw new UnAuthorizedOperationException(id, Status.class, getNoPermissionMsg());
    }
    EntityEncoder encoder = HTMLEntityEncoder.getInstance();
    status = statusService.updateStatus(id, name);
    JSONObject json = new JSONObject();
    json.put("id", status.getId());
    json.put("name", encoder.encode(status.getName()));
    json.put("localizedName", encoder.encode(ResourceUtil.resolveStatus(bundle, status.getName())));
    json.put("rank", status.getRank());
    return Response.ok(json.toString()).withCharset(Tools.UTF_8);
  }

  @Resource
  @Ajax
  @MimeType.HTML
  public Response resolveStatusName(String name) {
    EntityEncoder encoder = HTMLEntityEncoder.getInstance();
    return Response.ok(encoder.encode(ResourceUtil.resolveStatus(bundle, name)));
  }
}
