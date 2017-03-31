package org.lathanh.play.rxandroid.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.lathanh.play.rxandroid.R;
import org.lathanh.play.rxandroid.demo.loadable.LoadableDemoFragment;
import org.lathanh.play.rxandroid.demo.multi_model.MultiModelDemoFragment;
import org.lathanh.play.rxandroid.demo.scheduler.SchedulerDemoFragment;
import org.lathanh.play.rxandroid.demo.update.DataUpdateDemoFragment;
import org.lathanh.play.rxandroid.demo.view_model.ViewModelDemoFragment;

/**
 * Displays a list of demo fragments.
 *
 * When a fragment is chosen, it replaces this one as the content of the
 * Activity.
 *
 * @author Robert LaThanh
 * @since 2017-02-24
 */
public class MainMenuFragment extends Fragment {

  /**
   * Each item in the list of this menu needs to be able to create the
   * Fragment to be shown.
   */
  private interface CreateFragment {
    Fragment createFragment();
  }

  /**
   * The list of demo fragments, along with the name of each one (which will be
   * displayed as list item text).
   */
  private enum MenuItem implements CreateFragment {
    SCHEDULER("1. Scheduler") {
      @Override
      public Fragment createFragment() {
        return new SchedulerDemoFragment();
      }
    },
    DATA_UPDATE("2. Data Update") {
      @Override
      public Fragment createFragment() {
        return new DataUpdateDemoFragment();
      }
    },
    VIEW_MODEL("3. ViewModel") {
      @Override
      public Fragment createFragment() {
        return new ViewModelDemoFragment();
      }
    },
    LOADABLE("4. Loadable") {
      @Override
      public Fragment createFragment() {
        return new LoadableDemoFragment();
      }
    },
    MULTI_MODEL("5. Multi-Model") {
      @Override
      public Fragment createFragment() {
        return new MultiModelDemoFragment();
      }
    }
    ;

    private final String menuItemText;

    MenuItem(String menuItemText) {
      this.menuItemText = menuItemText;
    }
  } // enum MenuItems


  //== 'Fragment' methods =====================================================

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.recycler_view_layout, container,
                                 false);
    RecyclerView recyclerView =
        (RecyclerView) root.findViewById(R.id.recycler_view);
    recyclerView.setAdapter(new MenuItemAdapter(getContext()));
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    return recyclerView;
  }

  //== Inner classes ==========================================================

  public class MenuItemAdapter
      extends RecyclerView.Adapter<SimpleListItemViewHolder> {

    private LayoutInflater inflater;

    MenuItemAdapter(Context context) {
      inflater = LayoutInflater.from(context);
    }

    @Override
    public SimpleListItemViewHolder onCreateViewHolder(
        ViewGroup parent, int viewType) {
      return
          new SimpleListItemViewHolder(
              inflater.inflate(android.R.layout.simple_list_item_1, parent,
                               false));
    }

    @Override
    public void onBindViewHolder(SimpleListItemViewHolder holder,
                                 int position) {
      final MenuItem menuItem = MenuItem.values()[position];
      holder.menuItemOnClickListener.menuItem = menuItem;
      holder.text1.setText(menuItem.menuItemText);
    }

    @Override
    public int getItemCount() {
      return MenuItem.values().length;
    }
  } // class MenuItemAdapter

  private class MenuItemOnClickListener implements View.OnClickListener {
    private MenuItem menuItem;

    @Override
    public void onClick(View view) {
      DemoActivity activity = (DemoActivity) getActivity();
      activity.switchToFragment(menuItem.createFragment());
    }
  }

  public class SimpleListItemViewHolder extends RecyclerView.ViewHolder {

    private TextView text1;
    private MenuItemOnClickListener menuItemOnClickListener;

    SimpleListItemViewHolder(View itemView) {
      super(itemView);
      menuItemOnClickListener = new MenuItemOnClickListener();

      this.text1 = (TextView) itemView.findViewById(android.R.id.text1);
      this.text1.setOnClickListener(menuItemOnClickListener);
    }
  }
}
