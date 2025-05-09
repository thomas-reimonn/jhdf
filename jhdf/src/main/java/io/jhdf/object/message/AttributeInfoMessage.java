/*
 * This file is part of jHDF. A pure Java library for accessing HDF5 files.
 *
 * https://jhdf.io
 *
 * Copyright (c) 2025 James Mudd
 *
 * MIT License see 'LICENSE' file
 */
package io.jhdf.object.message;

import io.jhdf.BufferBuilder;
import io.jhdf.Constants;
import io.jhdf.Superblock;
import io.jhdf.Utils;
import io.jhdf.exceptions.HdfException;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * <p>
 * Attribute Info Message
 * </p>
 *
 * <p>
 * <a href=
 * "https://support.hdfgroup.org/HDF5/doc/H5.format.html#AinfoMessage">Format
 * Spec</a>
 * </p>
 *
 * @author James Mudd
 */
public class AttributeInfoMessage extends Message {

	public static final int MESSAGE_TYPE = 21;

	private static final int MAXIMUM_CREATION_INDEX_PRESENT = 0;
	private static final int ATTRIBUTE_CREATION_ORDER_PRESENT = 1;

	private final int maximumCreationIndex;
	private final long fractalHeapAddress;
	private final long attributeNameBTreeAddress;
	private final long attributeCreationOrderBTreeAddress;

	/* package */ AttributeInfoMessage(ByteBuffer bb, Superblock sb, BitSet messageFlags) {
		super(messageFlags);

		final byte version = bb.get();
		if (version != 0) {
			throw new HdfException("Unrecognized version " + version);
		}

		BitSet flags = BitSet.valueOf(new byte[]{bb.get()});

		if (flags.get(MAXIMUM_CREATION_INDEX_PRESENT)) {
			maximumCreationIndex = Utils.readBytesAsUnsignedInt(bb, 2);
		} else {
			maximumCreationIndex = -1;
		}

		fractalHeapAddress = Utils.readBytesAsUnsignedLong(bb, sb.getSizeOfOffsets());

		attributeNameBTreeAddress = Utils.readBytesAsUnsignedLong(bb, sb.getSizeOfOffsets());

		if (flags.get(ATTRIBUTE_CREATION_ORDER_PRESENT)) {
			attributeCreationOrderBTreeAddress = Utils.readBytesAsUnsignedLong(bb, sb.getSizeOfOffsets());
		} else {
			attributeCreationOrderBTreeAddress = -1;
		}
	}

	private AttributeInfoMessage(BitSet flags, int maximumCreationIndex, long fractalHeapAddress, long attributeNameBTreeAddress, long attributeCreationOrderBTreeAddress) {
		super(flags);
		this.maximumCreationIndex = maximumCreationIndex;
		this.fractalHeapAddress = fractalHeapAddress;
		this.attributeNameBTreeAddress = attributeNameBTreeAddress;
		this.attributeCreationOrderBTreeAddress = attributeCreationOrderBTreeAddress;
	}

	public int getMaximumCreationIndex() {
		return maximumCreationIndex;
	}

	public long getFractalHeapAddress() {
		return fractalHeapAddress;
	}

	public long getAttributeNameBTreeAddress() {
		return attributeNameBTreeAddress;
	}

	public long getAttributeCreationOrderBTreeAddress() {
		return attributeCreationOrderBTreeAddress;
	}

	@Override
	public int getMessageType() {
		return MESSAGE_TYPE;
	}

	public static AttributeInfoMessage create() {
		return new AttributeInfoMessage(
			new BitSet(1),
			-1,
			Constants.UNDEFINED_ADDRESS,
			Constants.UNDEFINED_ADDRESS,
			Constants.UNDEFINED_ADDRESS
		);
	}

	@Override
	public ByteBuffer toBuffer() {
		return new BufferBuilder()
			.writeByte(0) // version
			.writeBytes(flagsToBytes()) // flags
			 // .writeShort(maximumCreationIndex) // flag not set
			.writeLong(fractalHeapAddress)
			.writeLong(attributeNameBTreeAddress)
			// .writeLong(attributeCreationOrderBTreeAddress) // flag not set
			.build();
	}
}
