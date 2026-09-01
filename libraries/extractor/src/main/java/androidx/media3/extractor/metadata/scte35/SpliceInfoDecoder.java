/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.media3.extractor.metadata.scte35;

import androidx.annotation.Nullable;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.metadata.MetadataInputBuffer;
import androidx.media3.extractor.metadata.SimpleMetadataDecoder;
import java.nio.ByteBuffer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Decodes splice info sections and produces splice commands. */
@UnstableApi
public final class SpliceInfoDecoder extends SimpleMetadataDecoder {

  private static final String TAG = "SpliceInfoDecoder";

  /**
   * The number of bytes in a {@code splice_info_section} up to and including {@code
   * splice_command_type}, which this decoder reads unconditionally: table_id(8),
   * section_syntax_indicator(1), private_indicator(1), reserved(2), section_length(12),
   * protocol_version(8), encrypted_packet(1), encryption_algorithm(6), pts_adjustment(33),
   * cw_index(8), tier(12), splice_command_length(12), splice_command_type(8) = 112 bits.
   */
  private static final int HEADER_LENGTH = 14;

  private static final int TYPE_SPLICE_NULL = 0x00;
  private static final int TYPE_SPLICE_SCHEDULE = 0x04;
  private static final int TYPE_SPLICE_INSERT = 0x05;
  private static final int TYPE_TIME_SIGNAL = 0x06;
  private static final int TYPE_PRIVATE_COMMAND = 0xFF;

  private final ParsableByteArray sectionData;
  private final ParsableBitArray sectionHeader;

  private @MonotonicNonNull TimestampAdjuster timestampAdjuster;

  public SpliceInfoDecoder() {
    sectionData = new ParsableByteArray();
    sectionHeader = new ParsableBitArray();
  }

  @Override
  @SuppressWarnings("ByteBufferBackingArray") // Buffer validated by SimpleMetadataDecoder.decode
  protected Metadata decode(MetadataInputBuffer inputBuffer, ByteBuffer buffer) {
    // Internal timestamps adjustment.
    if (timestampAdjuster == null
        || inputBuffer.subsampleOffsetUs != timestampAdjuster.getTimestampOffsetUs()) {
      timestampAdjuster = new TimestampAdjuster(inputBuffer.timeUs);
      timestampAdjuster.adjustSampleTimestamp(inputBuffer.timeUs - inputBuffer.subsampleOffsetUs);
    }

    byte[] data = buffer.array();
    int size = buffer.limit();
    if (size < HEADER_LENGTH) {
      // A section shorter than the fixed header can't be parsed at all. This happens in practice
      // when a manifest signals an SCTE-35 event with a missing or truncated payload (a DASH
      // <Event> with no Signal/Binary child, or an HLS #EXT-X-DATERANGE with no SCTE35 attribute).
      // Drop the event rather than reading past the end of the buffer.
      Log.w(TAG, "Discarding malformed splice info section, length=" + size);
      return new Metadata();
    }
    sectionData.reset(data, size);
    sectionHeader.reset(data, size);
    // table_id(8), section_syntax_indicator(1), private_indicator(1), reserved(2),
    // section_length(12), protocol_version(8), encrypted_packet(1), encryption_algorithm(6).
    sectionHeader.skipBits(39);
    long ptsAdjustment = sectionHeader.readBits(1);
    ptsAdjustment = (ptsAdjustment << 32) | sectionHeader.readBits(32);
    // cw_index(8), tier(12).
    sectionHeader.skipBits(20);
    int spliceCommandLength = sectionHeader.readBits(12);
    int spliceCommandType = sectionHeader.readBits(8);
    @Nullable SpliceCommand command = null;
    // Go to the start of the command by skipping all fields up to command_type.
    sectionData.skipBytes(HEADER_LENGTH);
    // The command bodies are variable length and read from sectionData without bounds checks, so a
    // section that is truncated part-way through its command would throw. A malformed ad marker
    // must not become a fatal playback error, so drop the event instead.
    try {
      switch (spliceCommandType) {
        case TYPE_SPLICE_NULL:
          command = new SpliceNullCommand();
          break;
        case TYPE_SPLICE_SCHEDULE:
          command = SpliceScheduleCommand.parseFromSection(sectionData);
          break;
        case TYPE_SPLICE_INSERT:
          command =
              SpliceInsertCommand.parseFromSection(sectionData, ptsAdjustment, timestampAdjuster);
          break;
        case TYPE_TIME_SIGNAL:
          command =
              TimeSignalCommand.parseFromSection(sectionData, ptsAdjustment, timestampAdjuster);
          break;
        case TYPE_PRIVATE_COMMAND:
          command = PrivateCommand.parseFromSection(sectionData, spliceCommandLength, ptsAdjustment);
          break;
        default:
          // Do nothing.
          break;
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Discarding unparseable splice command, type=" + spliceCommandType, e);
      return new Metadata();
    }
    return command == null ? new Metadata() : new Metadata(command);
  }
}
