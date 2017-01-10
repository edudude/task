package org.exoplatform.task.service;

import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.model.User;
import org.exoplatform.task.service.impl.UserServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

/**
 * Test class for UserService
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

  @Mock
  private OrganizationService organizationService;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private DAOHandler daoHandler;

  @Mock
  private CalendarService calendarService;

  @InjectMocks
  private UserServiceImpl userService;

  @Before
  public void setup() {
    when(identityManager.getOrCreateIdentity(eq(OrganizationIdentityProvider.NAME), anyString(), anyBoolean()))
            .then(i -> new Identity(OrganizationIdentityProvider.NAME, i.getArgumentAt(1, String.class)));
    when(identityManager.getOrCreateIdentity(eq(OrganizationIdentityProvider.NAME), anyString(), anyBoolean()))
            .then(i -> new Identity(OrganizationIdentityProvider.NAME, i.getArgumentAt(1, String.class)));
  }

  @Test
  public void shouldReturnUsersWhenInputUsersMatch() throws Exception {
    // Given
    Collection<String> memberships = new ArrayList<>(Arrays.asList("user1", "user2", "user3", "test4"));

    // When
    ListAccess<User> userListAccess = userService.searchUsersInMemberships(memberships, "user");
    User[] users = userListAccess.load(0, userListAccess.getSize());

    // Then
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user1")));
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user2")));
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user3")));
    assertTrue(Stream.of(users).map(user -> user.getUsername()).noneMatch(username -> username.equals("test4")));
  }

}