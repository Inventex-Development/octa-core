package dev.inventex.octa.concurrent.future;

import dev.inventex.octa.function.ThrowableFunction;
import dev.inventex.octa.function.ThrowableRunnable;
import dev.inventex.octa.function.ThrowableSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * A Future represents a callback, which can be completed or failed either synchronously or asynchronously.
 * A Future can be completed with the creation of A new I object, and can be failed by an exception
 * happening whilst executing a Future task.
 * <br><br>
 * This class also contains useful methods to attach callbacks for completion/failure events,
 * and to create new Future objects based on this instance.
 * <br>
 * Error recovery is also possible using the {@link #fallback(Object)} and {@link #fallback(Function)} methods.
 * <br>
 * The syntax encourages chaining, therefore less code is needed to handle certain tasks/events.
 * @param <T> the type of the returned value of the completed Future
 * @author AdvancedAntiSkid
 * @author MrGazdag
 * @since 1.0
 */
public class Future<T> {
    /**
     * The object used for thread locking for unsafe value modifications.
     */
    private final Object lock = new Object();

    /**
     * The list of the future completion handlers.
     */
    private final List<Consumer<T>> completionHandlers = new CopyOnWriteArrayList<>();

    /**
     * The list of the future failure handlers.
     */
    private final List<Consumer<Throwable>> errorHandlers = new CopyOnWriteArrayList<>();

    /**
     * The value of the completion result. Initially <code>null</code>, it is set to the completion object
     * after the completion is finished (which might still be <code>null</code>).
     */
    @Nullable
    private volatile T value;

    /**
     * The error that occurred whilst executing and caused a future failure.
     * Initially <code>null</code>, after a failure, it is guaranteed to be non-null.
     */
    @Nullable
    private volatile Throwable error;

    /**
     * Indicates whether the future completion had been done (either successfully or unsuccessfully).
     */
    private volatile boolean completed;

    /**
     * Indicates whether the future completion was failed.
     */
    private volatile boolean failed;

    /**
     * Creates a new, uncompleted Future.
     */
    public Future() {
    }

    /**
     * Block the current thread and wait for the Future completion to happen.
     * After the completion happened, the completion result T object is returned.
     * <br><br>
     * If the future completes with an exception, a {@link FutureExecutionException} is thrown.
     * The actual exception that made the future fail can be obtained using {@link FutureExecutionException#getCause()}.
     * <br><br>
     * Note that if the future completes successfully with <code>null</code>, the method will also return <code>null</code>.
     * <br><br>
     * @return the completion value or a default value
     *
     * @throws FutureExecutionException the completion failed and there was no default value to return
     */
    public T get() throws FutureExecutionException {
        try {
            // wait for the future completion without specifying a timeout
            return await(0, false, null);
        } catch (FutureTimeoutException e) {
            // this should not happen
            throw new IllegalStateException("Timeout should have been avoided", e);
        }
    }

    /**
     * Block the current thread and wait for the Future completion to happen.
     * After the completion happened, the completion result T object is returned.
     * <br><br>
     * If the future completes with an exception, a {@link FutureExecutionException} is thrown.
     * The actual exception that made the future fail can be obtained using {@link FutureExecutionException#getCause()}.
     * <br><br>
     * If the request has a timeout and exceeds the given time interval, a {@link FutureTimeoutException} is thrown.
     * If the timeout is 0, the method will block indefinitely.
     * <br><br>
     * Note that if the future completes successfully with <code>null</code>, the method will also return <code>null</code>.
     * <br><br>
     * @param timeout the maximum time interval to wait for the value, if this is exceeded, then a {@link FutureTimeoutException} is thrown.
     * @return the completion value or a default value
     *
     * @throws FutureTimeoutException the timeout interval has exceeded
     * @throws FutureExecutionException the completion failed and there was no default value to return
     */
    public T get(long timeout) throws FutureTimeoutException, FutureExecutionException {
        // wait for the future completion with a specified timeout
        return await(timeout, false, null);
    }

    /**
     * Block the current thread and wait for the Future completion to happen.
     * After the completion happened, the completion result T object is returned.
     * <br><br>
     * If the future completes with an exception, the <code>defaultValue</code> is returned.
     * <br><br>
     * Note that if the future completes successfully with <code>null</code>, the method will also return <code>null</code>.
     * <br><br>
     * @param defaultValue the default value which is returned on a completion failure
     * @return the completion value or a default value
     */
    public T getOrDefault(@Nullable T defaultValue) {
        try {
            // wait for the future completion with a specified default value
            return await(0, true, defaultValue);
        } catch (FutureExecutionException | FutureTimeoutException e) {
            // this should not happen
            throw new IllegalStateException("Timeout should have been avoided", e);
        }
    }

    /**
     * Block the current thread and wait for the Future completion to happen.
     * After the completion happened, the completion result T object is returned.
     * <br><br>
     * If the future completes with an exception, the <code>defaultValue</code> is returned.
     * <br><br>
     * If the request has a timeout and exceeds the given time interval, a {@link FutureTimeoutException} is thrown.
     * If the timeout is 0, the method will block indefinitely.
     * <br><br>
     * Note that if the future completes successfully with <code>null</code>, the method will also return <code>null</code>.
     * <br><br>
     * @param timeout the maximum time interval to wait for the value, if this is exceeded, then a {@link FutureTimeoutException} is thrown.
     * @param defaultValue the default value which is returned on a completion failure
     * @return the completion value or a default value
     *
     * @throws FutureTimeoutException the timeout interval has exceeded
     */
    public T getOrDefault(long timeout, @Nullable T defaultValue) throws FutureTimeoutException {
        try {
            // wait for the future completion with a specified timeout and default value
            return await(timeout, true, defaultValue);
        } catch (FutureExecutionException e) {
            // this should not happen
            throw new IllegalStateException("Execution exception should have been avoided", e);
        }
    }

    /**
     * Block the current thread and wait for the Future completion to happen.
     * After the completion happened, the completion result T object is returned.
     * <br><br>
     * If the future completes with an exception, a {@link FutureExecutionException} is thrown,
     * or the <code>defaultValue</code> is returned if present.
     * The actual exception that made the future fail can be obtained using {@link FutureExecutionException#getCause()}.
     * <br><br>
     * If the request has a timeout and exceeds the given time interval, a {@link FutureTimeoutException} is thrown.
     * If the timeout is 0, the method will block indefinitely.
     * <br><br>
     * Note that if the future completes successfully with <code>null</code>, the method will also return <code>null</code>.
     * <br><br>
     * @param timeout the maximum time interval to wait for the value, if this is exceeded, then a {@link FutureTimeoutException} is thrown.
     * @param hasDefault indicates whether a default value should be returned on a completion failure
     * @param defaultValue the default value which is returned on a completion failure
     * @return the completion value or a default value
     *
     * @throws FutureTimeoutException the timeout interval has exceeded
     * @throws FutureExecutionException the completion failed and a default value was not specified
     *
     * @see #get()
     * @see #get(long)
     * @see #getOrDefault(Object)
     * @see #getOrDefault(long, Object)
     */
    private synchronized T await(long timeout, boolean hasDefault, @Nullable T defaultValue) throws FutureTimeoutException, FutureExecutionException {
        // check if the future is already completed
        if (completed) {
            // check if the completion was successful
            if (!failed)
                return value;
            // completion was unsuccessful
            // return the default value if it is present
            if (hasDefault)
                return defaultValue;
            // no default value set, throw the completion error
            throw new FutureExecutionException(error);
        }
        // the future is not yet completed
        // ensure the lock is not used externally
        synchronized (lock) {
            try {
                // freeze the current thread until the future completion occurs
                // wait for the completion notification
                lock.wait(timeout);
            } catch (InterruptedException ignored) {
                // ignore if the completion thread was interrupted
            }
        }
        // check if the timeout has been exceeded, but the future hasn't been completed yet
        if (!completed)
            throw new FutureTimeoutException(timeout);
        // the future has been completed
        // check if the completion was successful
        if (!failed)
            return value;
        // the completion was unsuccessful
        // return the default value if it is present
        if (hasDefault)
            return defaultValue;
        // no default value set, throw the completion error
        throw new FutureExecutionException(error);
    }

    /**
     * Get instantly the completion value or the default value if the Future hasn't been completed yet.
     * @param defaultValue default value to return if the Future isn't completed
     * @return the completion value or the default value
     */
    public T getNow(@Nullable T defaultValue) {
        return completed ? value : defaultValue;
    }

    /**
     * Complete the Future successfully with the value given.
     * Call all the callbacks waiting on the completion of this Future.
     * <br><br>
     * If this Future was already completed (either successful or unsuccessful), this method does nothing.
     *
     * @param value the completion value
     * @return <code>true</code> if the Future was completed with the value,
     * <code>false</code> otherwise
     */
    public boolean complete(@Nullable T value) {
        // check if the future is already completed
        if (completed)
            return false;
        // set the completion value and unlock the waiting thread
        synchronized (lock) {
            this.value = value;
            completed = true;
            lock.notify();
        }
        // call the completion handlers
        handleCompleted(value);
        return true;
    }

    /**
     * Try to call the completion handlers.
     * @param value the completion value
     */
    private void handleCompleted(@Nullable T value) {
        // call the completion handlers
        for (Consumer<T> handler : completionHandlers) {
            try {
                // try call the completion handler
                handler.accept(value);
            } catch (Throwable e) {
                // fail the completion
                fail(e);
                // TODO should the loop break here?
            }
        }
    }

    /**
     * Fail the Future completion with the given error.
     * Call all the callbacks waiting on the failure of this Future.
     * <br><br>
     * If this Future was already completed (either successful or unsuccessful), this method does nothing.
     *
     * @param error the error occurred whilst completing
     * @return <code>true</code> if the Future was completed with an error, <code>false</code> otherwise
     */
    public boolean fail(@NotNull Throwable error) {
        // check if the future is already completed
        if (completed)
            return false;
        // set the error and unlock the waiting thread
        synchronized (lock) {
            this.error = error;
            completed = true;
            failed = true;
            lock.notify();
        }
        // call the failure handlers
        handleFailed(error);
        return true;
    }

    /**
     * Try to call the completion handlers.
     * @param error the error occurred whilst completing
     */
    private void handleFailed(@NotNull Throwable error) {
        // call the completion handlers
        for (Consumer<Throwable> handler : errorHandlers) {
            try {
                // try call the failure handler
                handler.accept(error);
            } catch (Throwable ignored) {
                // TODO should it handle error handler exceptions?
            }
        }
    }

    /**
     * Register a completion handler to be called when the Future completes without an error.
     * <br><br>
     * If the Future completes with an exception, the specified <code>action</code> will not be called.
     * If you wish to handle exceptions as well,
     * use {@link #result(BiConsumer)} or {@link #except(Consumer)} methods.
     * <br><br>
     * If the Future is already completed successfully, the action will be called immediately with
     * the completion value. If the Future failed with an exception, the action will not be called.
     *
     * @param action the successful completion callback
     * @return this Future
     */
    @NotNull
    public Future<T> then(@NotNull Consumer<T> action) {
        synchronized (lock) {
            // register the action if the Future hasn't been completed yet
            if (!completed)
                completionHandlers.add(action);
                // the Future is already completed
                // call the callback if the completion was successful
            else if (!failed)
                action.accept(value);
            return this;
        }
    }

    /**
     * Register an asynchronous completion handler to be called when the Future completes without an error.
     * <br><br>
     * If the Future completes with an exception, the specified <code>action</code> will not be called.
     * If you wish to handle exceptions as well,
     * use {@link #result(BiConsumer)} or {@link #except(Consumer)} methods.
     * <br><br>
     * If the Future is already completed successfully, the action will be called immediately with
     * the completion value. If the Future failed with an exception, the action will not be called.
     *
     * @param action the successful completion callback
     * @return this Future
     */
    @NotNull
    public Future<T> thenAsync(@NotNull Consumer<T> action) {
        synchronized (lock) {
            // register the action if the Future hasn't been completed yet
            if (!completed)
                completionHandlers.add(value -> executeLockedAsync(() -> action.accept(value)));
            // the Future is already completed
            // call the callback if the completion was successful
            else if (!failed)
                executeLockedAsync(() -> action.accept(value));
            return this;
        }
    }

    /**
     * Create a new Future that will transform the value to a new Future using the given transformer.
     * <br><br>
     * After this Future will successfully complete, the result will be passed to the specified transformer.
     * The output of the transformer will be the input for the new Future.
     * <br><br>
     * If this Future completes with an exception, the new Future
     * will be completed with the same exception.
     * <br><br>
     * If the current Future is already completed successfully, the transformer will be called
     * immediately, and a completed Future will be returned.
     *
     * @param transformer the function that transforms the value from T to U
     * @param <U> the new Future type
     * @return a new Future of type U
     */
    @NotNull
    public <U> Future<U> transform(@NotNull Function<T, U> transformer) {
        synchronized (lock) {
            // check if the Future is already completed
            if (completed) {
                // check if the completion was unsuccessful
                if (failed)
                    return failed(error);
                // try to transform the future value
                try {
                    return completed(transformer.apply(value));
                } catch (Exception e) {
                    // unable to transform the Future, return a failed Future
                    return failed(e);
                }
            }
            // the future hasn't been completed yet, create a new Future
            // that will try to transform the value once it is completed
            Future<U> future = new Future<>();
            // register the Future completion transformer
            completionHandlers.add(value -> {
                // try to transform the Future value
                try {
                    future.complete(transformer.apply(value));
                } catch (Exception e) {
                    // unable to transform the value, fail the Future
                    future.fail(e);
                }
            });
            // register the error handler
            errorHandlers.add(future::fail);
            return future;
        }
    }

    /**
     * Create a new Future that will transform the value to a new Future using the given transformer.
     * <br><br>
     * After this Future will successfully complete, the result will be passed to the specified transformer.
     * The output of the transformer will be the input for the new Future.
     * <br><br>
     * If this Future completes with an exception, the new Future
     * will be completed with the same exception.
     * <br><br>
     * If the current Future is already completed successfully, the transformer will be called
     * immediately, and a completed Future will be returned.
     *
     * @param transformer the function that transforms the value from T to U
     * @param <U> the new Future type
     * @return a new Future of type U
     */
    @NotNull
    public <U> Future<U> tryTransform(@NotNull ThrowableFunction<T, U, Throwable> transformer) {
        synchronized (lock) {
            // check if the Future is already completed
            if (completed) {
                // check if the completion was unsuccessful
                if (failed)
                    return failed(error);
                // try to transform the future value
                try {
                    return completed(transformer.apply(value));
                } catch (Throwable e) {
                    // unable to transform the Future, return a failed Future
                    return failed(e);
                }
            }
            // the future hasn't been completed yet, create a new Future
            // that will try to transform the value once it is completed
            Future<U> future = new Future<>();
            // register the Future completion transformer
            completionHandlers.add(value -> {
                // try to transform the Future value
                try {
                    future.complete(transformer.apply(value));
                } catch (Throwable e) {
                    // unable to transform the value, fail the Future
                    future.fail(e);
                }
            });
            // register the error handler
            errorHandlers.add(future::fail);
            return future;
        }
    }

    /**
     * Create a new Future that does not care about the completion value, it only checks for successful or
     * failed completion.
     * <br><br>
     * After this Future will successfully complete, a null be passed to the new Future.
     * <br><br>
     * If this Future completes with an exception, the new Future
     * will be completed with the same exception.
     * <br><br>
     * If the current Future is already completed successfully, a completed Future will be returned with the value of null.
     *
     * @return a new Future of Void type
     */
    @NotNull
    public Future<Void> callback() {
        synchronized (lock) {
            // check if the Future is already completed
            if (completed) {
                // check if the completion was unsuccessful
                if (failed)
                    return failed(error);
                // try to transform the future value
                try {
                    return completed((Void) null);
                } catch (Exception e) {
                    // unable to transform the Future, return a failed Future
                    return failed(e);
                }
            }
            // the future hasn't been completed yet, create a new Future
            // that will try to transform the value once it is completed
            Future<Void> future = new Future<>();
            // register the Future completion transformer
            completionHandlers.add(value -> {
                // try to transform the Future value
                try {
                    future.complete(null);
                } catch (Exception e) {
                    // unable to transform the value, fail the Future
                    future.fail(e);
                }
            });
            // register the error handler
            errorHandlers.add(future::fail);
            return future;
        }
    }

    /**
     * Register a failure handler to be called when the Future completes with an error.
     * <br><br>
     * If the Future completes successfully, the specified <code>action</code> will not be called.
     * If you wish to handle successful completions as well,
     * use {@link #result(BiConsumer)} or {@link #then(Consumer)} methods.
     * <br><br>
     * If the Future is already completed unsuccessfully, the action will be called immediately with
     * the completion error. If the Future has completed with a result, the action will not be called.
     *
     * @param action the failed completion handler
     * @return this Future
     */
    @NotNull
    public Future<T> except(@NotNull Consumer<Throwable> action) {
        synchronized (lock) {
            // register the action if the Future hasn't been completed yet
            if (!completed)
                errorHandlers.add(action);
                // the Future is already completed
                // call the callback if the completion was unsuccessful
            else if (failed)
                action.accept(error);
            return this;
        }
    }

    /**
     * Register a failure handler to be called when the Future completes with an error.
     * <br><br>
     * If the Future completes successfully, the specified <code>action</code> will not be called.
     * If you wish to handle successful completions as well,
     * use {@link #result(BiConsumer)} or {@link #then(Consumer)} methods.
     * <br><br>
     * If the Future is already completed unsuccessfully, the action will be called immediately with
     * the completion error. If the Future has completed with a result, the action will not be called.
     *
     * @param action the failed completion handler
     * @return this Future
     */
    @NotNull
    public Future<T> exceptAsync(@NotNull Consumer<Throwable> action) {
        synchronized (lock) {
            // register the action if the Future hasn't been completed yet
            if (!completed)
                errorHandlers.add(error -> executeLockedAsync(() -> action.accept(error)));
                // the Future is already completed
                // call the callback if the completion was unsuccessful
            else if (failed)
                executeLockedAsync(() -> action.accept(error));
            return this;
        }
    }

    /**
     * Create a new Future that will transform the exception from the old Future to a value.
     * <br><br>
     * If this Future completes successfully, the new Future will be completed
     * with the same exact value.
     * <br><br>
     * If this Future fails with an exception, the transformer will be called to
     * try to transform the exception to a fallback value. Finally, the value will be the
     * completion value of the new Future.
     * <br><br>
     * If the transformer's result is a constant, consider using {@link #fallback(Object)} instead,
     * as it does not require allocating a Function.
     *
     * @param transformer the function that transforms the error to T
     * @return a new Future
     */
    @NotNull
    public Future<T> fallback(@NotNull Function<Throwable, T> transformer) {
        synchronized (lock) {
            // check if the Future is already completed
            if (completed) {
                // check if the completion was successful
                if (!failed)
                    return completed(value);
                // try to transform the error to a value
                try {
                    return completed(transformer.apply(error));
                } catch (Exception e) {
                    // unable to transform the Future, return a failed Future
                    return failed(e);
                }
            }
            // the future hasn't been completed yet, create a new Future
            // that will try to transform the error once it is failed
            Future<T> future = new Future<>();
            // register the completion handler
            completionHandlers.add(future::complete);
            // register the error transformer
            errorHandlers.add(error -> {
                // try to transform the Future error
                try {
                    future.complete(transformer.apply(error));
                } catch (Exception e) {
                    // unable to transform the error, fail the Future
                    future.fail(e);
                }
            });
            return future;
        }
    }

    /**
     * Create a new Future that will complete with the fallback value if this Future fails.
     * <br><br>
     * If this Future completes successfully, the new Future will be completed
     * with the same exact value.
     * <br><br>
     * If this Future fails with an exception, the fallback value will be used to complete the new Future.
     * This can be used for error recovery, or to produce a fallback object,
     * that will be returned upon unsuccessful completion.
     * <br><br>
     * If the fallback object is not a constant, consider using {@link #fallback(Function)} instead,
     * to allow dynamic fallback object creation.
     *
     * @param fallbackValue the value used if an exception occurs
     * @return a new Future
     */
    @NotNull
    public Future<T> fallback(@Nullable T fallbackValue) {
        synchronized (lock) {
            // check if the Future is already completed
            if (completed) {
                // complete the Future with the fallback value if the
                // current Future's completion was failed
                if (failed)
                    return completed(fallbackValue);
                // the completion was successful, return the completion value
                return completed(value);
            }
            // the future hasn't been completed yet, create a new Future
            // that will use the fallback value if the current Future fails
            Future<T> future = new Future<>();
            // register the completion handler
            completionHandlers.add(future::complete);
            // register the error fallback handler
            errorHandlers.add(error -> future.complete(fallbackValue));
            return future;
        }
    }

    /**
     * Register a special handler, that listens to both successful and unsuccessful completions.
     * <br><br>
     * After a successful completion, the specified action will be called with the result value,
     * and the exception will be <code>null</code>.
     * <br>
     * If the Future is completed with an exception, the result will be null, and the exception will be given.
     * <br><br>
     * If you wish to determine if the completion was successful, consider checking if the exception is
     * <code>null</code>, as the completion might be successful with a <code>null</code> result.
     * <pre>
     * future.result((value, exception) -> {
     *     if (exception == null) {
     *         // successful completion, handle result
     *     } else {
     *         // unsuccessful completion, handle exception
     *     }
     * });
     * </pre>
     * If the Future is already completed, the action will be called immediately
     * with the completed value or exception.
     *
     * @param action the completion value and error handler
     * @return this Future
     */
    @NotNull
    public Future<T> result(@NotNull BiConsumer<T, Throwable> action) {
        synchronized (lock) {
            // call the action if the Future is already completed
            if (completed) {
                action.accept(value, error);
                return this;
            }
            // the Future hasn't been completed yet, register the callbacks
            completionHandlers.add(value -> action.accept(value, null));
            errorHandlers.add(error -> action.accept(null, error));
            return this;
        }
    }

    /**
     * Register a special handler, that listens to both successful and unsuccessful completions.
     * Use the transformer to create a new Future using the completion value and error.
     * <br><br>
     * After a successful completion, the specified action will be called with the result value,
     * and the exception will be <code>null</code>.
     * <br>
     * If the Future is completed with an exception, the result will be null, and the exception will be given.
     * <br><br>
     * If you wish to determine if the completion was successful, consider checking if the exception is
     * <code>null</code>, as the completion might be successful with a <code>null</code> result.
     * <pre>
     * future.result((value, exception) -> {
     *     if (exception == null) {
     *         // successful completion, handle result
     *     } else {
     *         // unsuccessful completion, handle exception
     *     }
     *     return modifiedValue;
     * });
     * </pre>
     * If the Future is already completed, the action will be called immediately
     * with the completed value or exception.
     *
     * @param transformer the Future value transformer
     * @return a new Future of type U
     */
    @NotNull
    public <U> Future<U> result(@NotNull BiFunction<T, Throwable, U> transformer) {
        synchronized (lock) {
            // check if the Future is already completed
            if (completed) {
                // try to transform the value and create a new Future with it
                try {
                    return completed(transformer.apply(value, error));
                } catch (Exception e) {
                    // unable to transform the error, create a Failed future
                    return failed(e);
                }
            }
            // the Future hasn't been completed yet, create a new one
            Future<U> future = new Future<>();
            // register the completion transformer
            completionHandlers.add(value -> {
                // try to transform the value
                try {
                    future.complete(transformer.apply(value, null));
                } catch (Exception e) {
                    // unable to transform the error, fail the Future
                    future.fail(e);
                }
            });
            // register the failure transformer
            errorHandlers.add(error -> {
                // try to transform the error
                try {
                    future.complete(transformer.apply(null, error));
                } catch (Exception e) {
                    // unable to transform the error, fail the Future
                    future.fail(e);
                }
            });
            return future;
        }
    }

    /**
     * Create a new Future, that will be completed unsuccessfully using a {@link FutureTimeoutException}
     * if the specified time has elapsed without a response. If this Future completes before the
     * timeout has passed, the new Future will be completed with this Future's result value.
     * <br><br>
     * If this Future completes unsuccessfully, the new Future will be completed with the same exception.
     *
     * @param timeout the time to wait (in milliseconds) until a {@link FutureTimeoutException} is thrown.
     * @return a new Future
     */
    @NotNull
    public Future<T> timeout(long timeout) {
        synchronized (lock) {
            // create a new Future to send the timeout result to
            Future<T> future = new Future<>();
            // check if the future is already completed
            if (completed) {
                // check if the completion was successful
                if (!failed)
                    return completed(value);
                // future was failed, retrieve the error
                return failed(error);
            }
            // create a new thread to run the timeout countdown on
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            // register the completion handler
            completionHandlers.add(value -> {
                // complete the timeout future
                future.complete(value);
                // shutdown the timeout task
                executor.shutdownNow();
            });
            // register the error handler
            errorHandlers.add(error -> {
                // fail the timeout future
                future.fail(error);
                // shutdown the timeout task
                executor.shutdownNow();
            });
            // execute the completion using the timeout delay
            executor.schedule(() -> {
                // fail the future if it hasn't been completed yet, and the
                // timeout limit has exceeded
                future.fail(new FutureTimeoutException(timeout));
                // execution has been finished, shutdown the executor
                executor.shutdown();
            }, timeout, TimeUnit.MILLISECONDS);
            return future;
        }
    }

    /**
     * Create a new Future, that will be completed unsuccessfully using a {@link FutureTimeoutException}
     * if the specified time has elapsed without a response. If this Future completes before the
     * timeout has passed, the new Future will be completed with this Future's result value.
     * <br><br>
     * If this Future completes unsuccessfully, the new Future will be completed with the same exception.
     *
     * @param timeout the time to wait until a {@link FutureTimeoutException} is thrown.
     * @param unit the type of the timeout (milliseconds, seconds, etc)
     * @return a new Future
     */
    @NotNull
    public Future<T> timeout(long timeout, @NotNull TimeUnit unit) {
        return timeout(TimeUnit.MILLISECONDS.convert(timeout, unit));
    }

    /**
     * Create a new Future which acts the same way this Future does.
     * @return a new Future
     */
    @NotNull
    public Future<T> mock() {
        synchronized (lock) {
            // create a new Future
            Future<T> future = new Future<>();
            // check if the Future is already completed
            if (completed) {
                // check if the completion was failed
                if (failed)
                    future.fail(error);
                    // handle successful completion
                else
                    future.complete(value);
            }
            // the Future hasn't been completed yet
            else {
                // register the completion handler
                completionHandlers.add(future::complete);
                // register the error handler
                errorHandlers.add(future::fail);
            }
            return future;
        }
    }

    /**
     * Indicates whether the future completion had been done (either successfully or unsuccessfully).
     * In order to determine if the completion was successful, use {@link #isFailed()}.
     *
     * @return <code>true</code> if this Future has already completed, <code>false</code> otherwise
     * @see #isFailed()
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Indicates whether the future was completed unsuccessfully.
     * If the Future hasn't been completed yet, this method returns <code>false</code>.
     *
     * @return <code>true</code> if the completion was unsuccessful, <code>false</code> otherwise
     * @see #isCompleted()
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Perform a task whilst the value is locked.
     * @param task the task to perform
     */
    private void executeLockedAsync(@NotNull Runnable task) {
        // create a new executor to run the completion on
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // execute the task asynchronously
        executor.execute(() -> {
            // lock the Future operations and run the task
            synchronized (lock) {
                task.run();
            }
            // execution has been finished, shutdown the executor
            executor.shutdown();
        });
    }

    /**
     * Create a new Future, that is completed initially using the specified value.
     * @param value the completion result
     * @param <T> the type of the Future
     * @return a new, completed Future
     */
    @NotNull
    public static <T> Future<T> completed(@Nullable T value) {
        // create a new empty Future
        Future<T> future = new Future<>();
        // set the future state
        future.value = value;
        future.completed = true;
        return future;
    }

    /**
     * Create a new Future, that is completed without a specified value.
     * @param <T> the type of the Future
     * @return a new, completed Future
     */
    @NotNull
    public static <T> Future<T> completed() {
        // create a new empty Future
        Future<T> future = new Future<>();
        // set the future state
        future.completed = true;
        return future;
    }

    /**
     * Create a new Future, that is completed initially using the specified value.
     * @param value the completion result
     * @param <T> the type of the Future
     * @return a new, completed Future
     */
    @NotNull
    public static <T> Future<T> completed(@NotNull Supplier<T> value) {
        // create a new empty Future
        Future<T> future = new Future<>();
        // set the future state
        future.value = value.get();
        future.completed = true;
        return future;
    }

    /**
     * Create a new Future, that is failed initially using the specified error.
     * @param error the completion error
     * @param <T> the type of the Future
     * @return a new, failed Future
     */
    @NotNull
    public static <T> Future<T> failed(@NotNull Throwable error) {
        // create a new empty Future
        Future<T> future = new Future<>();
        // set the future state
        future.error = error;
        future.completed = true;
        future.failed = true;
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread using the specified value.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is not a constant, consider using {@link #completeAsync(Supplier, Executor)} instead,
     * as it does allow dynamic object creation.
     *
     * @param result the value that is used to complete the Future with
     * @param executor the executor used to complete the Future on
     * @param <T> the type of the future
     * @return a new Future
     */
    @NotNull
    public static <T> Future<T> completeAsync(@Nullable T result, @NotNull Executor executor) {
        // create an empty future
        Future<T> future = new Future<>();
        // complete the future on the executor thread
        executor.execute(() -> {
            try {
                future.complete(result);
            } catch (Exception e) {
                future.fail(e);
            }
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread using the specified value.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object, Executor)} instead,
     * as it does not require allocating a supplier.
     *
     * @param result the value that is used to complete the Future with
     * @param executor the executor used to complete the Future on
     * @param <T> the type of the future
     * @return a new Future
     */
    @NotNull
    public static <T> Future<T> completeAsync(@NotNull Supplier<T> result, @NotNull Executor executor) {
        // create an empty future
        Future<T> future = new Future<>();
        // complete the future on the executor thread
        executor.execute(() -> {
            try {
                future.complete(result.get());
            } catch (Exception e) {
                future.fail(e);
            }
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread using the specified value.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object, Executor)} instead,
     * as it does not require allocating a supplier.
     *
     * @param result the value that is used to complete the Future with
     * @param executor the executor used to complete the Future on
     * @param <T> the type of the future
     * @return a new Future
     */
    @NotNull
    public static <T> Future<T> tryCompleteAsync(@NotNull ThrowableSupplier<T, Throwable> result,
                                                 @NotNull Executor executor) {
        // create an empty future
        Future<T> future = new Future<>();
        // complete the future on the executor thread
        executor.execute(() -> {
            try {
                future.complete(result.get());
            } catch (Throwable e) {
                future.fail(e);
            }
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread using the specified value.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is not a constant, consider using {@link #completeAsync(Supplier)} instead,
     * as it does allow dynamic object creation.
     *
     * @param result the value that is used to complete the Future with
     * @param <T> the type of the future
     * @return a new Future
     */
    @NotNull
    public static <T> Future<T> completeAsync(@Nullable T result) {
        // create an empty future
        Future<T> future = new Future<>();
        // create a new executor to run the completion on
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // complete the future
            try {
                future.complete(result);
            } catch (Exception e) {
                future.fail(e);
            }
            // execution has been finished, shutdown the executor
            executor.shutdown();
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread using the specified value.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object)} instead,
     * as it does not require allocating a supplier.
     *
     * @param result the value that is used to complete the Future with
     * @param <T> the type of the future
     * @return a new Future
     */
    @NotNull
    public static <T> Future<T> completeAsync(@NotNull Supplier<T> result) {
        // create an empty future
        Future<T> future = new Future<>();
        // create a new executor to run the completion on
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // complete the future
            try {
                future.complete(result.get());
            } catch (Exception e) {
                future.fail(e);
            }
            // execution has been finished, shutdown the executor
            executor.shutdown();
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread using the specified value.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object)} instead,
     * as it does not require allocating a supplier.
     *
     * @param result the value that is used to complete the Future with
     * @param <T> the type of the future
     * @return a new Future
     */
    @NotNull
    public static <T> Future<T> tryCompleteAsync(@NotNull ThrowableSupplier<T, Throwable> result) {
        // create an empty future
        Future<T> future = new Future<>();
        // create a new executor to run the completion on
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // complete the future
            try {
                future.complete(result.get());
            } catch (Throwable e) {
                future.fail(e);
            }
            // execution has been finished, shutdown the executor
            executor.shutdown();
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread, after running the specified task.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object)} instead,
     * as it does not require allocating a supplier.
     *
     * @param task the task to run to complete the future
     * @return a new Future
     */
    @NotNull
    public static Future<Void> completeAsync(@NotNull Runnable task) {
        // create an empty future
        Future<Void> future = new Future<>();
        // create a new executor to run the completion on
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.fail(e);
            }
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread, after running the specified task.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object)} instead,
     * as it does not require allocating a supplier.
     *
     * @param task the task to run to complete the future
     * @return a new Future
     */
    @NotNull
    public static Future<Void> tryCompleteAsync(@NotNull ThrowableRunnable<Throwable> task) {
        // create an empty future
        Future<Void> future = new Future<>();
        // create a new executor to run the completion on
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable e) {
                future.fail(e);
            }
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread, after running the specified task.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object, Executor)} instead,
     * as it does not require allocating a supplier.
     *
     * @param task the task to run to complete the future
     * @param executor the executor used to complete the Future on
     * @return a new Future
     */
    @NotNull
    public static Future<Void> completeAsync(@NotNull Runnable task, @NotNull Executor executor) {
        // create an empty future
        Future<Void> future = new Future<>();
        executor.execute(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.fail(e);
            }
        });
        return future;
    }

    /**
     * Create a new Future, that will be completed automatically on a different thread, after running the specified task.
     * <br><br>
     * Note that if the new Future is completed faster, than the current one is able to append any callbacks on it,
     * then some callbacks might be executed on the current thread.
     * Therefore, make sure to register the callbacks to this Future first.
     * <br><br>
     * If the result object is a constant, consider using {@link #completeAsync(Object, Executor)} instead,
     * as it does not require allocating a supplier.
     *
     * @param task the task to run to complete the future
     * @param executor the executor used to complete the Future on
     * @return a new Future
     */
    @NotNull
    public static Future<Void> completeAsync(@NotNull ThrowableRunnable<Throwable> task, @NotNull Executor executor) {
        // create an empty future
        Future<Void> future = new Future<>();
        executor.execute(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable e) {
                future.fail(e);
            }
        });
        return future;
    }
}
