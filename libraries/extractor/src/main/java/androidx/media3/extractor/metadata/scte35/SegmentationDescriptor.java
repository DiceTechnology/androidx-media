package androidx.media3.extractor.metadata.scte35;

import android.os.Parcel;
import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;

/** A {@link SpliceDescriptor} that defines a segmentation descriptor. */
public class SegmentationDescriptor extends SpliceDescriptor {

  public enum SegmentationType {
    NOT_INDICATED(0x00),
    CONTENT_IDENTIFICATION(0x01),
    PROGRAM_START(0x10),
    PROGRAM_END(0x11),
    PROGRAM_EARLY_TERMINATION(0x12),
    PROGRAM_BREAKAWAY(0x13),
    PROGRAM_RESUMPTION(0x14),
    PROGRAM_RUNOVER_PLANNED(0x15),
    PROGRAM_RUNOVER_UNPLANNED(0x16),
    PROGRAM_OVERLAP_START(0x17),
    PROGRAM_BLACKOUT_OVERRIDE(0x18),
    PROGRAM_JOIN(0x19),
    CHAPTER_START(0x20),
    CHAPTER_END(0x21),
    BREAK_START(0x22),
    BREAK_END(0x23),
    PROVIDER_ADVERTISEMENT_START(0x30),
    PROVIDER_ADVERTISEMENT_END(0x31),
    DISTRIBUTOR_ADVERTISEMENT_START(0x32),
    DISTRIBUTOR_ADVERTISEMENT_END(0x33),
    PROVIDER_PLACEMENT_OPPORTUNITY_START(0x34),
    PROVIDER_PLACEMENT_OPPORTUNITY_END(0x35),
    DISTRIBUTOR_PLACEMENT_OPPORTUNITY_START(0x36),
    DISTRIBUTOR_PLACEMENT_OPPORTUNITY_END(0x37),
    PROVIDER_OVERLAY_PLACEMENT_OPPORTUNITY_START(0x38),
    PROVIDER_OVERLAY_PLACEMENT_OPPORTUNITY_END(0x39),
    DISTRIBUTOR_OVERLAY_PLACEMENT_OPPORTUNITY_START(0x3A),
    DISTRIBUTOR_OVERLAY_PLACEMENT_OPPORTUNITY_END(0x3B),
    PROVIDER_PROMO_START(0x3C),
    PROVIDER_PROMO_END(0x3D),
    DISTRIBUTOR_PROMO_START(0x3E),
    DISTRIBUTOR_PROMO_END(0x3F),
    UNSCHEDULED_EVENT_START(0x40),
    UNSCHEDULED_EVENT_END(0x41),
    ALTERNATE_CONTENT_OPPORTUNITY_START(0x42),
    ALTERNATE_CONTENT_OPPORTUNITY_END(0x43),
    PROVIDER_AD_BLOCK_START(0x44),
    PROVIDER_AD_BLOCK_END(0x45),
    DISTRIBUTOR_AD_BLOCK_START(0x46),
    DISTRIBUTOR_AD_BLOCK_END(0x47),
    NETWORK_START(0x50),
    NETWORK_END(0x51);

    private final int id;

    SegmentationType(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public static SegmentationType from(int id) {
      if (id >= 0) {
        for (SegmentationType type : SegmentationType.values()) {
          if (type.getId() == id) {
            return type;
          }
        }
      }
      return null;
    }
  }

  public final long segmentationEventId;
  public final boolean segmentationEventCancelIndicator;
  public final boolean programSegmentationFlag;
  public final boolean segmentationDurationFlag;
  public final boolean deliveryNotRestrictedFlag;
  public final boolean webDeliveryAllowedFlag;
  public final boolean noRegionalBlackoutFlag;
  public final boolean archiveAllowedFlag;
  public final long segmentationDurationUs;
  public final int segmentationUpidType;
  public final int segmentationUpidLength;
  public final byte[] segmentationUpid;
  public final SegmentationType segmentationType;
  public final int segmentNum;
  public final int segmentsExpected;
  public final int subSegmentNum;
  public final int subSegmentsExpected;

  private SegmentationDescriptor(
      int descriptorLength,
      String identifier,
      long segmentationEventId,
      boolean segmentationEventCancelIndicator,
      boolean programSegmentationFlag,
      boolean segmentationDurationFlag,
      boolean deliveryNotRestrictedFlag,
      boolean webDeliveryAllowedFlag,
      boolean noRegionalBlackoutFlag,
      boolean archiveAllowedFlag,
      long segmentationDurationUs,
      int segmentationUpidType,
      int segmentationUpidLength,
      byte[] segmentationUpid,
      SegmentationType segmentationType,
      int segmentNum,
      int segmentsExpected,
      int subSegmentNum,
      int subSegmentsExpected) {
    super(DescriptorType.SEGMENTATION_DESCRIPTOR, descriptorLength, identifier);
    this.segmentationEventId = segmentationEventId;
    this.segmentationEventCancelIndicator = segmentationEventCancelIndicator;
    this.programSegmentationFlag = programSegmentationFlag;
    this.segmentationDurationFlag = segmentationDurationFlag;
    this.deliveryNotRestrictedFlag = deliveryNotRestrictedFlag;
    this.webDeliveryAllowedFlag = webDeliveryAllowedFlag;
    this.noRegionalBlackoutFlag = noRegionalBlackoutFlag;
    this.archiveAllowedFlag = archiveAllowedFlag;
    this.segmentationDurationUs = segmentationDurationUs;
    this.segmentationUpidType = segmentationUpidType;
    this.segmentationUpidLength = segmentationUpidLength;
    this.segmentationUpid = segmentationUpid;
    this.segmentationType = segmentationType;
    this.segmentNum = segmentNum;
    this.segmentsExpected = segmentsExpected;
    this.subSegmentNum = subSegmentNum;
    this.subSegmentsExpected = subSegmentsExpected;
  }

  static SegmentationDescriptor parseFromSection(ParsableByteArray sectionData, int descriptorLength) {
    final int sectionEndPosition = sectionData.getPosition() + descriptorLength;
    String identifier = sectionData.readString(4);

    long segmentationEventId = sectionData.readUnsignedInt();
    boolean segmentationEventCancelIndicator = (sectionData.readUnsignedByte() & 0x80) != 0;
    boolean programSegmentationFlag = false;
    boolean segmentationDurationFlag = false;
    boolean deliveryNotRestrictedFlag = false;
    boolean webDeliveryAllowedFlag = false;
    boolean noRegionalBlackoutFlag = false;
    boolean archiveAllowedFlag = false;
    long segmentationDurationUs = C.TIME_UNSET;
    int segmentationUpidType = 0;
    int segmentationUpidLength = 0;
    byte[] segmentationUpid = new byte[0];
    SegmentationType segmentationType = SegmentationType.NOT_INDICATED;
    int segmentNum = 0;
    int segmentsExpected = 0;
    int subSegmentNum = 0;
    int subSegmentsExpected = 0;
    if (!segmentationEventCancelIndicator) {
      int headerByte = sectionData.readUnsignedByte();
      programSegmentationFlag = (headerByte & 0x80) != 0;
      segmentationDurationFlag = (headerByte & 0x40) != 0;
      deliveryNotRestrictedFlag = (headerByte & 0x20) != 0;
      if (!deliveryNotRestrictedFlag) {
        webDeliveryAllowedFlag = (headerByte & 0x10) != 0;
        noRegionalBlackoutFlag = (headerByte & 0x08) != 0;
        archiveAllowedFlag = (headerByte & 0x04) != 0;
        // device_restrictions(2)
      }

      if (!programSegmentationFlag) {
        int componentCount = sectionData.readUnsignedByte();
        // component_tag(8), reserved(7), pts_offset(33)
        sectionData.skipBytes(componentCount * 6);
      }

      if (segmentationDurationFlag) {
        long firstByte = sectionData.readUnsignedByte();
        long segmentationDuration90khz = ((firstByte & 0x01) << 32) | sectionData.readUnsignedInt();
        segmentationDurationUs = segmentationDuration90khz * 1000 / 90;
      }

      segmentationUpidType = sectionData.readUnsignedByte();
      segmentationUpidLength = sectionData.readUnsignedByte();
      segmentationUpid = new byte[segmentationUpidLength];
      sectionData.readBytes(segmentationUpid, 0, segmentationUpidLength);

      segmentationType = SegmentationType.from(sectionData.readUnsignedByte());
      segmentNum = sectionData.readUnsignedByte();
      segmentsExpected = sectionData.readUnsignedByte();
      int bytesLeft = sectionEndPosition - sectionData.getPosition();
      if (bytesLeft >= 2
          && (segmentationType == SegmentationType.PROVIDER_PLACEMENT_OPPORTUNITY_START
          || segmentationType == SegmentationType.DISTRIBUTOR_PLACEMENT_OPPORTUNITY_START
          || segmentationType == SegmentationType.PROVIDER_OVERLAY_PLACEMENT_OPPORTUNITY_START
          || segmentationType == SegmentationType.DISTRIBUTOR_OVERLAY_PLACEMENT_OPPORTUNITY_START)) {
        subSegmentNum = sectionData.readUnsignedByte();
        subSegmentsExpected = sectionData.readUnsignedByte();
      }
    }

    skipBytes(sectionEndPosition, sectionData);
    return new SegmentationDescriptor(
        descriptorLength,
        identifier,
        segmentationEventId,
        segmentationEventCancelIndicator,
        programSegmentationFlag,
        segmentationDurationFlag,
        deliveryNotRestrictedFlag,
        webDeliveryAllowedFlag,
        noRegionalBlackoutFlag,
        archiveAllowedFlag,
        segmentationDurationUs,
        segmentationUpidType,
        segmentationUpidLength,
        segmentationUpid,
        segmentationType,
        segmentNum,
        segmentsExpected,
        subSegmentNum,
        subSegmentsExpected);
  }

  static SegmentationDescriptor fromParcel(Parcel in) {
    // We had read the first int of spliceDescriptorTag.
    return new SegmentationDescriptor(
        in.readInt(),
        in.readString(),
        in.readLong(),
        in.readByte() == 1,
        in.readByte() == 1,
        in.readByte() == 1,
        in.readByte() == 1,
        in.readByte() == 1,
        in.readByte() == 1,
        in.readByte() == 1,
        in.readLong(),
        in.readInt(),
        in.readInt(),
        Util.castNonNull(in.createByteArray()),
        SegmentationType.from(in.readInt()),
        in.readInt(),
        in.readInt(),
        in.readInt(),
        in.readInt());
  }

  @Override
  public void writeToParcel(Parcel dest) {
    super.writeToParcel(dest);
    // More fields.
    dest.writeLong(segmentationEventId);
    dest.writeByte((byte) (segmentationEventCancelIndicator ? 1 : 0));
    dest.writeByte((byte) (programSegmentationFlag ? 1 : 0));
    dest.writeByte((byte) (segmentationDurationFlag ? 1 : 0));
    dest.writeByte((byte) (deliveryNotRestrictedFlag ? 1 : 0));
    dest.writeByte((byte) (webDeliveryAllowedFlag ? 1 : 0));
    dest.writeByte((byte) (noRegionalBlackoutFlag ? 1 : 0));
    dest.writeByte((byte) (archiveAllowedFlag ? 1 : 0));
    dest.writeLong(segmentationDurationUs);
    dest.writeInt(segmentationUpidType);
    dest.writeInt(segmentationUpidLength);
    dest.writeByteArray(segmentationUpid);
    dest.writeInt(segmentationType.getId());
    dest.writeInt(segmentNum);
    dest.writeInt(segmentsExpected);
    dest.writeInt(subSegmentNum);
    dest.writeInt(subSegmentsExpected);
  }
}
