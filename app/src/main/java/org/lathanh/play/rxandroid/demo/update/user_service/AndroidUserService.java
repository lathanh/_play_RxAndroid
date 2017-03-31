package org.lathanh.play.rxandroid.demo.update.user_service;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.NonNull;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;
import org.lathanh.play.rxandroid.demo.update.user_service.UserService.User;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.WeakHashMap;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * This wraps the platform-agnostic {@link ObservableUserService} to make it
 * useful for Android.
 *
 * It Android Data Binding Observable objects so the service can easily update
 * objects (in addition to the objects being easily bound to the view).
 * In other words, a container (which can contain the User object) is returned.
 * That way, when a new, updated User object is received from the UserService
 * (via the ObservableUserService), we have a way to easily update clients that
 * already have a previous version of the User: we just swap in the new User
 * object in the UserObservable container that we previously gave to them.
 *
 * So, this means keeping (weak) references to every container that we return.
 *
 * Note that for simplicity of this demo, the UserObservable container is not
 * created/returned until the User has been retrieved (via the
 * ObservableUserService), so this implementation will never return a container
 * without a User in it (that is, User will never be null).
 *
 * @author Robert LaThanh
 * @since 2017-02-27
 */
public class AndroidUserService {

  //== Inner classes ==========================================================

  /**
   * Instead of directly returning Users, we return this container.
   * By keeping a (weak) reference of all containers that we've returned, we can
   * put a new, updated User into containers when a User gets updated, and the
   * client (who we gave the container to) will automatically have the updated
   * User.
   * The client, by subscribing to the User field in the container, can be
   * notified so it can automatically, say, update the view.
   */
  public static class UserObservable extends BaseObservable {

    @NonNull private LoadingState loadingState;
    @NonNull private User user;

    private UserObservable(@NonNull User user) {
      this.loadingState = LoadingState.DATA;
      this.user = user;
    }

    @NonNull
    @Bindable
    public LoadingState getLoadingState() {
      return loadingState;
    }

    void setLoadingState(@NonNull LoadingState loadingState) {
      this.loadingState = loadingState;
      notifyPropertyChanged(BR.loadingState);
    }

    @NonNull
    @Bindable
    public User getUser() {
      return user;
    }

    void setUser(@NonNull User user) {
      this.user = user;
      notifyPropertyChanged(BR.user);
      notifyPropertyChanged(BR.dateString);
    }

    @Bindable
    public String getDateString() {
      DateFormat dateTimeInstance =
          SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
      return dateTimeInstance.format(user.getLastUpdate());
    }
  } // class UserObservable


  //== Instance fields ========================================================

  private final ObservableUserService observableUserService = new ObservableUserService();
  private final WeakHashMap<Long, UserObservable> objectsInUse = new WeakHashMap<>();


  //== Public 'AndroidUserService' methods ====================================

  public Observable<UserObservable> getUsersById(Collection<Long> userIds) {
    return observableUserService.getUsersById(userIds)
        .map(new Function<User, UserObservable>() {
          @Override
          public UserObservable apply(User user)
              throws Exception {
            return createOrUpdateUserObservable(user);
          }
        });
  }

  public Observable<UserObservable> updateUser(long id) {
    UserObservable objectInUse = objectsInUse.get(id);
    if (objectInUse != null) {
      objectInUse.setLoadingState(LoadingState.UPDATING);
    }

    return observableUserService.updateUser(id)
        .map(new Function<User, UserObservable>() {
          @Override
          public UserObservable apply(User user)
              throws Exception {
            return createOrUpdateUserObservable(user);
          }
        });
  }


  //== Private 'AndroidUserService' methods ===================================

  /**
   * Each time we get a new/updated user, see if we've already created a
   * container for it (that we've given to clients).
   * If so, re-use that container (so there should only ever be one container
   * per UserId), and update the User inside it.
   * So, multiple calls to get the same user (by ID) will also receive the same
   * container.
   */
  private UserObservable createOrUpdateUserObservable(User user) {
    UserObservable objectInUse = objectsInUse.get(user.getId());
    if (objectInUse != null) {
      objectInUse.setUser(user);
      objectInUse.setLoadingState(LoadingState.DATA);
      return objectInUse;
    } else {
      UserObservable userObservable =
          new UserObservable(user);
      userObservable.setLoadingState(LoadingState.DATA);
      objectsInUse.put(user.getId(), userObservable);
      return userObservable;
    }
  } // createOrUpdateUserObservable()

}
