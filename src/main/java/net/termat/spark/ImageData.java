package net.termat.spark;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;

public class ImageData {

	@DatabaseField(generatedId=true)
	public long id;

	@DatabaseField
	public long itemId;

	@DatabaseField
	public String pass;

	@DatabaseField
	public Date date;

	@DatabaseField
	public String title;

	@DatabaseField
    public String thum;

}
