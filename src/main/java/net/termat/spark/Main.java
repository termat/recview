package net.termat.spark;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import spark.ModelAndView;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;
public class Main {

	public static void main(String[] args) {
		Optional<String> optionalPort = Optional.ofNullable(System.getenv("PORT"));
		optionalPort.ifPresent(p -> {
			int port = Integer.parseInt(p);
			Spark.port(port);
		});
		staticFileLocation("/public");
		ImageDB db=new ImageDB();
		try{
			db.connectSqlite("images.db", true);
//			String dbUrl = System.getenv("JDBC_DATABASE_URL");
//			db.connectPostgreSql(dbUrl, true);
		}catch(Exception e){
			e.printStackTrace();
		}
		get("/", (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            List<ImageData> list=db.getData();
            DateFormat df=DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            for(ImageData id : list){
            	id.pass=df.format(id.date);
            }
            model.put("list",list);
            return new ModelAndView(model, "top.mustache");
        }, new MustacheTemplateEngine());

		get("/vr/:param", (request, response) -> {
	           Map<String, Object> model = new HashMap<>();
			Map<String,String> map=paramMap(request.params("param"));
    		if(map.containsKey("id")){
        		String pf=map.get("id");
        		long pid=Long.parseLong(pf);
        		String pass=map.get("pass");
        		model.put("id", pid);
        		model.put("pass", pass);
	            return new ModelAndView(model, "vr.mustache");
    		}else{
	       		response.status(404);
	            return new ModelAndView(model, "404.mustache");
    		}
        }, new MustacheTemplateEngine());

		post("/upload", "multipart/form-data", (request, response) -> {
			String location = "image";
			long maxFileSize = 200000000;
			long maxRequestSize = 200000000;
			int fileSizeThreshold = 1024;
			MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
			     location, maxFileSize, maxRequestSize, fileSizeThreshold);
			request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
			String title=request.raw().getParameter("title").toString();
			String pass=request.raw().getParameter("pass").toString();
			Part file=request.raw().getPart("img");
			try (final InputStream in = file.getInputStream()) {
				BufferedImage img=ImageIO.read(in);
				db.addImage(img, title, pass);
			}
			multipartConfigElement = null;
			file = null;
			return "OK";
		});

		get("/image/:param", (request, response) -> {
			Map<String,String> map=paramMap(request.params("param"));
    		if(map.containsKey("id")){
        		String pf=map.get("id");
        		long pid=Long.parseLong(pf);
        		String pass=map.get("pass");
        		byte[] bb=db.getImage(pid,pass);
        		if(bb!=null){
    	        	response.status(200);
    	        	response.raw().setContentType("image/jpeg");
    	        	try (OutputStream out = response.raw().getOutputStream()) {
    	        		out.write(bb, 0, bb.length);
    	        		out.flush();
    	        		out.close();
    	        	 }
    	        	return response;
        		}else{
    	       		response.status(404);
    	    		return "<html><body><h1>404 Not Found</h1></body></html>";
        		}
    		}else{
	       		response.status(404);
	    		return "<html><body><h1>404 Not Found</h1></body></html>";
    		}
        });

		get("/delete/:param", (request, response) -> {
			Map<String,String> map=paramMap(request.params("param"));
    		if(map.containsKey("id")){
        		String pf=map.get("id");
        		long pid=Long.parseLong(pf);
        		String pass=map.get("pass");
        		db.deleteImage(pid, pass);
	       		response.status(200);
	    		return true;
    		}else{
	       		response.status(404);
	    		return false;
    		}
        });
	}

	private static Map<String,String> paramMap(String param) throws Exception{
    	Map<String,String> ret=new HashMap<String,String>();
    	String[] p=param.split("&");
    	for(int i=0;i<p.length;i++){
    		String[] k=p[i].split("=");
    		if(k.length<2)continue;
    		ret.put(k[0], k[1]);
    	}
    	return ret;
    }
}
