package fr.edu.lyon.nuxeo.onlyoffice.service;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

import fr.edu.lyon.onlyoffice.api.FileUtility;

/**
 * Données associées à un document en édition
 * @author ftorchet
 *
 */
public class OnlyOfficeEditDocument extends AbstractOnlyOfficeDocument
{
	private DocumentModel		doc;

	private static final String	SITE_ONLYOFFICE_CALLBACK			= "site/onlyoffice/callbackEdit/";
	private static final String	SITE_ONLYOFFICE_CALLBACK_COEDITING	= "site/onlyoffice/callbackCoEdit/";
	private static final String	SITE_ONLYOFFICE_DOWNLOAD			= "site/onlyoffice/download/";

	public OnlyOfficeEditDocument(CoreSession session, DocumentModel document, OnlyOfficeContext context)
	{
		super(session, context);
		doc = document;
	}

	private boolean isCoEditionEnabled()
	{
		return isAutosaveEnabled();
	}

	@Override
	public String getFiletype()
	{
		return FileUtility.getFileExtension(getFilename()).toLowerCase().substring(1);
	}

	@Override
	public String getFilename()
	{
		BlobHolder bh = doc.getAdapter(BlobHolder.class);
		Blob blob = bh.getBlob();

		return blob.getFilename();
	}

	@Override
	public String getKey()
	{
		String version = doc.getVersionLabel();
		if(version.contains("+")){
			version = version.replace('+', '.');
			version += getModificationDateTimeStamp();
		}
		
		String prefix =  Framework.getService(OnlyofficeService.class).getConfig().getPrefix();
	
		/* 
		 * doc.getId() remplace doc.getPropertyValue('uid:uid') pour eviter la dépendance à la propriété
		 *'uid:uid' qui peut etre null 
		 */
		String uid = (String) doc.getId(); 
		
		StringBuilder key=new StringBuilder(prefix).append(uid.replace('-', '.')).append(KEY_SEPARATOR).append(version);
		if (isCoEditionEnabled())
		{
			/*
			 *  on distingue la clé pour la co-édition pour garantir la mise en cache
			 *  de l'URL de callback
			 */
			key.append(KEY_SEPARATOR).append('c');
		}

		return key.toString();
	}

	@Override
	public String getTitle()
	{
		return doc.getTitle();
	}

	@Override
	public String getAuthor()
	{
		return getRealName(getPrincipal((String) doc.getPropertyValue("dc:creator")));
	}

	@Override
	public String getDocumentType()
	{
		return FileUtility.getFileType(getFilename()).toString().toLowerCase();
	}

	@Override
	public String getCreationDate()
	{
		return df.format(((Calendar) doc.getPropertyValue("dc:created")).getTime());
	}
	
	public String getModificationDateTimeStamp()
	{
		return Long.toString(((Calendar) doc.getPropertyValue("dc:modified")).getTime().getTime());
	}
	

	@Override
	public boolean hasUserEditPermission()
	{
		return session.hasPermission(doc.getRef(), SecurityConstants.WRITE);
    }
    
	@Override
	public boolean hasSharingEditPermission()
	{
		return session.hasPermission(doc.getRef(), SecurityConstants.WRITE_SECURITY);
	}

	@Override
	public String getDocumentUrl(String sessionToken)
	{
		return new StringBuilder(context.getCallbackBaseUrl()).append(SITE_ONLYOFFICE_DOWNLOAD).append(doc.getId()).append("?sessionToken=")
				.append(sessionToken).toString();
	}

	@Override
	public String getCallbackUrl(String sessionToken)
	{
		return new StringBuilder(context.getCallbackBaseUrl()).append(isCoEditionEnabled() ? SITE_ONLYOFFICE_CALLBACK_COEDITING:SITE_ONLYOFFICE_CALLBACK).append(doc.getId()).append("?sessionToken=")
        .append(sessionToken).toString();
	}

	@Override
	public String getGobackUrl(GobackResolver gobackResolver)
	{
		return gobackResolver == null ? null : gobackResolver.getGobackUrl(context, doc);
	}
}
