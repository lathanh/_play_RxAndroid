package org.lathanh.play.rxandroid.demo.loadable;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;
import org.lathanh.play.rxandroid.databinding.LoadableDemoFragmentBinding;
import org.lathanh.play.rxandroid.databinding.LoadableDemoFriendListItemBinding;
import org.lathanh.play.rxandroid.demo.loadable.friend_service.AndroidFriendService;
import org.lathanh.play.rxandroid.demo.loadable.user_service.AndroidUserService;
import org.lathanh.play.rxandroid.demo.loadable.friend_service.FriendService;
import org.lathanh.play.rxandroid.demo.loadable.friend_service.FriendService.GetFriendsResponse;
import org.lathanh.play.rxandroid.demo.update.user_service.UserService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Like
 * {@link org.lathanh.play.rxandroid.demo.view_model.ViewModelDemoFragment},
 * but
 *   1. Fetches the list of friends (just their User IDs) separately from the
 *      User data model (of each friend).
 *        * While the list is loading, shows a progress spinner. This is
 *          accomplished by binding (Android Data Binding) the progress spinner
 *          and the list to the friend-loading observable.
 *   2. Shows each item (Friend/User) before the data for the item has actually
 *      loaded.
 *
 * Demonstrates:
 *   * Fetching the list of friends (just their User IDs) independently from
 *     the User data model of each friend.
 *     So, the list and a placeholder for each item can be shown as soon as the
 *     list is returned even though the data for each item has not been fetched
 *     yet.
 *   * Showing a placeholder for an item until the Data Model (User) for it has
 *     been loaded.
 *
 * Implementation Notes:
 *   * In order for a placeholder to be shown for an item, we need the service
 *     (that will provide the data) to give us a container for that data right
 *     away (that the data will be placed into).
 *     If we also want to control how the Observable (that will fetch that data)
 *     will be subscribed to (including on which Scheduler), then that service
 *     also needs to provide us with the Observable.
 *     So, for this implementation, the service returns a pair.
 *     The first element of the pair, a container (an "LOC", or Loadable
 *     Observable Container), is given to the Adapter on the UI thread.
 *     The second element of the pair, the Observable that will actually fetch
 *     the data (and then put it into the LOC), is then scheduled on the IO
 *     Scheduler.
 *
 * @author Robert LaThanh
 * @since 2017-03-01
 */
public class LoadableDemoFragment extends Fragment {

  //== Private constants ======================================================

  private static final int NUM_ITEMS_TO_GET = 50;


  //== Operating fields =======================================================

  //-- Dependencies
  private final AndroidFriendService androidFriendService = new AndroidFriendService();
  private final AndroidUserService androidUserService = new AndroidUserService();

  //-- Operating fields
  // Page View Model
  private AndroidFriendService.GetFriendsLoc getFriendsLoc;
  private LoadableDemoFragmentBinding binding;

  // List
  private final List<FriendItemVm> friendItemVms = new ArrayList<>();
  private ViewModelAdapter adapter;


  //== 'Fragment' methods =====================================================

  /** Begin the loading of the friends. */
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //-- Fetch the list of friends
    io.reactivex.Observable<AndroidFriendService.GetFriendsLoc> getFriendsLocObservable =
        androidFriendService.getFriends(
            new FriendService.GetFriendsRequest(NUM_ITEMS_TO_GET, 25, 1));
    getFriendsLocObservable
        .subscribeOn(Schedulers.io())
        /** Receive the list of friends. */
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Consumer<AndroidFriendService.GetFriendsLoc>() {
          @Override
          public void accept(AndroidFriendService.GetFriendsLoc getFriendsLoc)
              throws Exception {
            LoadableDemoFragment.this.getFriendsLoc = getFriendsLoc;
            if (binding != null) binding.setGetFriends(getFriendsLoc);
          }
        })
        /**
         * ... have the friend list.
         * Create observable to go over each friend (ID)
         */
        .observeOn(Schedulers.io())
        .flatMap(new Function<AndroidFriendService.GetFriendsLoc, ObservableSource<Long>>() {
          @Override
          public ObservableSource<Long> apply(AndroidFriendService.GetFriendsLoc getFriendsLoc)
              throws Exception {
            GetFriendsResponse getFriendsResponse =
                getFriendsLoc.getGetFriendsResponse();
            return io.reactivex.Observable.fromIterable(getFriendsResponse.friendUserIds);
          }
        })
        /**
         * ... have friend user ID (iteration).
         * 1. Create the view model for that friend and add to the adapter
         * 2. Create Observable to request the user, but pass that to another
         *    thread to actually perform.
         */
        .observeOn(AndroidSchedulers.mainThread())
        .map(new Function<Long, io.reactivex.Observable<UserService.User>>() {
          @Override
          public io.reactivex.Observable<UserService.User> apply(
              Long friendUserId)
              throws Exception {
            // generate observable request for the friend
            Pair<io.reactivex.Observable<UserService.User>, AndroidUserService.UserLoc> pair =
                androidUserService.getUser(friendUserId);

            // add the request LOC to the adapter
            FriendItemVm friendItemVm = new FriendItemVm(pair.second);
            friendItemVms.add(friendItemVm);
            if (adapter != null) {
              adapter.notifyItemInserted(friendItemVms.size() - 1);
            }

            // pass the observable onto the next operator for it to be actually
            // performed
            return pair.first;
          }
        })
        /**
         * Take the observable that the androidUserService gave us, and now
         * actually have it subscribed to (fetches the User)
         * The User will automatically be placed in the UserLoc that we were
         * also given (and the view will automatically be updated since the
         * view subscribes to the UserLoc).
         */
        .observeOn(Schedulers.io())
        .concatMap(
            new Function<io.reactivex.Observable<UserService.User>, ObservableSource<?>>() {
              @Override
              public ObservableSource<?> apply(
                  io.reactivex.Observable<UserService.User> userObservable)
                  throws Exception {
                return userObservable;
              }
            })
        .subscribe();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    // The loading of the list of friends (just their userIds) determines
    // the state of the whole fragment; whether it should should show a
    // progress spinner or the list
    binding = LoadableDemoFragmentBinding.inflate(inflater, container, false);
    binding.setGetFriends(getFriendsLoc);

    //-- Friends list
    // won't actually be visible until the list of friends is loaded
    adapter = new ViewModelAdapter(getContext());
    binding.recyclerView.setAdapter(adapter);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    return binding.getRoot();
  }


  //== Inner classes ==========================================================

  private class ViewHolder extends RecyclerView.ViewHolder {

    private final LoadableDemoFriendListItemBinding binding;

    ViewHolder(LoadableDemoFriendListItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    private void bind(FriendItemVm friendItemVm) {
      this.binding.setFriend(friendItemVm);
    }
  } // class ViewHolder


  /** @see org.lathanh.play.rxandroid.demo.view_model.ViewModelDemoFragment.ViewModel */
  public class FriendItemVm extends BaseObservable {
    //-- For Actions (service calls)
    private long userId;

    //-- For View
    private LoadingState loadingState;
    private String userName;
    private String lastUpdateString;

    private FriendItemVm(final AndroidUserService.UserLoc userLoc) {
      setUser(userLoc.getUser());

      userLoc.addOnPropertyChangedCallback(
          new OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
              if (i == BR.loadingState) {
                FriendItemVm.this.loadingState = userLoc.getLoadingState();
                FriendItemVm.this.notifyPropertyChanged(BR.loadingState);
              } else if (i == BR.user) {
                setUser(userLoc.getUser());
              }
            }
          }
      );
    }

    /**
     * When the user is loaded or a changed user object arrives, do the adapting
     * (prepare the User data model for binding to the view).
     */
    private void setUser(UserService.User user) {
      if (user == null) return;

      this.userId = user.getId();
      this.userName = user.getName();

      DateFormat dateTimeInstance =
          SimpleDateFormat.getDateTimeInstance(DateFormat.LONG,
                                               DateFormat.LONG);
      this.lastUpdateString = dateTimeInstance.format(user.getLastUpdate());

      this.notifyPropertyChanged(BR.userName);
      this.notifyPropertyChanged(BR.lastUpdateString);
    }

    @Bindable
    public LoadingState getLoadingState() {
      return loadingState;
    }

    @Bindable
    public String getUserName() {
      return userName;
    }

    @Bindable
    public String getLastUpdateString() {
      return lastUpdateString;
    }

    public void onUpdateButtonClick() {
      androidUserService.updateUser(userId)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe();
    }
  } // class FriendItemVm

  /** Simply connects each ViewModel to the ViewHolder. */
  public class ViewModelAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final LayoutInflater inflater;

    ViewModelAdapter(Context context) {
      inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new ViewHolder(LoadableDemoFriendListItemBinding.inflate(inflater,
                                                                      parent,
                                                                      false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.bind(friendItemVms.get(position));
    }

    @Override
    public int getItemCount() {
      return friendItemVms.size();
    }
  } // class ViewModelAdapter

}
