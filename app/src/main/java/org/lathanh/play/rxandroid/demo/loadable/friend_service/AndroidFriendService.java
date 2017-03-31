package org.lathanh.play.rxandroid.demo.loadable.friend_service;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.Nullable;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;
import org.lathanh.play.rxandroid.demo.loadable.friend_service.FriendService.GetFriendsResponse;

import java.util.WeakHashMap;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * Basically like the
 * {@link org.lathanh.play.rxandroid.demo.loadable.user_service.AndroidUserService},
 * except that instead of provideing Users, it provides a list of friends (a
 * list of Users).
 *
 * @author Robert LaThanh
 * @since 2017-03-02
 */
public class AndroidFriendService {

  //== Public inner classes ===================================================

  public static class GetFriendsLoc extends BaseObservable {
    @Nullable private LoadingState loadingState;
    @Nullable private GetFriendsResponse getFriendsResponse;

    @Bindable
    @Nullable
    public LoadingState getLoadingState() {
      return loadingState;
    }
    void setLoadingState(@Nullable LoadingState loadingState) {
      this.loadingState = loadingState;
      notifyPropertyChanged(BR.loadingState);
    }

    @Bindable
    @Nullable
    public GetFriendsResponse getGetFriendsResponse() {
      return getFriendsResponse;
    }

    public void setGetFriendsResponse(GetFriendsResponse getFriendsResponse) {
      this.getFriendsResponse = getFriendsResponse;
      notifyPropertyChanged(BR.getFriendsResponse);
    }
  }


  //== Operating fields =======================================================

  private final ObservableFriendService observableFriendService = new ObservableFriendService();
  private final WeakHashMap<FriendService.GetFriendsRequest, GetFriendsLoc> objectsInUse = new WeakHashMap<>();


  //== 'AndroidFriendService' methods =========================================

  public Observable<GetFriendsLoc> getFriends(FriendService.GetFriendsRequest
                                                  getFriendsRequest) {
    final GetFriendsLoc getFriendsLoc;
    GetFriendsLoc existing = objectsInUse.get(getFriendsRequest);
    if (existing != null) {
      getFriendsLoc = existing;
    } else {
      getFriendsLoc = new GetFriendsLoc();
      objectsInUse.put(getFriendsRequest, getFriendsLoc);
    }

    return observableFriendService
        .getFriends(getFriendsRequest)
        .doOnSubscribe(new Consumer<Disposable>() {
          @Override
          public void accept(Disposable disposable) throws Exception {
            getFriendsLoc.setLoadingState(LoadingState.LOADING);
          }
        })
        .map(new Function<GetFriendsResponse, GetFriendsLoc>() {
          @Override
          public GetFriendsLoc apply(
              GetFriendsResponse getFriendsResponse) throws Exception {
            getFriendsLoc.setGetFriendsResponse(getFriendsResponse);
            getFriendsLoc.setLoadingState(LoadingState.DATA);
            return getFriendsLoc;
          }
        });
  }

}
