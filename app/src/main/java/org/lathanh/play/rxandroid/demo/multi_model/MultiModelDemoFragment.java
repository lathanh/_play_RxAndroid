package org.lathanh.play.rxandroid.demo.multi_model;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.lathanh.play.rxandroid.databinding.LoadableDemoFragmentBinding;
import org.lathanh.play.rxandroid.databinding.MultiModelDemoListItemBinding;
import org.lathanh.play.rxandroid.demo.loadable.friend_service.AndroidFriendService;
import org.lathanh.play.rxandroid.demo.loadable.user_service.AndroidUserService;
import org.lathanh.play.rxandroid.demo.loadable.friend_service.FriendService;
import org.lathanh.play.rxandroid.demo.multi_model.random_service.AndroidRandomNumberService;
import org.lathanh.play.rxandroid.demo.update.user_service.UserService;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Building upon
 * {@link org.lathanh.play.rxandroid.demo.loadable.LoadableDemoFragment}...
 *
 * A View Model often needs data from several Data Models before it's ready to
 * be displayed.
 * In this demo, the View Model of each item ("Friend") in the list is given
 * three Observable (Android Data Binding Observable) containers, one for each
 * Data Model that it depends on.
 * It listens for changes to the container — that is, when the data has arrived
 * — and when all three have arrived, the View Model "adapts" (sets the
 * view-ready values from the data models), and the item is displayed.
 *
 * Implementation Notes:
 *   * The ViewModel is a top-level class
 *   * Each friend requires three data models before they can be adapted into a
 *     View Model: a User, and two Random Number objects.
 *
 * @author Robert LaThanh
 * @since 2017-03-08
 */
public class MultiModelDemoFragment extends Fragment {

  //== Private constants ======================================================

  private static final int NUM_ITEMS_TO_GET = 10;


  //== Operating fields =======================================================

  //-- Dependencies
  private final AndroidFriendService androidFriendService = new AndroidFriendService();
  private final AndroidUserService androidUserService = new AndroidUserService();
  private final AndroidRandomNumberService androidRandomNumberService = new AndroidRandomNumberService();

  // Page View Model
  private AndroidFriendService.GetFriendsLoc getFriendsLoc;
  private LoadableDemoFragmentBinding binding;

  // List
  private final List<ItemViewModel> listViewModels = new ArrayList<>();
  private ItemViewModelAdapter adapter;


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
            MultiModelDemoFragment.this.getFriendsLoc = getFriendsLoc;
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
            FriendService.GetFriendsResponse getFriendsResponse =
                getFriendsLoc.getGetFriendsResponse();
            return Observable.fromIterable(getFriendsResponse.friendUserIds);
          }
        })
        /**
         * ... have friend user ID (iteration).
         * 1. Create the view model for that friend and add to the adapter
         * 2. Create Observables that request the data, but pass those to
         *    another thread to actually perform.
         */
        .observeOn(AndroidSchedulers.mainThread())
        .map(new Function<Long, io.reactivex.Observable<?>>() {
          @Override
          public io.reactivex.Observable<?> apply(Long friendUserId)
              throws Exception {
            // generate observable request for the friend and their other data
            Pair<Observable<UserService.User>, AndroidUserService.UserLoc> userPair =
                androidUserService.getUser(friendUserId);
            Pair<Observable<AndroidRandomNumberService.RandomNumberLoc>, AndroidRandomNumberService.RandomNumberLoc> randomNumberLocPair1 =
                androidRandomNumberService.getRandomNumber();
            Pair<Observable<AndroidRandomNumberService.RandomNumberLoc>, AndroidRandomNumberService.RandomNumberLoc> randomNumberLocPair2 =
                androidRandomNumberService.getRandomNumber();

            // Create the View Model for this friend, which will take the three
            // LOCs, and adapt them once all three have loaded.
            ItemViewModel itemViewModel =
                new ItemViewModel(androidUserService, userPair.second,
                                  randomNumberLocPair1.second,
                                  randomNumberLocPair2.second);
            listViewModels.add(itemViewModel);
            if (adapter != null) {
              adapter.notifyItemInserted(listViewModels.size() - 1);
            }

            // pass the three observables (for the three data models) onto the
            // next operator for them to be actually performed
            return Observable.concat(userPair.first,
                                     randomNumberLocPair1.first,
                                     randomNumberLocPair2.first);
          }
        })
        /**
         * Take the observables created in the last step and actually
         * subscribe to them (causes them to actually be fetched).
         * The data will automatically be placed in the LOC (that we were given)
         * and the view will automatically update (when it has all the data
         * models it needs).
         */
        .observeOn(Schedulers.io())
        .concatMap(new Function<Observable<?>, ObservableSource<?>>() {
          @Override
          public ObservableSource<?> apply(Observable<?> observable)
              throws Exception {
            return observable;
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
    adapter = new ItemViewModelAdapter(getContext());
    binding.recyclerView.setAdapter(adapter);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    return binding.getRoot();
  }


  //== Inner classes ==========================================================

  private class ViewHolder extends RecyclerView.ViewHolder {

    private final MultiModelDemoListItemBinding binding;

    ViewHolder(MultiModelDemoListItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ItemViewModel itemViewModel) {
      this.binding.setFriend(itemViewModel);
    }
  } // class ViewHolder

  /** Simply connects each ViewModel to the ViewHolder. */
  public class ItemViewModelAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final LayoutInflater inflater;

    ItemViewModelAdapter(Context context) {
      inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new ViewHolder(MultiModelDemoListItemBinding.inflate(inflater,
                                                                  parent,
                                                                  false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.bind(listViewModels.get(position));
    }

    @Override
    public int getItemCount() {
      return listViewModels.size();
    }
  } // class ItemViewModelAdapter

}
