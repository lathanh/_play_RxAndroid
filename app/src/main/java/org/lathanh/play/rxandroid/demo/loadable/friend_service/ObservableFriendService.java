package org.lathanh.play.rxandroid.demo.loadable.friend_service;

import java.util.concurrent.Callable;

import io.reactivex.Observable;

/**
 * This wraps the platform-agnostic {@link FriendService} to make it use the
 * Reactive, Observable pattern.
 *
 * Like the UserService, it is still platform/client-agnostic
 * @author Robert LaThanh
 * @since 2017-03-02
 */

public class ObservableFriendService {

  //== Dependencies ===========================================================

  private final FriendService friendService = new FriendService();


  //== Public 'ObservableUserService' methods =================================

  public Observable<FriendService.GetFriendsResponse> getFriends(
      final FriendService.GetFriendsRequest getFriendsRequest) {
    return Observable.fromCallable(
        new Callable<FriendService.GetFriendsResponse>() {
          @Override
          public FriendService.GetFriendsResponse call() throws Exception {
            return friendService.getFriends(getFriendsRequest);
          }
        }
    );
  }

}
