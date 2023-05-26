package androidx.media3.extractor.metadata.scte35;

import android.os.Parcel;
import androidx.media3.common.util.ParsableByteArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Superclass for SCTE35 splice descriptors. */
public abstract class SpliceDescriptor {

  public enum DescriptorType {
    AVAIL_DESCRIPTOR(0x00),
    DTMF_DESCRIPTOR(0x01),
    SEGMENTATION_DESCRIPTOR(0x02),
    TIME_DESCRIPTOR(0x03),
    AUDIO_DESCRIPTOR(0x04);

    private final int tag;

    DescriptorType(int tag) {
      this.tag = tag;
    }

    public int getTag() {
      return tag;
    }

    public static DescriptorType from(int tag) {
      if (tag >= 0) {
        for (DescriptorType type : DescriptorType.values()) {
          if (type.getTag() == tag) {
            return type;
          }
        }
      }
      return null;
    }
  }

  public final DescriptorType descriptorType;
  public final int descriptorLength;
  public final String identifier; // CUEI

  public SpliceDescriptor(DescriptorType descriptorType, int descriptorLength, String identifier) {
    this.descriptorType = descriptorType;
    this.descriptorLength = descriptorLength;
    this.identifier = identifier;
  }

  public boolean isSegmentationDescriptor() {
    return descriptorType == DescriptorType.SEGMENTATION_DESCRIPTOR;
  }

  // Handle the descriptor_loop_length and splice_descriptor().
  static List<SpliceDescriptor> parseFromSection(ParsableByteArray sectionData) {
    if (sectionData.bytesLeft() <= 2) {
      return Collections.emptyList();
    }
    int descriptorLoopLength = sectionData.readUnsignedShort();
    if (descriptorLoopLength <= 0) {
      return Collections.emptyList();
    }

    final int sectionEndPosition = sectionData.getPosition() + descriptorLoopLength;
    List<SpliceDescriptor> descriptors = new ArrayList<>();
    while (descriptorLoopLength > 5) {
      DescriptorType descriptorType = DescriptorType.from(sectionData.readUnsignedByte());
      int descriptorLength = sectionData.readUnsignedByte();
      switch (descriptorType) {
        case AVAIL_DESCRIPTOR:
          descriptors.add(AvailDescriptor.parseFromSection(sectionData, descriptorLength));
          break;
        case DTMF_DESCRIPTOR:
          descriptors.add(DTMFDescriptor.parseFromSection(sectionData, descriptorLength));
          break;
        case SEGMENTATION_DESCRIPTOR:
          descriptors.add(
              SegmentationDescriptor.parseFromSection(sectionData, descriptorLength));
          break;
        case TIME_DESCRIPTOR:
          descriptors.add(TimeDescriptor.parseFromSection(sectionData, descriptorLength));
          break;
        case AUDIO_DESCRIPTOR:
          descriptors.add(AudioDescriptor.parseFromSection(sectionData, descriptorLength));
          break;
        default:
          sectionData.skipBytes(descriptorLength);
          // Do nothing.
          break;
      }
      descriptorLoopLength -= descriptorLength + 2;
    }
    skipBytes(sectionEndPosition, sectionData);
    return descriptors;
  }

  static void skipBytes(int sectionEndPosition, ParsableByteArray sectionData) {
    int bytesLeft = sectionEndPosition - sectionData.getPosition();
    if (bytesLeft > 0) {
      sectionData.skipBytes(bytesLeft);
    }
  }

  static List<SpliceDescriptor> createFromParcel(Parcel in) {
    int descriptorListSize = in.readInt();
    List<SpliceDescriptor> descriptorList = new ArrayList<>(descriptorListSize);
    for (int i = 0; i < descriptorListSize; i++) {
      DescriptorType descriptorType = DescriptorType.from(in.readInt());
      switch (descriptorType) {
        case AVAIL_DESCRIPTOR:
          descriptorList.add(AvailDescriptor.fromParcel(in));
          break;
        case DTMF_DESCRIPTOR:
          descriptorList.add(DTMFDescriptor.fromParcel(in));
          break;
        case SEGMENTATION_DESCRIPTOR:
          descriptorList.add(SegmentationDescriptor.fromParcel(in));
          break;
        case TIME_DESCRIPTOR:
          descriptorList.add(TimeDescriptor.fromParcel(in));
          break;
        case AUDIO_DESCRIPTOR:
          descriptorList.add(AudioDescriptor.fromParcel(in));
          break;
        default:
          // Do nothing.
          break;
      }
    }
    return Collections.unmodifiableList(descriptorList);
  }

  static void writeToParcel(List<SpliceDescriptor> descriptors, Parcel dest) {
    int descriptorListSize = descriptors.size();
    dest.writeInt(descriptorListSize);
    for (int i = 0; i < descriptorListSize; i++) {
      descriptors.get(i).writeToParcel(dest);
    }
  }

  public void writeToParcel(Parcel dest) {
    dest.writeInt(descriptorType.getTag());
    dest.writeInt(descriptorLength);
    dest.writeString(identifier);
  }

  @Override
  public String toString() {
    return "SCTE-35 splice descriptor: type=" + getClass().getSimpleName();
  }

  /** A {@link SpliceDescriptor} that defines an avail descriptor. */
  public static class AvailDescriptor extends SpliceDescriptor {

    private AvailDescriptor(int descriptorLength, String identifier) {
      super(DescriptorType.AVAIL_DESCRIPTOR, descriptorLength, identifier);
    }

    static AvailDescriptor parseFromSection(ParsableByteArray sectionData, int descriptorLength) {
      final int sectionEndPosition = sectionData.getPosition() + descriptorLength;
      String identifier = sectionData.readString(4);
      // provider_avail_id(32)
      skipBytes(sectionEndPosition, sectionData);
      return new AvailDescriptor(descriptorLength, identifier);
    }

    static AvailDescriptor fromParcel(Parcel in) {
      // We had read the first int of spliceDescriptorTag.
      return new AvailDescriptor(in.readInt(), in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest) {
      super.writeToParcel(dest);
      // More fields.
    }
  }

  /** A {@link SpliceDescriptor} that defines a DTMF descriptor. */
  public static class DTMFDescriptor extends SpliceDescriptor {

    private DTMFDescriptor(int descriptorLength, String identifier) {
      super(DescriptorType.DTMF_DESCRIPTOR, descriptorLength, identifier);
    }

    static DTMFDescriptor parseFromSection(ParsableByteArray sectionData, int descriptorLength) {
      final int sectionEndPosition = sectionData.getPosition() + descriptorLength;
      String identifier = sectionData.readString(4);
      // preroll(8), dtmf_count(3), reserved(5)
      // DTMF_char(8)
      skipBytes(sectionEndPosition, sectionData);
      return new DTMFDescriptor(descriptorLength, identifier);
    }

    static DTMFDescriptor fromParcel(Parcel in) {
      // We had read the first int of spliceDescriptorTag.
      return new DTMFDescriptor(in.readInt(), in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest) {
      super.writeToParcel(dest);
      // More fields.
    }
  }

  /** A {@link SpliceDescriptor} that defines a time descriptor. */
  public static class TimeDescriptor extends SpliceDescriptor {

    private TimeDescriptor(int descriptorLength, String identifier) {
      super(DescriptorType.TIME_DESCRIPTOR, descriptorLength, identifier);
    }

    static TimeDescriptor parseFromSection(ParsableByteArray sectionData, int descriptorLength) {
      final int sectionEndPosition = sectionData.getPosition() + descriptorLength;
      String identifier = sectionData.readString(4);
      // TAI_seconds(48), TAI_ns(32), UTC_offset(16)
      skipBytes(sectionEndPosition, sectionData);
      return new TimeDescriptor(descriptorLength, identifier);
    }

    static TimeDescriptor fromParcel(Parcel in) {
      // We had read the first int of spliceDescriptorTag.
      return new TimeDescriptor(in.readInt(), in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest) {
      super.writeToParcel(dest);
      // More fields.
    }
  }

  /** A {@link SpliceDescriptor} that defines a audio descriptor. */
  public static class AudioDescriptor extends SpliceDescriptor {

    private AudioDescriptor(int descriptorLength, String identifier) {
      super(DescriptorType.AUDIO_DESCRIPTOR, descriptorLength, identifier);
    }

    static AudioDescriptor parseFromSection(ParsableByteArray sectionData, int descriptorLength) {
      final int sectionEndPosition = sectionData.getPosition() + descriptorLength;
      String identifier = sectionData.readString(4);
      // audio_count(4), reserved(4)
      // component_tag(8), ISO_code(24), Bit_Stream_Mode(3), Num_Channels(4), Full_Srvc_Audio(1)
      skipBytes(sectionEndPosition, sectionData);
      return new AudioDescriptor(descriptorLength, identifier);
    }

    static AudioDescriptor fromParcel(Parcel in) {
      // We had read the first int of spliceDescriptorTag.
      return new AudioDescriptor(in.readInt(), in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest) {
      super.writeToParcel(dest);
      // More fields.
    }
  }
}
