package org.lathanh.play.rxandroid.demo.update.user_service;

import java.util.Date;

/**
 * This class represents a remote service API that takes time to perform its
 * operations.
 *
 * The most basic service, it simply fetches and updates users;
 * each operation takes "a while" (a couple hundred milliseconds or more) as
 * though it is performing a remote operation.
 * It is designed to be client/platform/use-case agnostic; that is, it is not
 * tailored for, say, use on Android (where such work must be done off of the
 * UI thread) nor for a server-based app.
 * Therefore, it doesn't do anything like threading (which consumers must handle
 * if necessary); the request is performed synchronously.
 *
 * The Objects it "retrieves" (it really just creates POJOs) are "Users",
 * and those Users can be "modified" (just changes the 'lastUpdated' date) by
 * ID.
 *
 * @author Robert LaThanh
 * @since 2017-02-28
 */
public class UserService {

  //== Private constants ======================================================

  private static final long LOAD_DELAY_MS = 250;
  private static final long UPDATE_DELAY_MS = 3000;


  //== Public inner classes ===================================================

  /**
   * Data Model / Response Object.
   *
   * That is, a request for a user simply returns this User object.
   */
  public static class User {
    private final long id;
    private final String name;
    private final Date lastUpdate;

    public User(long id) {
      this.id = id;
      this.name = Long.toString(id);
      this.lastUpdate = new Date();
    }

    public long getId() {
      return id;
    }

    /** Just the {@link #getId()}} as a String. */
    public String getName() {
      return name;
    }

    public Date getLastUpdate() {
      return lastUpdate;
    }
  }


  //== API methods ============================================================

  public User getUserById(long id) {
    try {
      Thread.sleep(LOAD_DELAY_MS);
    } catch (InterruptedException e) {
      // who dares interrupt my sleep?!
    }

    return new User(id);
  }

  /**
   * "Modifies" the user that has the given ID, and returns a User object with
   * the up-to-date values.
   *
   * This implementation simply a user with a new {@link User#getLastUpdate()
   * set to the current time.
   *
   * @return a new, instance of the User object
   */
  public User updateUser(long id) {
    try {
      Thread.sleep(UPDATE_DELAY_MS);
    } catch (InterruptedException e) {
      // who dares interrupt my sleep?!
    }

    return new User(id);
  }
}
