package fr.edu.lyon.nuxeo.onlyoffice.service;

import java.security.Principal;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

import fr.edu.lyon.onlyoffice.api.FileUtility;

/**
 * Données associées à un document en création
 * @author ftorchet
 *
 */
public class OnlyOfficeCreateDocument extends AbstractOnlyOfficeDocument
{
	private static final String	SITE_ONLYOFFICE_TEMPLATE	= "site/onlyoffice/template/";
	private static final String	SITE_ONLYOFFICE_CALLBACK	= "site/onlyoffice/callbackCreate/";

	private static final String	FILETYPE_OPTION				= "filetype";
	private static final String	FILENAME_OPTION				= "filename";

	private DocumentModel		parent;

	private String				filetype;
	private String				filename;

	public OnlyOfficeCreateDocument(CoreSession session, DocumentModel document, OnlyOfficeContext context)
	{
		super(session, context);

		this.filename = (String) context.get(FILENAME_OPTION);
		this.filetype = (String) context.get(FILETYPE_OPTION);

		parent = document;
	}

	@Override
	public String getFilename()
	{
		return new StringBuilder(filename).append(".").append(StringUtils.lowerCase(filetype)).toString();
	}

	@Override
	public String getDocumentType()
	{
		return FileUtility.getFileType(getFilename()).toString().toLowerCase();
	}

	@Override
	public String getFiletype()
	{
		return StringUtils.lowerCase(filetype);
	}

	@Override
	public String getKey()
	{
		Principal principal = session.getPrincipal();
		return new StringBuilder(principal.getName().replace('@', '.')).append(KEY_SEPARATOR).append(Calendar.getInstance().getTimeInMillis()).toString();
	}

	@Override
	public String getTitle()
	{
		return FileUtility.getFileNameWithoutExtension(getFilename());
	}

	@Override
	public String getAuthor()
	{
		return getRealName((NuxeoPrincipal) session.getPrincipal());
	}

	@Override
	public String getCreationDate()
	{
		return df.format(Calendar.getInstance().getTime());
	}

	@Override
	public boolean hasUserEditPermission()
	{
		return session.hasPermission(parent.getRef(), SecurityConstants.ADD_CHILDREN);
	}

	@Override
	public boolean hasSharingEditPermission()
	{
		return session.hasPermission(parent.getRef(), SecurityConstants.WRITE_SECURITY);
	}

	@Override
	public String getDocumentUrl(String sessionToken)
	{
		return new StringBuilder(context.getCallbackBaseUrl()).append(SITE_ONLYOFFICE_TEMPLATE).append(filetype)
				.append("?sessionToken=").append(sessionToken).toString();
	}

	@Override
	public String getCallbackUrl(String sessionToken)
	{
		String docId=FileUtility.getEncoded(parent.getId(),filename,filetype);
		return new StringBuilder(context.getCallbackBaseUrl()).append(SITE_ONLYOFFICE_CALLBACK).append(docId).append("?sessionToken=")
           .append(sessionToken).toString();
	}

	@Override
	public String getGobackUrl(GobackResolver gobackResolver)
	{
		return gobackResolver == null ? null : gobackResolver.getGobackUrl(context, parent);
	}
}
