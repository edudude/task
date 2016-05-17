package org.exoplatform.task.util;

import org.exoplatform.task.model.User;
import org.exoplatform.task.service.UserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 */
@RunWith(Parameterized.class)
public class CommentUtilTest {

  @Mock
  UserService userService;

  private String text;
  private String expectedText;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  public CommentUtilTest(String text, String expectedText) {
    this.text = text;
    this.expectedText = expectedText;
  }

  @Parameterized.Parameters
  public static Collection<String[]> data() {
    return Arrays.asList(new String[][]{
            {"Comment for @thomas", "Comment for <a href=\"http://exoplatform.com/thomas\">Thomas Delhom&eacute;nie</a>"},
            {"Comment with\nmultilines", "Comment with<br>multilines"},
            {"Comment for @thomas\nand\nmultilines.", "Comment for <a href=\"http://exoplatform.com/thomas\">Thomas Delhom&eacute;nie</a><br>and<br>multilines."},
            {"Comment with HTML tags <img>, <a> and <br>", "Comment with HTML tags &lt;img&gt;, &lt;a&gt; and &lt;br&gt;"}
    });
  }

  @Test
  public void testFormatMention() throws Exception {
    Mockito.when(userService.loadUser("thomas"))
            .thenReturn(new User("thomas", "thomas.delhomenie@exoplatform.com", "Thomas", "Delhoménie", "Thomas Delhoménie", "", "http://exoplatform.com/thomas"));

    Assert.assertEquals(expectedText, CommentUtil.formatComment(text, userService));
  }
}