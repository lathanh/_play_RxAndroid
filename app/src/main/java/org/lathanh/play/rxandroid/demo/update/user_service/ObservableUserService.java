package org.lathanh.play.rxandroid.demo.update.user_service;

import java.util.Collection;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * This wraps the platform-agnostic {@link UserService} to make it use the
 * Reactive, Observable pattern.
 *
 * Like the UserService, it is still platform/client-agnostic
 *
 * @author Robert LaThanh
 * @since 2017-03-01
 */
public class ObservableUserService {

  //== Dependencies ===========================================================

  private final UserService userService = new UserService();


  //== 'ObservableUserService' methods ========================================

  public Observable<UserService.User> getUserById(final long userId) {
    return Observable.fromCallable(
        new Callable<UserService.User>() {
          @Override
          public UserService.User call() throws Exception {
            return userService.getUserById(userId);
          }
        });
  }

  /**
   * A bulk-getter for Users (by ID) that uses an Observable to emits each
   * user.
   */
  public Observable<UserService.User> getUsersById(Collection<Long> userIds) {
    return
        Observable
            .fromIterable(userIds)
            .map(new Function<Long, UserService.User>() {
              @Override
              public UserService.User apply(Long id) throws Exception {
                return userService.getUserById(id);
              }
        });
  }

  public Observable<UserService.User> updateUser(final long id) {
    return Observable.fromCallable(new Callable<UserService.User>() {
      @Override
      public UserService.User call() throws Exception {
        return userService.updateUser(id);
      }
    });
  } // postChangeToDate()

}
