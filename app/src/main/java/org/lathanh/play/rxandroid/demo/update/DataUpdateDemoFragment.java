package org.lathanh.play.rxandroid.demo.update;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.lathanh.play.rxandroid.R;
import org.lathanh.play.rxandroid.databinding.DataUpdateDemoUserListItemBinding;
import org.lathanh.play.rxandroid.demo.update.user_service.AndroidUserService;
import org.lathanh.play.rxandroid.demo.update.user_service.AndroidUserService.UserObservable;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Demonstrates:
 *   * When an item's button (in the list) is clicked on, it uses an Rx
 *     Scheduler/Observable to perform the update.
 *   * When a Data Model is updated (as a result of the button being clicked),
 *     the view is automatically updated (using Android Data Binding
 *     Observables)
 *
 * Implementation notes:
 *   * The ({@link org.lathanh.play.rxandroid.demo.update.user_service.UserService.User})
 *     Data Model is immutable.
 *     An "update" to the user is actually a new instance of the User object
 *     with updated values (but same identifier).
 *   * In order for the "service" layer to be able to automatically update the
 *     view when the Data Model is updated; that is, when there's a new User
 *     object, I have the service layer return a {@link UserObservable}
 *     container, and then the User object inside can be swapped with a new one
 *     when it becomes available.
 *     The view automatically updates by subscribing to changes within the
 *     container (that is, of the User object referenced).
 *
 * @author Robert LaThanh
 * @since 2017-02-27
 */
public class DataUpdateDemoFragment extends Fragment {

  //== Private constants ======================================================

  private static final int NUM_ITEMS_TO_GET = 30;


  //== Operating fields =======================================================

  private final AndroidUserService androidUserService = new AndroidUserService();
  private final List<UserObservable> userObservables = new ArrayList<>();
  private UserObservableAdapter adapter;


  //== 'Fragment' methods =====================================================


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
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<UserObservable>() {
          @Override
          public void accept(UserObservable userObservable)
              throws Exception {
            userObservables.add(userObservable);
            if (adapter != null) {
              adapter.notifyItemInserted(userObservables.size() - 1);
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
    adapter = new UserObservableAdapter(getContext());
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    return recyclerView;
  }


  //== Inner classes ==========================================================

  /**
   * Each item in the adapter is just a {@link UserObservable}.
   * When the user is loaded (into the UserObservable), the view is
   * automatically updated (thanks Android Data Binding)!
   */
  public class UserObservableAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final LayoutInflater inflater;

    UserObservableAdapter(Context context) {
      inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return
          new ViewHolder(
              DataUpdateDemoUserListItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.setUserObservable(userObservables.get(position));
    }

    @Override
    public int getItemCount() {
      return userObservables.size();
    }
  } // class UserObservableAdapter

  /**
   * 1. Provides the UserObservable to the view (for automatic binding).
   * 2. Provides the onUpdateButtonClick for the Update button in the view
   */
  public class ViewHolder extends RecyclerView.ViewHolder {

    //== Operating fields
    private final DataUpdateDemoUserListItemBinding binding;

    //== Operating parameters
    private UserObservable userObservable;

    ViewHolder(DataUpdateDemoUserListItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void setUserObservable(UserObservable userObservable) {
      this.userObservable = userObservable;
      this.binding.setUserObservable(userObservable);
      this.binding.setViewHolder(this);
    }

    //== 'ViewHolder' methods
    public void onUpdateButtonClick() {
      androidUserService.updateUser(userObservable.getUser().getId())
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe();
    }
  } // class ViewHolder
}
