package net.termat.spark;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class ImageDB {
	private ConnectionSource connectionSource = null;
	private Dao<ImageData,Long> imgDao;
	private Dao<ImageSrc,Long> srcDao;

	/**
	 * データベース接続
	 *
	 * @param dbName DBのパス
	 * @param create DBの自動生成の有無
	 * @throws SQLException
	 */
	public void connectDB(String dbName,boolean create) throws SQLException{
		try{
			if(!dbName.endsWith(".db"))dbName=dbName+".db";
			Class.forName("org.sqlite.JDBC");
			connectionSource = new JdbcConnectionSource("jdbc:sqlite:"+dbName);
			imgDao= DaoManager.createDao(connectionSource, ImageData.class);
			if(create)TableUtils.createTableIfNotExists(connectionSource, ImageData.class);
			srcDao= DaoManager.createDao(connectionSource, ImageSrc.class);
			if(create)TableUtils.createTableIfNotExists(connectionSource, ImageSrc.class);
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * BufferedImageをbyte[]に変換
	 * @param img BufferedImage
	 * @param ext 画像の拡張子
	 * @return byte[]
	 * @throws IOException
	 */
	public static byte[] bi2Bytes(BufferedImage img,String ext)throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write( img, ext, baos );
		baos.flush();
		return baos.toByteArray();
	}

	/**
	 * byte[]をBufferedImageに変換
	 * @param raw byte[]
	 * @return BufferedImage
	 * @throws IOException
	 */
	public static BufferedImage bytes2Bi(byte[] raw)throws IOException{
		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
		BufferedImage img=ImageIO.read(bais);
		return img;
	}

	public void addImage(BufferedImage img,String title,String pass)throws Exception{
		ImageData data=new ImageData();
		ImageSrc src=new ImageSrc();
		data.itemId=System.currentTimeMillis();
		src.itemId=data.itemId;
		data.title=title;
		data.pass=pass;
		data.date=new Date(System.currentTimeMillis());
		data.thum=createThum(img);
		src.imageBytes=bi2Bytes(img,"jpg");
		imgDao.createIfNotExists(data);
		srcDao.createIfNotExists(src);
	}

	private static String createThum(BufferedImage img)throws IOException{
		double scale=200.0/(double)img.getWidth();
		AffineTransform af=AffineTransform.getScaleInstance(scale, scale);
		BufferedImage after = new BufferedImage(200,(int)(img.getHeight()*scale), img.getType());
		AffineTransformOp scaleOp =new AffineTransformOp(af, AffineTransformOp.TYPE_BILINEAR);
		after = scaleOp.filter(img,after);
		return Base64.getEncoder().encodeToString(bi2Bytes(after,"jpg"));
	}

	public List<ImageData> getData()throws Exception{
		return imgDao.queryForAll();
	}

	public byte[] getImage(long id,String pass)throws Exception{
		ImageData im=imgDao.queryForId(id);
		if(im==null)return null;
		if(im.pass==null||im.pass.isEmpty()||im.pass.equals(pass)){
			return getImage(im.itemId);
		}else{
			return null;
		}
	}

	public byte[] getImage(long itemId)throws Exception{
		QueryBuilder<ImageSrc, Long> query=srcDao.queryBuilder();
		query.where().eq("itemId", itemId);
		List<ImageSrc> list=srcDao.query(query.prepare());
		if(list.size()>0){
//			BufferedImage im=bytes2Bi(list.get(0).imageBytes);
			return list.get(0).imageBytes;
		}else{
			return null;
		}
	}

	public void deleteImage(long id,String pass)throws Exception{
		ImageData im=imgDao.queryForId(id);
		if(im.pass==null||im.pass.isEmpty()||im.pass.equals(pass)){
			imgDao.delete(im);
			QueryBuilder<ImageSrc, Long> query=srcDao.queryBuilder();
			query.where().eq("itemId", im.itemId);
			List<ImageSrc> list2=srcDao.query(query.prepare());
			if(list2.size()>0){
				srcDao.delete(list2.get(0));
			}
		}
	}
}
