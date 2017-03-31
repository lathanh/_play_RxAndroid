package org.lathanh.play.rxandroid.demo.loadable.user_service;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;
import org.lathanh.play.rxandroid.demo.update.user_service.ObservableUserService;
import org.lathanh.play.rxandroid.demo.update.user_service.UserService;

import java.util.WeakHashMap;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * For the "Loadable" demo, I create a new AndroidUserService (instead of using
 * the "Update" demo's
 * {@link org.lathanh.play.rxandroid.demo.update.user_service.AndroidUserService},
 * because I wanted to be able to return a container for the User immediately —
 * upon the getUser call — so that the container can be used right away (viz.,
 * in a row of the list).
 * This allows the view to be ready right away (with a placeholder), and when
 * the user is fetched and placed into the container, the view can then
 * automatically update.
 *
 * @author Robert LaThanh
 * @since 2017-03-03
 */
public class AndroidUserService {

  //== Public inner classes ===================================================

  /**
   * "LOC" = Loadable, Observable Container
   *   * Container: Rather than returning the User object directly, this object
   *     which can contain the User object is returned instead.
   *     This allows to User to be provided at a later time, and changed.
   *   * Loadable: The User object within the container can initially be null
   *     while the User is fetched. Once the User is fetched, it can be set —
   *     or "loaded" — into the container.
   *     There is also a LoadingState field that provides detail on the status
   *     of the User object.
   *   * Observable: The field for the User (and the LoadingState field) are
   *     Observable, so the consumer may subscribe to changes.
   *     This allows the consumer to, for example, automatically update views
   *     upon loading/updating of the User object.
   */
  public static class UserLoc extends BaseObservable {
    @Nullable private LoadingState loadingState;
    @Nullable private UserService.User user;

    @Bindable
    @Nullable
    public LoadingState getLoadingState() {
      return loadingState;
    }

    public void setLoadingState(@Nullable LoadingState loadingState) {
      this.loadingState = loadingState;
      this.notifyPropertyChanged(BR.loadingState);
    }

    @Bindable
    @Nullable
    public UserService.User getUser() {
      return user;
    }

    public void setUser(@Nullable UserService.User user) {
      this.user = user;
      this.notifyPropertyChanged(BR.user);
    }
  }


  //== Operating fields =======================================================

  private final ObservableUserService observableUserService = new ObservableUserService();
  private final WeakHashMap<Long, UserLoc> objectsInUse = new WeakHashMap<>();


  //== 'AndroidUserService' methods ===========================================

  /**
   * @return
   *     * First: Observable&lt;UserService.User&gt;: An Observable that will
   *       fetch the User when {@link Observable#subscribe() subscribed} to.
   *     * Second: UserLoc: A 'Loadable, Observable Container' into which the
   *       User will be set upon successful fetch.
   */
  public Pair<Observable<UserService.User>, UserLoc> getUser(long id) {
    // Get the existing container for this user (ID), or create a new one
    // So, there should only be on container out there per user, and if/when
    // the User is updated, we can update the User in that container
    final UserLoc userLoc;
    UserLoc existing = objectsInUse.get(id);
    if (existing != null) {
      userLoc = existing;
    } else {
      userLoc = new UserLoc();
      objectsInUse.put(id, userLoc);
    }

    Observable<UserService.User> observable =
        observableUserService.getUserById(id)
            .doOnSubscribe(new Consumer<Disposable>() {
              @Override
              public void accept(Disposable disposable) throws Exception {
                userLoc.setLoadingState(LoadingState.LOADING);
              }
            })
            .doOnNext(new Consumer<UserService.User>() {
              @Override
              public void accept(UserService.User user) throws Exception {
                userLoc.setUser(user);
                userLoc.setLoadingState(LoadingState.DATA);
              }
            });
    return new Pair<>(observable, userLoc);
  }

  public Observable<UserLoc> updateUser(final long id) {
    final UserLoc objectInUse = objectsInUse.get(id);
    if (objectInUse != null) {
      objectInUse.setLoadingState(LoadingState.UPDATING);
    }

    return observableUserService.updateUser(id)
        .map(new Function<UserService.User, UserLoc>() {
          @Override
          public UserLoc apply(UserService.User user)
              throws Exception {
            if (objectInUse != null) {
              objectInUse.setUser(user);
              objectInUse.setLoadingState(LoadingState.DATA);
              return objectInUse;
            } else {
              UserLoc newUserLoc = new UserLoc();
              newUserLoc.setUser(user);
              newUserLoc.setLoadingState(LoadingState.DATA);
              objectsInUse.put(id, newUserLoc);
              return newUserLoc;
            }
          }
        });
  }
}
