package org.lathanh.play.loading;

/**
 * The various possible states of loading data.
 *
 * {@code null} means that data loading has not started.
 *
 * @author Robert LaThanh
 * @since 2017-02-23
 */
public enum LoadingState {
  /** The data is in the process of being loaded. */
  LOADING,

  /**
   * Data is loaded/present, but there is also work in progress that will
   * change it (some or all of its values).
   */
  UPDATING,

  /** The data is loaded. */
  DATA,

  /**
   * Data is loaded/present, but it is (or probably is) out-of-date (possibly
   * known because the update of other data is known to affect this data).
   */
  STALE,

  /** Data was not able to be loaded due to an error.*/
  ERROR,
}
