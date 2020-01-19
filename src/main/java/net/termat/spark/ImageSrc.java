package net.termat.spark;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public class ImageSrc {
	@DatabaseField(generatedId=true)
	public long id;

	@DatabaseField
	public long itemId;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    public byte[] imageBytes;
}
