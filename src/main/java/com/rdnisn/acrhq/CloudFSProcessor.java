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
		, uploadUrl, uploadToken, authToken
	}
	
	public static void setReply(final Exchange exchange, Verb action, Object val) {
		switch (action) {
			case authorizeService : {
//				exchange.getOut().setHeader(B2AUTHN, val);
//				exchange.getOut().setHeader("Authorization", ((B2Response)val).getAuthorizationToken());
				exchange.setProperty(B2AUTHN, val);
				exchange.getOut().setHeader("Authorization", ((B2Response)val).getAuthorizationToken());
				exchange.getOut().setHeader("Authorization", ((B2Response)val).getAuthorizationToken());
				break;
			}
			case authToken : {
				exchange.getOut().setHeader("authToken",  val);
		    		break;
		    	}
			case listBuckets : break;
			case transientUpload : {
				exchange.getOut().setHeader("locprocdata",  val);
		    		break;
		    	}
			case uploadUrl : {
				exchange.getOut().setHeader("uploadUrl", val);
				break;
			}
			case uploadToken : {
				exchange.getOut().setHeader("uploadToken", val);
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
		
		return getReply(exchange, action, Object.class);
	}
	
	public static <T> T getReply(Exchange exchange, Verb action, Class<T> type) {
		
		Object ans =  null;
		
		switch (action) {
			case authorizeService : {
//				ans = exchange.getIn().getHeader(B2AUTHN);
				ans = exchange.getProperty(B2AUTHN);
				break;
			}
			case authToken : {
				ans = exchange.getIn().getHeader("authToken");
		    		break;
		    	}
			case listBuckets : {
				ans = exchange.getIn().getHeader(B2AUTHN);
				break;
			}
			case createBucket :{
				break;
			}
			case uploadUrl : {
				ans = exchange.getIn().getHeader("uploadUrl");
				break;
			}
			case uploadToken : {
				ans = exchange.getIn().getHeader("uploadToken");
				break;
			}
			case uploadObject : {
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
		return (T) ans;
	}
	
}
