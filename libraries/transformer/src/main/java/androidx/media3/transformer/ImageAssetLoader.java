/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media3.transformer;

import static androidx.media3.common.util.Assertions.checkNotNull;
import static androidx.media3.common.util.Assertions.checkState;
import static androidx.media3.transformer.ExportException.ERROR_CODE_IO_UNSPECIFIED;
import static androidx.media3.transformer.ExportException.ERROR_CODE_UNSPECIFIED;
import static androidx.media3.transformer.SampleConsumer.INPUT_RESULT_END_OF_STREAM;
import static androidx.media3.transformer.SampleConsumer.INPUT_RESULT_SUCCESS;
import static androidx.media3.transformer.SampleConsumer.INPUT_RESULT_TRY_AGAIN_LATER;
import static androidx.media3.transformer.Transformer.PROGRESS_STATE_AVAILABLE;
import static androidx.media3.transformer.Transformer.PROGRESS_STATE_NOT_STARTED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.BitmapLoader;
import androidx.media3.common.util.ConstantRateTimestampIterator;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceBitmapLoader;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.transformer.SampleConsumer.InputResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An {@link AssetLoader} implementation that loads images into {@link Bitmap} instances.
 *
 * <p>Supports the image formats listed <a
 * href="https://developer.android.com/guide/topics/media/media-formats#image-formats">here</a>
 * except from GIFs, which could exhibit unexpected behavior.
 */
@UnstableApi
public final class ImageAssetLoader implements AssetLoader {

  /** An {@link AssetLoader.Factory} for {@link ImageAssetLoader} instances. */
  public static final class Factory implements AssetLoader.Factory {

    private final Context context;

    public Factory(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public AssetLoader createAssetLoader(
        EditedMediaItem editedMediaItem, Looper looper, Listener listener) {
      return new ImageAssetLoader(context, editedMediaItem, listener);
    }
  }

  public static final String MIME_TYPE_IMAGE_ALL = MimeTypes.BASE_TYPE_IMAGE + "/*";

  private static final int QUEUE_BITMAP_INTERVAL_MS = 10;

  private final EditedMediaItem editedMediaItem;
  private final DataSource.Factory dataSourceFactory;
  private final Listener listener;
  private final ScheduledExecutorService scheduledExecutorService;

  @Nullable private SampleConsumer sampleConsumer;
  private @Transformer.ProgressState int progressState;

  private volatile int progress;

  private ImageAssetLoader(Context context, EditedMediaItem editedMediaItem, Listener listener) {
    checkState(editedMediaItem.durationUs != C.TIME_UNSET);
    checkState(editedMediaItem.frameRate != C.RATE_UNSET_INT);
    this.editedMediaItem = editedMediaItem;
    dataSourceFactory = new DefaultDataSource.Factory(context);
    this.listener = listener;
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    progressState = PROGRESS_STATE_NOT_STARTED;
  }

  @Override
  // Ignore Future returned by scheduledExecutorService because failures are already handled in the
  // runnable.
  @SuppressWarnings("FutureReturnValueIgnored")
  public void start() {
    progressState = PROGRESS_STATE_AVAILABLE;
    listener.onDurationUs(editedMediaItem.durationUs);
    listener.onTrackCount(1);
    BitmapLoader bitmapLoader =
        new DataSourceBitmapLoader(
            MoreExecutors.listeningDecorator(scheduledExecutorService), dataSourceFactory);
    MediaItem.LocalConfiguration localConfiguration =
        checkNotNull(editedMediaItem.mediaItem.localConfiguration);
    @Nullable BitmapFactory.Options options = null;
    if (Util.SDK_INT >= 26) {
      options = new BitmapFactory.Options();
      options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    }
    ListenableFuture<Bitmap> future = bitmapLoader.loadBitmap(localConfiguration.uri, options);
    Futures.addCallback(
        future,
        new FutureCallback<Bitmap>() {
          @Override
          public void onSuccess(Bitmap bitmap) {
            progress = 50;
            try {
              Format format =
                  new Format.Builder()
                      .setHeight(bitmap.getHeight())
                      .setWidth(bitmap.getWidth())
                      .setSampleMimeType(MIME_TYPE_IMAGE_ALL)
                      .setColorInfo(ColorInfo.SRGB_BT709_FULL)
                      .build();
              listener.onTrackAdded(format, SUPPORTED_OUTPUT_TYPE_DECODED);
              scheduledExecutorService.submit(() -> queueBitmapInternal(bitmap, format));
            } catch (RuntimeException e) {
              listener.onError(ExportException.createForAssetLoader(e, ERROR_CODE_UNSPECIFIED));
            }
          }

          @Override
          public void onFailure(Throwable t) {
            listener.onError(ExportException.createForAssetLoader(t, ERROR_CODE_IO_UNSPECIFIED));
          }
        },
        scheduledExecutorService);
  }

  @Override
  public @Transformer.ProgressState int getProgress(ProgressHolder progressHolder) {
    if (progressState == PROGRESS_STATE_AVAILABLE) {
      progressHolder.progress = progress;
    }
    return progressState;
  }

  @Override
  public ImmutableMap<Integer, String> getDecoderNames() {
    return ImmutableMap.of();
  }

  @Override
  public void release() {
    progressState = PROGRESS_STATE_NOT_STARTED;
    scheduledExecutorService.shutdownNow();
  }

  // Ignore Future returned by scheduledExecutorService because failures are already handled in the
  // runnable.
  @SuppressWarnings("FutureReturnValueIgnored")
  private void queueBitmapInternal(Bitmap bitmap, Format format) {
    try {
      if (sampleConsumer == null) {
        sampleConsumer = listener.onOutputFormat(format);
        scheduledExecutorService.schedule(
            () -> queueBitmapInternal(bitmap, format), QUEUE_BITMAP_INTERVAL_MS, MILLISECONDS);
        return;
      }
      // TODO(b/262693274): consider using listener.onDurationUs() or the MediaItem change
      //    callback rather than setting duration here.
      @InputResult
      int result =
          sampleConsumer.queueInputBitmap(
              bitmap,
              new ConstantRateTimestampIterator(
                  editedMediaItem.durationUs, editedMediaItem.frameRate));

      switch (result) {
        case INPUT_RESULT_SUCCESS:
          progress = 100;
          sampleConsumer.signalEndOfVideoInput();
          break;
        case INPUT_RESULT_TRY_AGAIN_LATER:
          scheduledExecutorService.schedule(
              () -> queueBitmapInternal(bitmap, format), QUEUE_BITMAP_INTERVAL_MS, MILLISECONDS);
          break;
        case INPUT_RESULT_END_OF_STREAM:
          progress = 100;
          break;
        default:
          throw new IllegalStateException();
      }
    } catch (ExportException e) {
      listener.onError(e);
    } catch (RuntimeException e) {
      listener.onError(ExportException.createForAssetLoader(e, ERROR_CODE_UNSPECIFIED));
    }
  }
}
