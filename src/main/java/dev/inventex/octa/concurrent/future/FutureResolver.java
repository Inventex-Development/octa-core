package dev.inventex.octa.concurrent.future;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an interface that can be used to complete a {@link Future} from an external context.
 *
 * @param <T> the type of the future value
 */
public interface FutureResolver<T> {
    /**
     * Complete the Future successfully with the value given.
     * Call all the callbacks waiting on the completion of this Future.
     * <p>
     * If this Future was already completed (either successful or unsuccessful), this method does nothing.
     *
     * @param value the completion value
     * @return <code>true</code> if the Future was completed with the value,
     * <code>false</code> otherwise
     */
    @CanIgnoreReturnValue
    boolean complete(@Nullable T value);

    /**
     * Fail the Future completion with the given error.
     * Call all the callbacks waiting on the failure of this Future.
     * <p>
     * If this Future was already completed (either successful or unsuccessful), this method does nothing.
     *
     * @param error the error occurred whilst completing
     * @return <code>true</code> if the Future was completed with an error, <code>false</code> otherwise
     */
    @CanIgnoreReturnValue
    boolean fail(@NotNull Throwable error);
}
