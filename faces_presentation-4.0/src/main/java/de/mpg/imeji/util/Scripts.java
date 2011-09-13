package de.mpg.imeji.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;
import javax.xml.rpc.ServiceException;

import org.apache.axis.encoding.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import de.escidoc.core.common.exceptions.EscidocException;
import de.escidoc.schemas.commontypes.x04.LinkForCreate;
import de.escidoc.schemas.components.x09.ComponentDocument.Component;
import de.escidoc.schemas.contentmodel.x01.ContentModelDocument;
import de.escidoc.schemas.item.x09.ItemDocument;
import de.mpg.escidoc.services.framework.PropertyReader;
import de.mpg.escidoc.services.framework.ProxyHelper;
import de.mpg.escidoc.services.framework.ServiceLocator;
import de.mpg.imeji.escidoc.ItemVO;
import de.mpg.imeji.upload.deposit.DepositController;
import de.mpg.jena.ImejiBean2RDF;
import de.mpg.jena.ImejiJena;
import de.mpg.jena.controller.AlbumController;
import de.mpg.jena.controller.CollectionController;
import de.mpg.jena.controller.ImageController;
import de.mpg.jena.util.ObjectHelper;
import de.mpg.jena.vo.Album;
import de.mpg.jena.vo.CollectionImeji;
import de.mpg.jena.vo.Image;
import de.mpg.jena.vo.Person;
import de.mpg.jena.vo.User;

public class Scripts 
{
	 public String copyDataFromCoreToCore(User admin) throws IOException, URISyntaxException, Exception
	 {
		 String oldCoreserviceUrl = "http://vm45.mpdl.mpg.de:80";

		 ImageController ic = new ImageController(admin);
		 
		 String userHandleOldFW = login(oldCoreserviceUrl, "faces_user", "escidoc");
		 
		 String userHandle = LoginHelper.login(PropertyReader.getProperty("imeji.escidoc.user"), PropertyReader.getProperty("imeji.escidoc.password"));
		 
		 int counter = 0;
		 
		 for (Image image : ic.retrieveAll())
		 {
			 image = (Image) ObjectHelper.castAllHashSetToList(image);
			
			 if (!image.getFullImageUrl().toString().contains(ServiceLocator.getFrameworkUrl()))
			 {
				 counter++;
				 try
				 {
					 GetMethod getImage = loadImage(oldCoreserviceUrl, image.getFullImageUrl().toString().replace("http://dev-coreservice.mpdl.mpg.de:80", oldCoreserviceUrl), userHandleOldFW);
					 
					 if (getImage.getStatusCode() == HttpServletResponse.SC_OK)
					 {
						 // Create ImteVO
						 String mimeType = getImage.getResponseHeaders("Content-Type")[0].getValue();
						 String format =   mimeType.replace("image/", "");
						 ItemVO item = DepositController.createImejiItem(getImage.getResponseBodyAsStream(), image.getFilename()
								, "", mimeType, format, userHandle, null, PropertyReader.getProperty("escidoc.faces.context.id"));
						 					
						 // Create on new Coreservice
						  String itemXml = ServiceLocator.getItemHandler(userHandle).create(item.getItemDocument().xmlText().replaceAll("escidoc:faces40", PropertyReader.getProperty("escidoc.faces.content-model.id")));
						 //System.out.println(itemXml);
						  
						  
						 // Parse Response
						 item.setItem(ItemDocument.Factory.parse(itemXml));
					     
						 for (Component c : item.getItemDocument().getItem().getComponents().getComponentArray())
						 {
							 if (c.getProperties().getContentCategory().equals(PropertyReader.getProperty("xsd.metadata.content-category.original-resolution")))
							 {
								 image.setFullImageUrl(URI.create(ServiceLocator.getFrameworkUrl() + c.getContent().getHref()));
							 }
							 if (c.getProperties().getContentCategory().equals(PropertyReader.getProperty("xsd.metadata.content-category.thumbnail")))
							 {
								 image.setThumbnailImageUrl(URI.create(ServiceLocator.getFrameworkUrl() + c.getContent().getHref()));
							 }
							 if (c.getProperties().getContentCategory().equals(PropertyReader.getProperty("xsd.metadata.content-category.web-resolution")))
							 {
								 image.setWebImageUrl(URI.create(ServiceLocator.getFrameworkUrl() + c.getContent().getHref()));
							 }
						 }
					 
						 // Update image
						 image.setEscidocId(item.getItemDocument().getItem().getObjid());
						 System.out.println("updating " + image.getId() + " (" + image.getEscidocId() + ")...");
						 ic.update(image);
						 System.out.println("...done");
					}
					else
					{
						System.out.println(getImage.getStatusText());
						if (getImage.getResponseHeaders("eSciDocException").length > 0)
						{
							System.out.println(getImage.getResponseHeaders("eSciDocException")[0].getValue());
						}
					}
				 
				 }
				 catch (Exception e) 
				 {
					System.out.println(e.getMessage());
				 }
			 }
			 else
			 {
				 System.out.println("Image already transformed : " + image.getFullImageUrl());
			 }
			 
			 if (counter > 10000) break;
			
		 }
	    		
		 return "";
	 }
	 
	 public void createContentModel() throws Exception
	 {
		 String admin = LoginHelper.login("roland", "topor");
		 
		 ContentModelDocument cmd = ContentModelDocument.Factory.parse(new File("C:\\Users\\saquet\\faces40.xml"));
		 String response = ServiceLocator.getContentModelHandler(admin).create(cmd.xmlText());
		 System.out.println(response);
	 }
	 
	 
	 public GetMethod loadImage(String frameworkUrl, String imageUrl, String userHandle) throws Exception
	 { 		
		 System.out.println("loading image: " + imageUrl);
 		 byte[] buffer = null;
         GetMethod method = new GetMethod(imageUrl);
         method.setFollowRedirects(false);
 
         method.addRequestHeader("Cookie", "escidocCookie=" + userHandle);
         method.addRequestHeader("Cache-Control", "public");
         method.setRequestHeader("Connection", "close"); 
         // Execute the method with HttpClient.
         HttpClient client = new HttpClient();
         ProxyHelper.setProxy(client, frameworkUrl);
         client.executeMethod(method);
        
         //byte[] input;
         
         InputStream input = null;
         
         OutputStream out = new FileOutputStream("tmp");
         
         if (method.getStatusCode() == 302)
         {
             //try again
             method.releaseConnection();
             //userHandle = LoginHelper.login(PropertyReader.getProperty("imeji.escidoc.user"), PropertyReader.getProperty("imeji.escidoc.password"));
             method = new GetMethod(imageUrl);
             method.setFollowRedirects(false);
             method.addRequestHeader("Cookie", "escidocCookie=" + userHandle);
             client.executeMethod(method);
             
         }
         else
         {
             //input = method.getResponseBodyAsStream();
         }
         
         return method;
	 }
	 
	 public static String login(String frameworkUrl, String userName, String password) throws Exception
		{
	        StringTokenizer tokens = new StringTokenizer( frameworkUrl, "//" );
	                
	        tokens.nextToken();
	        StringTokenizer hostPort = new StringTokenizer(tokens.nextToken(), ":");
	        
	        String host = hostPort.nextToken();
	        int port = Integer.parseInt( hostPort.nextToken() );
	        
	        HttpClient client = new HttpClient();
	        client.getHostConfiguration().setHost( host, port, "http");
	        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	        
	        PostMethod login = new PostMethod( frameworkUrl + "/aa/j_spring_security_check");
	        login.addParameter("j_username", userName);
	        login.addParameter("j_password", password);
	        
	        client.executeMethod(login);
	        //System.out.println("Login form post: " + login.getStatusLine().toString());
	                
	        login.releaseConnection();
	        CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
	        Cookie[] logoncookies = cookiespec.match(
	                host, port, "/", false, 
	                client.getState().getCookies());
	        
	        Cookie sessionCookie = logoncookies[0];
	        
	        PostMethod postMethod = new PostMethod("/aa/login");
	        postMethod.addParameter("target", frameworkUrl);
	        client.getState().addCookie(sessionCookie);
	        client.executeMethod(postMethod);
	        //System.out.println("Login second post: " + postMethod.getStatusLine().toString());
	      
	        if (HttpServletResponse.SC_SEE_OTHER != postMethod.getStatusCode())
	        {
	            throw new HttpException("Wrong status code: " + login.getStatusCode());
	        }
	        
	        String userHandle = null;
	        Header headers[] = postMethod.getResponseHeaders();
	        for (int i = 0; i < headers.length; ++i)
	        {
	            if ("Location".equals(headers[i].getName()))
	            {
	                String location = headers[i].getValue();
	                int index = location.indexOf('=');
	                userHandle = new String(Base64.decode(location.substring(index + 1, location.length())));
	                //System.out.println("location: "+location);
	                //System.out.println("handle: "+userHandle);
	            }
	        }
	        
	        if (userHandle == null)
	        {
	            throw new ServiceException("User not logged in.");
	        }
	        return userHandle;
		}
	 
	 public void setCompleteNamesForContainers(User admin) throws Exception
	 {
		 CollectionController cc = new CollectionController(admin);
		 ImejiBean2RDF imejiBean2RDF = new ImejiBean2RDF(ImejiJena.collectionModel);
			
		 for (CollectionImeji c : cc.retrieveAll())
		 {
			 c = (CollectionImeji) ObjectHelper.castAllHashSetToList(c);
			 for (int i = 0; i < c.getMetadata().getPersons().size(); i++) 
			 {
				 ((List<Person>)c.getMetadata().getPersons()).get(i).setCompleteName(
						 	((List<Person>)c.getMetadata().getPersons()).get(i).getFamilyName() + " " 
						 	+ ((List<Person>)c.getMetadata().getPersons()).get(i).getGivenName());
				 System.out.println(((List<Person>)c.getMetadata().getPersons()).get(i).getCompleteName());
			 }
			 imejiBean2RDF.saveDeep(imejiBean2RDF.toList(c), admin);
		 }
		 
		 cc.cleanGraph(ImejiJena.collectionModel);
		 AlbumController ac = new AlbumController(admin);
		 imejiBean2RDF = new ImejiBean2RDF(ImejiJena.albumModel);
		 
		 for (Album a : ac.retrieveAll())
		 {
			 a = (Album) ObjectHelper.castAllHashSetToList(a);
			 for (int i = 0; i < a.getMetadata().getPersons().size(); i++) 
			 {
				 ((List<Person>)a.getMetadata().getPersons()).get(i).setCompleteName(
						 	((List<Person>)a.getMetadata().getPersons()).get(i).getFamilyName() + " " 
						 	+ ((List<Person>)a.getMetadata().getPersons()).get(i).getGivenName()); 
			 }
			 imejiBean2RDF.saveDeep(imejiBean2RDF.toList(a), admin);
		 }
		 ac.cleanGraph(ImejiJena.albumModel);
	 }
}
