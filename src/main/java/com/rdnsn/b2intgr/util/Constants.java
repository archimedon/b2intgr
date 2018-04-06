package com.rdnsn.b2intgr.util;

import com.google.common.net.HttpHeaders;
import org.apache.camel.Exchange;

import java.nio.charset.StandardCharsets;

public interface Constants {

	long KILOBYTE = 1024;
	long KILOBYTE_ON_DISK = 1000;

	long GIG_ON_DISK = KILOBYTE_ON_DISK^3;
	long GIG    = KILOBYTE^3;

    String UTF_8             = StandardCharsets.UTF_8.name();
    String AUTHORIZATION     = HttpHeaders.AUTHORIZATION;
    String CONTENT_LENGTH    = HttpHeaders.CONTENT_LENGTH;
    String LOCATION          = HttpHeaders.LOCATION;

    String USER_FILE         = "userFile";
    String AUTH_RESPONSE     = "authResponse";

    String X_BZ_FILE_NAME    = "X-Bz-File-Name";
    String X_BZ_INFO_AUTHOR  = "X-Bz-Info-Author";
    String X_BZ_CONTENT_SHA1 = "X-Bz-Content-Sha1";
    String X_BZ_PART_NUMBER  = "X-Bz-Part-Number";

	String TRNSNT_FILE_DESTDIR = "destDir";

    long B2_TOKEN_TTL        = (24 * 60 * 60) - 10;

    String[] B2_BUCKET_TYPES = new String[]{"allPrivate", "allPublic"};


	int ENOENT = 2;        /* No such file or directory */
	int EACCES = 13;       /* Permission denied */
	int EFAULT = 14;       /* Bad address */
	int EBUSY = 16;        /* Device or resource busy */
	int ENOTCONN = 107;    /* Transport endpoint is not connected */
	int ETIMEDOUT = 110;   /* Connection timed out */
	int EHOSTDOWN = 112;   /* Host is down */

}
