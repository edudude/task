package org.exoplatform.task.service;

import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.organization.impl.UserImpl;
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
  private UserHandler userHandler;

  @Mock
  private MembershipHandler membershipHandler;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private DAOHandler daoHandler;

  @Mock
  private CalendarService calendarService;

  @InjectMocks
  private UserServiceImpl userService;

  @Before
  public void setup() throws Exception {
    when(identityManager.getOrCreateIdentity(eq(OrganizationIdentityProvider.NAME), anyString(), anyBoolean()))
            .then(i -> new Identity(OrganizationIdentityProvider.NAME, i.getArgumentAt(1, String.class)));

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);

    when(organizationService.getUserHandler().findUsersByQuery(anyObject(), eq(UserStatus.ENABLED)))
            .thenReturn(new ListAccess<org.exoplatform.services.organization.User>() {
              org.exoplatform.services.organization.User[] users = new org.exoplatform.services.organization.User[] {
                      new UserImpl("user1"),
                      new UserImpl("user2"),
                      new UserImpl("user3")
              };

              @Override
              public org.exoplatform.services.organization.User[] load(int i, int i1) throws Exception, IllegalArgumentException {
                return users;
              }

              @Override
              public int getSize() throws Exception {
                return users.length;
              }
            });
    when(organizationService.getUserHandler().findUsersByGroupId("/group1"))
            .thenReturn(new ListAccess<org.exoplatform.services.organization.User>() {
      org.exoplatform.services.organization.User[] users = new org.exoplatform.services.organization.User[] {
              new UserImpl("user1")
      };

      @Override
      public org.exoplatform.services.organization.User[] load(int i, int i1) throws Exception, IllegalArgumentException {
        return users;
      }

      @Override
      public int getSize() throws Exception {
        return users.length;
      }
    });
    when(organizationService.getUserHandler().findUsersByGroupId("/group2"))
            .thenReturn(new ListAccess<org.exoplatform.services.organization.User>() {
      org.exoplatform.services.organization.User[] users = new org.exoplatform.services.organization.User[] {
              new UserImpl("user2")
      };

      @Override
      public org.exoplatform.services.organization.User[] load(int i, int i1) throws Exception, IllegalArgumentException {
        return users;
      }

      @Override
      public int getSize() throws Exception {
        return users.length;
      }
    });

    when(organizationService.getMembershipHandler().findMembershipsByUserAndGroup(eq("user1"), eq("/group1")))
            .thenReturn(Arrays.asList(new MembershipImpl("*:user1:/group1")));
    when(organizationService.getMembershipHandler().findMembershipsByUserAndGroup(eq("user2"), eq("/group2")))
            .thenReturn(Arrays.asList(new MembershipImpl("member:user2:/group2")));
  }

  @Test
  public void shouldReturnUsersWhenSearchingWithInputUsersMatch() throws Exception {
    // Given
    Collection<String> memberships = new ArrayList<>(Arrays.asList("user1", "user2", "user3", "test4"));

    // When
    ListAccess<User> userListAccess = userService.searchUsersInMemberships(memberships, "user");
    User[] users = userListAccess.load(0, userListAccess.getSize());

    // Then
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user1")));
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user2")));
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user3")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("test4")));
  }

  @Test
  public void shouldReturnUsersWhenSearchingWithInputUsersMembershipsMatch() throws Exception {
    // Given
    Collection<String> memberships = new ArrayList<>(Arrays.asList("*:/group1", "*:/group2"));

    // When
    ListAccess<User> userListAccess = userService.searchUsersInMemberships(memberships, "user");
    User[] users = userListAccess.load(0, userListAccess.getSize());

    // Then
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user1")));
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user2")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user3")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("test4")));



    // Given
    memberships = new ArrayList<>(Arrays.asList("*:/group1"));

    // When
    userListAccess = userService.searchUsersInMemberships(memberships, "user");
    users = userListAccess.load(0, userListAccess.getSize());

    // Then
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user1")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user2")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user3")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("test4")));


    // Given
    memberships = new ArrayList<>(Arrays.asList("manager:/group1", "manager:/group2"));

    // When
    userListAccess = userService.searchUsersInMemberships(memberships, "user");
    users = userListAccess.load(0, userListAccess.getSize());

    // Then
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user1")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user2")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user3")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("test4")));


    // Given
    memberships = new ArrayList<>(Arrays.asList("*:/group1", "member:/group2"));

    // When
    userListAccess = userService.searchUsersInMemberships(memberships, "user2");
    users = userListAccess.load(0, userListAccess.getSize());

    // Then
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user1")));
    assertTrue(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user2")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("user3")));
    assertFalse(Stream.of(users).map(user -> user.getUsername()).anyMatch(username -> username.equals("test4")));
  }

}