package com.github.kbinani.holosportsfestival2023;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

public class Result<T, Reason> {
  public final T result;
  public final Reason reason;

  public Result(T result, Reason reason) {
    this.result = result;
    this.reason = reason;
  }

  public void use(Consumer<T> cb) {
    if (result != null) {
      cb.accept(result);
    }
  }

  public <R> @Nullable R use(Function<T, R> cb) {
    if (result == null) {
      return null;
    } else {
      return cb.apply(result);
    }
  }
}
