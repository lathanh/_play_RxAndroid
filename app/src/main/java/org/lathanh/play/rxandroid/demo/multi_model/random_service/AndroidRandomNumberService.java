package org.lathanh.play.rxandroid.demo.multi_model.random_service;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.v4.util.Pair;

import org.lathanh.play.loading.LoadingState;
import org.lathanh.play.rxandroid.BR;

import java.util.Random;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.internal.operators.observable.ObservableScan;

/**
 * Created by rlathanh on 2017-03-08.
 */

public class AndroidRandomNumberService {

  private static final long BASE_SLEEP_MS = 100;
  private static final int SLEEP_MORE_RANGE_MS = 900;

  public static class RandomNumberLoc extends BaseObservable {
    private long id;

    private LoadingState loadingState;
    private long randomNumber;

    @Bindable
    public LoadingState getLoadingState() {
      return loadingState;
    }

    public void setLoadingState(LoadingState loadingState) {
      this.loadingState = loadingState;
      notifyPropertyChanged(BR.loadingState);
    }

    @Bindable
    public long getRandomNumber() {
      return randomNumber;
    }


    private void setRandomNumber(long randomNumber) {
      this.randomNumber = randomNumber;
      notifyPropertyChanged(BR.randomNumber);
      setLoadingState(LoadingState.DATA);
    }
  }

  private final Random random = new Random();

  public Pair<Observable<RandomNumberLoc>, RandomNumberLoc>
  getRandomNumber() {
    final RandomNumberLoc randomNumberLoc = new RandomNumberLoc();

    Observable<RandomNumberLoc> observable =
        ObservableScan.fromCallable(
            new Callable<RandomNumberLoc>() {
              @Override
              public RandomNumberLoc call() throws Exception {
                long millis = BASE_SLEEP_MS + random.nextInt(SLEEP_MORE_RANGE_MS);
                try {
                  Thread.sleep(millis);
                } catch (InterruptedException e) {
                  // can't catch a break!
                }

                randomNumberLoc.setRandomNumber(millis);
                return randomNumberLoc;
              }
            }
        );
    return new Pair<>(observable, randomNumberLoc);
  }

}
