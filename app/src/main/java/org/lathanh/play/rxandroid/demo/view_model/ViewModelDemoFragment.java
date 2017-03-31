package org.lathanh.play.rxandroid.demo.view_model;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;
import org.lathanh.play.rxandroid.R;
import org.lathanh.play.rxandroid.databinding.ViewModelDemoUserListItemBinding;
import org.lathanh.play.rxandroid.demo.update.user_service.AndroidUserService;
import org.lathanh.play.rxandroid.demo.update.user_service.UserService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Essentially identical to
 * {@link org.lathanh.play.rxandroid.demo.update.DataUpdateDemoFragment}, but
 * introduces a View Model between the Data Model and the View.
 * This allows adapting to occur independently of Binding, including ahead of
 * time and on a different thread/scheduler.
 *
 * Demonstrates:
 *   * Use of a View Model to "adapt"
 *     (https://github.com/lathanh/android-mvp-framework#adapting) the
 *     {@link org.lathanh.play.rxandroid.demo.update.user_service.UserService.User}
 *     Data Model to prepare it for view.
 *   * Adapting ahead of time from binding
 *   * Adapting on a different thread/scheduler from binding.
 *   * We also have a better home for each item's onUpdateButtonClicked
 *     listener
 *
 * Implementation Notes:
 *   * The User is available (in the UserObservable) at the time the
 *     UserObservable is emitted (despite the fact that the design implies that
 *     it could be {@code null} (until a later time)).
 *   * Therefore, adapting occurs as UserObservable is observed (on its way to
 *     the adapter).
 *
 * @author Robert LaThanh
 * @since 2017-03-07
 */
public class ViewModelDemoFragment extends Fragment {

  //== Private constants ======================================================

  private static final int NUM_ITEMS_TO_GET = 30;


  //== Operating fields =======================================================

  //-- Dependencies
  private final AndroidUserService androidUserService = new AndroidUserService();

  //-- Operating fields
  private final List<ViewModel> viewModels = new ArrayList<>();
  private ViewModelAdapter adapter;


  //== 'Fragment' methods =====================================================

  /** Begin the loading of the users. */
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Start asking for the users now.
    // As the users start to come in, it's (theoretically) possible that the
    // view/adapter aren't yet set up, but if it is, notify the adapter.
    List<Long> userIds = new ArrayList<>(NUM_ITEMS_TO_GET);
    for (long id = 1; id <= NUM_ITEMS_TO_GET; id++) { userIds.add(id); }
    androidUserService.getUsersById(userIds)
        .subscribeOn(Schedulers.io())
        /** Create the View Models on the computation thread. */
        .observeOn(Schedulers.computation())
        .map(new Function<AndroidUserService.UserObservable, ViewModel>() {
          @Override
          public ViewModel apply(
              AndroidUserService.UserObservable userObservable)
              throws Exception {
            return new ViewModel(userObservable);
          }
        })
        /** Add the view models to the adapter on the main thread. */
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Consumer<ViewModel>() {
              @Override
              public void accept(ViewModel viewModel)
                  throws Exception {
                viewModels.add(viewModel);
                if (adapter != null) {
                  adapter.notifyItemInserted(viewModels.size() - 1);
                }
              }
            });
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.recycler_view_layout, container, false);
    RecyclerView recyclerView =
        (RecyclerView) root.findViewById(R.id.recycler_view);
    adapter = new ViewModelAdapter(getContext());
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    return recyclerView;
  }


  //== Inner classes ==========================================================

  /**
   * Data Models usually shouldn't be given directly to the view; there are
   * operations to the data model to make them view-ready (e.g., formatting
   * dates) that are potentially/often costly that should not happen during
   * Binding (setting values of the View).
   * This View Model allows those costly operations ("adapting") to be done
   * separately from, ahead of time of, and on a different thread than binding.
   * The View Model holds onto those view-ready values over to and until
   * binding.
   */
  public class ViewModel extends BaseObservable {
    //-- For Actions (service calls)
    private final long userId;

    //-- For View
    private LoadingState loadingState;
    private String userName;
    private String lastUpdateString;

    private ViewModel(final AndroidUserService.UserObservable userObservable) {
      UserService.User user = userObservable.getUser();
      setUser(user);
      this.userId = user.getId();
      this.loadingState = LoadingState.DATA;

      // Subscribe to changes in the UserObservable (namely, for updates to the
      // User). Upon changes, we need to update the User in this View Model so
      // it can "adapt" again (prepare new values for the view, which Android
      // Data Binding will automatically pick up to update the view).
      userObservable.addOnPropertyChangedCallback(
          new OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
              if (i == BR.loadingState) {
                ViewModel.this.loadingState = userObservable.getLoadingState();
                ViewModel.this.notifyPropertyChanged(BR.loadingState);
              } else if (i == BR.user) {
                setUser(userObservable.getUser());
              }
            }
          }
      );
    }

    /**
     * When the (updated) user arrives, do the adapting (prepare the User data
     * model for binding to the view).
     */
    private void setUser(UserService.User user) {
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
  } // class ViewModel

  private class ViewHolder extends RecyclerView.ViewHolder {

    private final ViewModelDemoUserListItemBinding binding;

    private ViewHolder(ViewModelDemoUserListItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    private void bind(ViewModel viewModel) {
      this.binding.setUserViewModel(viewModel);
    }
  } // class ViewHolder

  /** Simply connects each ViewModel to the ViewHolder. */
  public class ViewModelAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final LayoutInflater inflater;

    ViewModelAdapter(Context context) {
      inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new ViewHolder(ViewModelDemoUserListItemBinding.inflate(inflater,
                                                                     parent,
                                                                     false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.bind(viewModels.get(position));
    }

    @Override
    public int getItemCount() {
      return viewModels.size();
    }
  } // class ViewModelAdapter

}
