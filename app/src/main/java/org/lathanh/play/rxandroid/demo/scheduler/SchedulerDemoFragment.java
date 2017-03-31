package org.lathanh.play.rxandroid.demo.scheduler;


import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;
import org.lathanh.play.rxandroid.databinding.SchedulerDemoFragmentBinding;

import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Demonstrates:
 *   * Using the Rx Scheduler (and some of its basic Operators)
 *   * A View Model with Android Data Binding to have the view automatically
 *     update when the View Model updates
 *   * Having the Observable update the View Model.
 *
 * Implementation Notes:
 *   * There is a single button, "Load Data", which schedules an
 *     Observable/Observer.
 *   * The Observable/Scheduler stuff happens in {@link #handleButtonOnClick()},
 *     which is bound to the "Load Data" button.
 *   * As the Observer's onStart and onComplete are called, the View Model's
 *     LoadingState is updated appropriately.
 *   * The View is automatically updated according to the LoadingState (using
 *     Android Data Binding).
 *
 * @author Robert LaThanh
 * @since 2017-02-21
 */
public class SchedulerDemoFragment extends Fragment {

  //== Operating fields ========================================================

  private CompositeDisposable disposables = new CompositeDisposable();
  private DisposableObserver<Long> observer = new DataObserver();
  private RandomNumberLoadableViewModel viewModel;


  //== 'Fragment' methods =====================================================

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // When this Fragment is created, we'll also have our data ("Data Model")
    // initialized as though it is not yet loaded (the new
    // DataLoadingStateContainer has no Data and is not yet loading).
    viewModel = new RandomNumberLoadableViewModel();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    SchedulerDemoFragmentBinding fragmentSchedulerDemoBinding =
        SchedulerDemoFragmentBinding.inflate(inflater, container, false);

    fragmentSchedulerDemoBinding.setLoadingDataVm(viewModel);
    return fragmentSchedulerDemoBinding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    disposables.clear();
  }


  //== Private methods ========================================================

  /**
   * This is where we'll see some Rx Operators in action, along with a
   * Scheduler.
   *
   * This "simulates" loading data.
   *
   * When the "Load Data" button is clicked, subscribe the {@link #observer} to
   * an Observable that will emit a single value after one second.
   */
  private void handleButtonOnClick() {
    Observable<Long> observable = Observable
        .just(true)
        .map(new Function<Boolean, Long>() {
          @Override
          public Long apply(Boolean aBoolean) throws Exception {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              // baby's crying again
            }

            return new Random().nextLong();
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());

    observable.subscribe(observer);
  }


  //== Inner classes ==========================================================

  /**
   * We need to "load data" asynchronously.
   * This container will hold that data once it's loaded, and in the meantime
   * also hold the loading state (e.g. whether the loading hasn't yet started,
   * is in progress, or has loaded).
   *
   * Since it's an Android Data Binding Observable, the view will automatically
   * update when its fields' values change.
   */
  public class RandomNumberLoadableViewModel extends BaseObservable {
    private LoadingState loadingState;
    private String dataString;

    void setLoadingState(LoadingState loadingState) {
      this.loadingState = loadingState;
      notifyPropertyChanged(BR.loadingState);
    }

    void setData(Long data) {
      this.dataString = Long.toString(data);
      notifyPropertyChanged(BR.dataString);
    }

    @Bindable
    public LoadingState getLoadingState() {
      return loadingState;
    }

    @Bindable
    public String getDataString() {
      return dataString;
    }

    /**
     */
    public void onClick() {
      handleButtonOnClick();
    }
  } // class RandomNumberLoadableViewModel

  /**
   * This Rx Observer updates the View Model (VM) so it reflects whether data is
   * loading/loaded, including putting the loaded value into the VM.
   */
  private class DataObserver extends DisposableObserver<Long> {

    @Override
    protected void onStart() {
      viewModel.setLoadingState(LoadingState.LOADING);
    }

    @Override
    public void onNext(Long value) {
      viewModel.setData(value);
    }

    @Override
    public void onError(Throwable e) {
      viewModel.setLoadingState(LoadingState.ERROR);
      viewModel.setData(-1L);
    }

    @Override
    public void onComplete() {
      viewModel.setLoadingState(LoadingState.DATA);
    }
  } // class DataObserver
}
