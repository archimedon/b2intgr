package com.rdnsn.b2intgr.util;

import com.google.common.net.HttpHeaders;
import org.apache.camel.Exchange;

import java.nio.charset.StandardCharsets;

public interface Constants {

	public static final long KILOBYTE = 1024;
	public static final long KILOBYTE_ON_DISK = 1000;

	public static final long GIG_ON_DISK = KILOBYTE_ON_DISK^3;
	public static final long GIG    = KILOBYTE^3;

	public String UTF_8             = StandardCharsets.UTF_8.name();
	public String AUTHORIZATION     = HttpHeaders.AUTHORIZATION;
	public String CONTENT_LENGTH    = HttpHeaders.CONTENT_LENGTH;
    public String LOCATION          = HttpHeaders.LOCATION;

	public String USER_FILE         = "userFile";
	public String AUTH_RESPONSE     = "authResponse";
	
	public String X_BZ_FILE_NAME    = "X-Bz-File-Name";
	public String X_BZ_INFO_AUTHOR  = "X-Bz-Info-Author";
	public String X_BZ_CONTENT_SHA1 = "X-Bz-Content-Sha1";
	public String X_BZ_PART_NUMBER  = "X-Bz-Part-Number";

	public String TRNSNT_FILE_DESTDIR = "destDir";

	public long B2_TOKEN_TTL        = (24 * 60 * 60) - 10;
	public String DIR_PATH = "path";
}
