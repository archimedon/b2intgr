package com.rdnisn.acrhq;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.InputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;

public abstract class CloudFSProcessor implements Processor {
	
    protected static Logger log = LoggerFactory.getLogger(CloudFSProcessor.class);

	public static final String B2AUTHN = "B2AUTHN";

	public enum Verb {
		uploadObject, deleteObject, deleteBucket, createBucket, listBuckets, authorizeService, transientUpload
	}
	
	public static void setReply(final Exchange exchange, Verb action, Object val) {
		switch (action) {
			case authorizeService : {
//				exchange.getOut().setHeader(B2AUTHN, val);
//				exchange.getOut().setHeader("Authorization", ((B2Response)val).getAuthorizationToken());
				exchange.setProperty(B2AUTHN, val);
				exchange.getOut().setHeader("Authorization", ((B2Response)val).getAuthorizationToken());
				break;
			}
			case listBuckets : break;
			case transientUpload : {
		    	log.debug("val " + val);

				exchange.getOut().setHeader("locprocdata",  val);
		    		break;
		    	}
			case createBucket : break;
			case uploadObject : break;
		case deleteBucket:
			break;
		case deleteObject:
			break;
		default:
			break;
		}
	}

	public static Object getReply(Exchange exchange, Verb action) {
		Object ans = null;
		
		switch (action) {
			case authorizeService : {
//				ans = exchange.getIn().getHeader(B2AUTHN);
				ans = exchange.getProperty(B2AUTHN);
				break;
			}
			case listBuckets : {
				ans = exchange.getOut().getHeader(B2AUTHN);
				break;
			}
			case createBucket :{
				ans = exchange.getOut().getHeader(B2AUTHN);
				break;
			}
			case uploadObject : {
				ans = exchange.getOut().getHeader(B2AUTHN);
				break;
			}
			case deleteBucket:
				break;
			case deleteObject:
				break;
			case transientUpload: {
				ans = exchange.getIn().getHeader("locprocdata");
				break;
			}

			default:
				break;
		}
		return ans;
	}
	
	public UploadData saveLocally(Message messageIn){

        MediaType mediaType = messageIn.getHeader(Exchange.CONTENT_TYPE, MediaType.class);
        String filePath = messageIn.getHeader("filePath", String.class).replaceAll("-","/");
        
        log.debug("filepAth: " + filePath);
        InputRepresentation representation =
            new InputRepresentation(messageIn.getBody(InputStream.class), mediaType);

		UploadData uploadData = null;
		
        try {
            List<FileItem> items = 
                new RestletFileUpload( new DiskFileItemFactory()).parseRepresentation(representation);

            if (! items.isEmpty()) {
            	
            		uploadData = new UploadData();
            	
            		for (FileItem item : items) {
            			if (item.isFormField()) {
            				uploadData.putFormField(item.getFieldName(), item.getString());
            			}
            			else {
            				Path destination = Paths.get(filePath, item.getName());
            				Files.createDirectories(destination.getParent());
    	                		Files.copy(item.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    	                		UserFile uf = new UserFile(destination, item.getFieldName());
            				uploadData.addFile(uf);
            			}
            		}
            }
        } catch (FileUploadException | IOException e) {
            e.printStackTrace();
        }
        return uploadData;

    }
	
}
