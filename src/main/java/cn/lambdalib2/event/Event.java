package cn.lambdalib2.event;

/**
 * Base class of all events posted on a {@link EventBus}. Mirrors the semantics of Forge's Event: events whose class
 *  (or super class) is annotated with {@link Cancelable} can be canceled.
 */
public class Event {

    public enum Result { DENY, DEFAULT, ALLOW }

    private boolean canceled = false;
    private Result result = Result.DEFAULT;

    public boolean isCancelable() {
        Class<?> c = getClass();
        while (c != null && c != Object.class) {
            if (c.isAnnotationPresent(Cancelable.class)) return true;
            c = c.getSuperclass();
        }
        return false;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean cancel) {
        if (!isCancelable()) {
            throw new UnsupportedOperationException("Attempted to cancel an uncancelable event " + getClass());
        }
        canceled = cancel;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result value) {
        result = value;
    }
}
