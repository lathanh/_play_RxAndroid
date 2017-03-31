
## Play — RxAndroid
> This is a "Play" project, meaning I'm just trying out new (to me) technology
> and techniques.
> In this case, trying out Reactive Extensions for Java/Android, and
> experimenting with using that together with things like Android Data Binding.
> 
> As a play project, I may take shortcuts (e.g., define strings in code),
> organize things strangely (put service classes in a subpackage of a Fragment
> class), or just plain do things the wrong way.
> So, while this could be a reference of what can be done, it should not be an
> example of how things _should_ be done!
>
> It assumes you've read [what Rx is about](http://reactivex.io/intro.html),
> understand the ideas of [Operators](http://reactivex.io/documentation/operators.html)
> and [Schedulers](http://reactivex.io/documentation/scheduler.html), and
> you're interested in seeing them in action.

## General Notes
* All of the demos use Android Data Binding.
    * This means that there are sometimes two Observables going on.
      Rx's Observables, which emit items to subscribers and observers for
      processing, and Android's Data Binding Observables that are given to
      View-Binding classes.

### Navigating the code
* org.lathanh.play.rxandroid.app
    * DemoActivity is a simple shell for showing the MainMenuFragment, and each of
      the demo fragments
        * MainMenuFragment is a chooser for the demo fragments

* Each demo fragment is in a package below 'org.lathanh.play.rxandroid.demo'
    * For example, the first demo, 
      [SchedulerDemoFragment](app/src/main/java/org/lathanh/play/rxandroid/demo/scheduler/SchedulerDemoFragment.java),
      is in a package named
      "[scheduler](app/src/main/java/org/lathanh/play/rxandroid/demo/scheduler/)"

* Each demo may rely on fake services; for example, a "UserService" that
  can fetch or update users.
     * These services are in a subpackage of the demo fragment

### "Architecture"
Part of the goal of these demos is to see what it's like to use Rx along with
existing infrastructure, such as a 3rd-party APIs that perform remote requests.
For example, the second demo, "Data Update," assumes that we're provided with a
"UserService," which is an API for fetching and modifying users.
So, I then wrap that service with an Observable version (ObservableUserService),
which wraps its operations in an Observable, like so:

1. **[UserService](app/src/main/java/org/lathanh/play/rxandroid/demo/update/user/UserService.java)**
   — This class represents a remote service API that takes time to perform its
   operations.
   The most basic service, it simply fetches and updates users; 
   each operation takes "a while" (a couple hundred milliseconds or more) as
   though it is performing a remote operation.
   It is designed to be client/platform/use-case agnostic; that is, it is not 
   tailored for, say, use on Android (where such work must be done off of the 
   UI thread) nor for a server-based app.
   Therefore, it doesn't do anything like threading (which consumers must handle
   if necessary); the request is performed synchronously.
2. **[ObservableUserService](app/src/main/java/org/lathanh/play/rxandroid/demo/update/user/ObservableUserService.java)**
   — This wraps the UserService with Rx Observables.
   So, each operation returns an Observable, and the caller sets up Observers to
   receive the result(s).
   Callers may also choose which thread to subscribeOn and observeOn.
   Like the UserService, it is still platform/client-agnostic.
3. **[AndroidUserService](app/src/main/java/org/lathanh/play/rxandroid/demo/update/user/AndroidUserService.java)**
   — This implementation, which wraps the ObservableUserService, is designed to 
   be used with Android, as the objects it emits are Data Binding Observables.
   That makes it easy to automatically display that data.

## Demos
For each demo, see the class documentation of the linked Fragment for details.

### Demo 1: Scheduler ([SchedulerDemoFragment](app/src/main/java/org/lathanh/play/rxandroid/demo/scheduler/SchedulerDemoFragment.java))
Essentially the "Hello, World" of Rx, it's mostly just puts Rx Operators,
Observables, and Scheduler to use.
It does also demonstrate using a 
[View Model](https://github.com/lathanh/android-mvp-framework#view-model) along 
with Android Data Binding to automatically update the view while waiting for the
data and when it is received.

### Demo 2: Data Update ([DataUpdateDemoFragment](app/src/main/java/org/lathanh/play/rxandroid/demo/update/DataUpdateDemoFragment.java))
When data in the underlying data model is updated, we need to update the view to
reflect the updated data.
This demo tests the full cycle of updating data; that is, an update triggered at
the UI layer (viz., widgets), updating data at the service layer, and the 
updated data traversing all the way back up to the view (through a series of
observables).

Also demonstrated are data appearing as they are each loaded (observed).

### Demo 3: View Model ([ViewModelDemoFragment](app/src/main/java/org/lathanh/play/rxandroid/demo/view_model/ViewModelDemoFragment.java))
Pretty much the same thing as the Data Update demo, but puts a View Model in 
between the data model and the view (didn't want it getting in the way of the
Data Update demo).

This allows 
"[adapting](https://github.com/lathanh/android-mvp-framework#adapting)" to
occur:

  * Independently from — ahead of time of — 
    "[binding](https://github.com/lathanh/android-mvp-framework#binding)".
  * On a different thread/scheduler from binding.

### Demo 4: ([LoadableDemoFragment](app/src/main/java/org/lathanh/play/rxandroid/demo/loadable/LoadableDemoFragment.java))
When retrieving a collection of data (for example, a list of friends) that
doesn't come in all at once, it can be great to show each item as it comes in
(versus waiting for all of the data in the list before showing the list).
The Rx model should be well-suited for this, and this demo demonstrates showing
each friend as the data for that user is received.

Because it wants to show a placeholder for each friend, one of the challenges is
that each row — while it's loading — needs a container into which the user can
be placed once it's loaded.
To accomplish this, the UserService returns both a container (that can be used
immediately to provide to the placeholder) right away, as well as an Observable
(that can be used — subscribed to — to fetch the user).

### Demo 5: ([MultiModelDemoFragment](app/src/main/java/org/lathanh/play/rxandroid/demo/multi_model/MultiModelDemoFragment.java))
A row in a list often needs multiple data models before it is ready for display.
In this demo, a row's View Model is only populated
("[adapted](https://github.com/lathanh/android-mvp-framework#adapting)") when
all three of the required data models have been provided to it.
