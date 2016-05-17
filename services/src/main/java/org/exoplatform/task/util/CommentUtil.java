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

package org.exoplatform.task.util;

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.task.model.User;
import org.exoplatform.task.service.UserService;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public final class CommentUtil {

  // regex allowing to return tokens and delimiters
  public static final String WITH_DELIMITER = "((?<=[%1$s])|(?=[%1$s]))";

  private CommentUtil() {
  }

  public static String formatComment(String text, UserService userService) {
    if(text == null || text.isEmpty()) {
      return text;
    }
    HTMLEntityEncoder encoder = HTMLEntityEncoder.getInstance();
    StringBuilder sb = new StringBuilder();

    String[] textParts = text.split(String.format(WITH_DELIMITER, "\\s\\t\\n\\r\\f"));
    for (String next : textParts) {
      if(next.length() == 0) {
        continue;
      } else if(next.charAt(0) == '@') {
        String username = next.substring(1);
        User user = userService.loadUser(username);
        if(user != null && !"guest".equals(user.getUsername())) {
          next = "<a href=\"" + user.getUrl() + "\">" + encoder.encodeHTML(user.getDisplayName()) + "</a>";
        } else {
          next = encoder.encodeHTML(next);
        }
      } else if(next.equals("\n") || next.equals("\n\r")) {
        next = "<br>";
      } else {
        next = encoder.encodeHTML(next);
      }
      sb.append(next);
    }

    return sb.toString();
  }
}
