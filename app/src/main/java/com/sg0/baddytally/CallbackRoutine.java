package com.sg0.baddytally;

public interface CallbackRoutine {
    void profileFetched();

    void alertResult(final String in, final Boolean ok, final Boolean ko);

    void completed(final String in, final Boolean ok);
}
