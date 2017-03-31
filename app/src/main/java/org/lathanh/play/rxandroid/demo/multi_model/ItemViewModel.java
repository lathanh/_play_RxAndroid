package org.lathanh.play.rxandroid.demo.multi_model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.support.annotation.NonNull;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;
import org.lathanh.play.rxandroid.demo.loadable.user_service.AndroidUserService;
import org.lathanh.play.rxandroid.demo.multi_model.random_service.AndroidRandomNumberService;
import org.lathanh.play.rxandroid.demo.update.user_service.UserService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * An ItemViewModel is a ViewModel for each item (a friend) in the list.
 * It's ViewModel fields contain view-ready fields once the data models have
 * become available and have been adapted.
 * Until then, they are null (and the View can adjust accordingly).
 *
 * This implementation also does its own adapting.
 * That is, it also contains the code to convert the data (from the Data Models)
 * into it's View Model (view-ready) fields.
 *
 * @author Robert LaThanh
 * @since 2017-03-08
 */
public class ItemViewModel extends BaseObservable {

  //== Private constants ======================================================

  private static final long ADAPT_DELAY_MS = 100;


  //== Instance fields ========================================================

  //-- Dependencies
  /** For performing an action upon onClick. */
  @NonNull private final AndroidUserService androidUserService;

  //-- Data Models
  @NonNull private final AndroidUserService.UserLoc userLoc;
  @NonNull private final AndroidRandomNumberService.RandomNumberLoc randomNumberLoc1;
  @NonNull private final AndroidRandomNumberService.RandomNumberLoc randomNumberLoc2;

  //-- For View
  private LoadingState loadingState;
  private String text1;
  private String text2;


  //== Constructor ============================================================

  /**
   * Initialized with a container for each expected Data Model.
   * It subscribes to each of them so it can be notified when the data arrives,
   * and calls {@link #onDataLoaded()} for each notification, which may result
   * in adapting.
   */
  public ItemViewModel(
      @NonNull AndroidUserService androidUserService,
      @NonNull AndroidUserService.UserLoc userLoc,
      @NonNull AndroidRandomNumberService.RandomNumberLoc randomNumberLoc1,
      @NonNull AndroidRandomNumberService.RandomNumberLoc randomNumberLoc2) {
    this.androidUserService = androidUserService;
    this.userLoc = userLoc;
    this.randomNumberLoc1 = randomNumberLoc1;
    this.randomNumberLoc2 = randomNumberLoc2;

    OnPropertyChangedCallback onPropertyChangedCallback = new OnPropertyChangedCallback() {
      @Override
      public void onPropertyChanged(Observable observable, int i) {
        // just easy/lazy. If the user inside is being updated, the userLoc
        // loading state changes, which which case we need to notify that the
        // userLoadingState value is changing. Without checking which LOC has
        // a property change, nor whether it's the relevant property, I'm just
        // going to always notify that the userLoadingState might have changed.
        // Like I said, lazy.
        notifyPropertyChanged(BR.userLoadingState);
        onDataLoaded();
      }
    };
    userLoc.addOnPropertyChangedCallback(onPropertyChangedCallback);
    randomNumberLoc1.addOnPropertyChangedCallback(onPropertyChangedCallback);
    randomNumberLoc2.addOnPropertyChangedCallback(onPropertyChangedCallback);
    onDataLoaded(); // it's possible all data were already available
  }


  //== For View (binding) =====================================================

  @Bindable
  public LoadingState getLoadingState() {
    return loadingState;
  }

  @Bindable
  public String getText1() {
    return text1;
  }

  @Bindable
  public String getText2() {
    return text2;
  }

  @Bindable
  public LoadingState getUserLoadingState() {
    return userLoc.getLoadingState();
  }

  public void onUpdateButtonClick() {
    //noinspection ConstantConditions // button would not have been available
    androidUserService.updateUser(userLoc.getUser().getId())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }


  //== Private methods ========================================================

  /**
   * This is called any time data has loaded (into one of the LOCs), including
   * when data has changed (inside the LOC).
   *
   * When all the data are in, this will "adapt" them; that is, set the view
   * model fields (which Android Data Binding will then update the view with).
   * These operations can
   *
   * We *could* do some adapting with partial data (when some data has come in
   * but other has not), showing the user what has arrived so far.
   * That's simple in that all you have to do is decide what adapting to do
   * when within this method (but you also have to decide whether that can
   * make sense for your UI).
   * For simplicity, for this demo, I'm not going to do any adapting until all
   * the data are in.
   */
  private void onDataLoaded() {
    if (userLoc.getLoadingState() != LoadingState.DATA
        || randomNumberLoc1.getLoadingState() != LoadingState.DATA
        || randomNumberLoc2.getLoadingState() != LoadingState.DATA) {
      return;
    }

    // Let's pretend that adapting is expensive and takes a while
    try {
      Thread.sleep(ADAPT_DELAY_MS);
    } catch (InterruptedException e) {
      // can't catch a break!
    }

    UserService.User user = userLoc.getUser();
    assert user != null; // based on LoadingState check
    DateFormat dateTimeInstance =
        SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

    loadingState = LoadingState.DATA;
    text1 = user.getId() + ": " + user.getName() + " (" + randomNumberLoc1.getRandomNumber() + ")";
    text2 = dateTimeInstance.format(user.getLastUpdate()) + " (" + randomNumberLoc2.getRandomNumber() + ")";
    notifyPropertyChanged(BR.loadingState);
    notifyPropertyChanged(BR.text1);
    notifyPropertyChanged(BR.text2);
  }
}
