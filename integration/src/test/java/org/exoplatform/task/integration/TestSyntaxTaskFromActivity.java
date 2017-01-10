/**
 * Copyright (C) 2017 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 **/
  
package org.exoplatform.task.integration;

import org.junit.Assert;
import org.junit.Test;

public class TestSyntaxTaskFromActivity {

  @Test
  public void testExtractTaskInfo() {
    String html = "we need to ++download new documentation from cloud<br>" +
        "It helps you understand better what's happening";
    String[] taskInfo = ActivityTaskCreationListener.extractTaskInfo(html);
    Assert.assertEquals("download new documentation from cloud", taskInfo[0]);
    Assert.assertEquals("It helps you understand better what's happening", taskInfo[1]);

    html = "we need to ++download new <strong>documentation from <i>cloud<br>" +
        "It helps you</i> understand</strong> better what's happening";
    taskInfo = ActivityTaskCreationListener.extractTaskInfo(html);
    Assert.assertEquals("download new documentation from cloud", taskInfo[0]);
    Assert.assertEquals("It helps you</i> understand</strong> better what's happening", taskInfo[1]);

    html = "we need to ++download new <strong>documentation from cloud<br>" +
        "It helps you understand</strong> better what's happening";
    taskInfo = ActivityTaskCreationListener.extractTaskInfo(html);
    Assert.assertEquals("download new documentation from cloud", taskInfo[0]);
    Assert.assertEquals("It helps you understand</strong> better what's happening", taskInfo[1]);
  }

  @Test
  public void testSubstituteTask() {
    String taskURL = "/link/to/task";

    String html = "we need to ++download new documentation from cloud<br>" +
        "It helps you understand better what's happening";
    String expected = "we need to <a href=\"/link/to/task\">++download new documentation from cloud</a><br>" +
        "It helps you understand better what's happening";
    Assert.assertEquals(expected, ActivityTaskProcessor.substituteTask(taskURL, html));

    html = "we need to ++download new <strong>documentation from cloud<br>" +
        "It helps you understand</strong> better what's happening";
    expected = "we need to <a href=\"/link/to/task\">++download new </a><strong>documentation from cloud<br>" +
        "It helps you understand</strong> better what's happening";
    Assert.assertEquals(expected, ActivityTaskProcessor.substituteTask(taskURL, html));
  }
}