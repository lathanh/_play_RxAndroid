package org.lathanh.play.rxandroid.demo.loadable.friend_service;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a remote service API that takes time to perform its
 * operations.
 * Like the
 * {@link org.lathanh.play.rxandroid.demo.update.user_service.UserService}, it
 * is platform/implementation agnostic
 *
 * The Friend service provides a list of friends in the form of a list of user
 * IDs.
 * Each of those Users may be fetched from the UserService.
 *
 * Also for this implementation, requests take the form of an object (rather
 * than a set of method parameters). Just for the heck of it.
 *
 * @author Robert LaThanh
 * @since 2017-03-02
 */

public class FriendService {

  //== Private constants ======================================================

  private static final long LOAD_DELAY_MS = 700;


  //== Public inner classes ===================================================

  /** Represents a request to get a list of friends. */
  public static class GetFriendsRequest {
    public final long userId;
    public final int perPage;
    public final int page;

    public GetFriendsRequest(long userId, int perPage, int page) {
      this.userId = userId;
      this.perPage = perPage;
      this.page = page;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof GetFriendsRequest)) return false;
      GetFriendsRequest other = (GetFriendsRequest) obj;
      return userId == other.userId
          && perPage == other.perPage
          && page == other.page;
    }
  }

  /** Represents a response for the request to get a list of friends. */
  public static class GetFriendsResponse {
    public final List<Long> friendUserIds;
    public final int firstIndex;
    public final int lastIndex;
    public final int totalFriendCount;

    private GetFriendsResponse(List<Long> friendUserIds, int firstIndex,
                              int lastIndex, int totalFriendCount) {
      this.friendUserIds = friendUserIds;
      this.firstIndex = firstIndex;
      this.lastIndex = lastIndex;
      this.totalFriendCount = totalFriendCount;
    }
  }


  //== API methods ============================================================

  public GetFriendsResponse getFriends(GetFriendsRequest getFriendsRequest) {
    try {
      Thread.sleep(LOAD_DELAY_MS);
    } catch (InterruptedException e) {
      // I was somehow able to get more sleep!
    }

    List<Long> friendIds = new ArrayList<>(getFriendsRequest.perPage);
    final int firstIndex = getFriendsRequest.perPage * (getFriendsRequest.page - 1) + 1;
    int i;
    long numTotalFriends = getFriendsRequest.userId; // just base it off of userId
    for (i = firstIndex;
         i <= numTotalFriends
             && i <= getFriendsRequest.page * getFriendsRequest.perPage;
         i++) {
      friendIds.add((long) i);
    }

    return new GetFriendsResponse(friendIds, firstIndex, i - 1,
                                  (int) numTotalFriends);
  }

}
