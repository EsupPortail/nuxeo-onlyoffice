package fr.edu.lyon.nuxeo.onlyoffice.service;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import fr.edu.lyon.nuxeo.jwt.service.JWTPayloadPluginService;
import net.sf.json.JSONObject;

@XObject("onlyoffice")
public class OnlyofficeConfig
{
	private static final String								ONLYOFFICE_CHAT_CONFIG			= "chat";
	private static final String								ONLYOFFICE_DOCUMENT_CONFIG		= "document";
	private static final String								ONLYOFFICE_TOKEN_CONFIG			= "token";
	private static final String								ONLYOFFICE_EDITOR_CONFIG		= "editorConfig";
	private static final String								ONLYOFFICE_CUSTOMIZATION_CONFIG	= "customization";
	private static final String								ONLYOFFICE_USER_CONFIG			= "user";
	private static final String								ONLYOFFICE_GOBACK_CONFIG		= "goback";
	private static final String								ONLYOFFICE_AUTOSAVE_CONFIG		= "autosave";
	private static final String								ONLYOFFICE_USERNAME_CONFIG		= "name";
	private static final String								ONLYOFFICE_USERID_CONFIG		= "id";
	private static final String								ONLYOFFICE_MODE_CONFIG			= "mode";
	private static final String								ONLYOFFICE_LANG_CONFIG			= "lang";
	private static final String								ONLYOFFICE_CALLBACK_URL_CONFIG	= "callbackUrl";
	private static final String								ONLYOFFICE_PERMISSIONS_CONFIG	= "permissions";
	private static final String								ONLYOFFICE_PRINT_CONFIG			= "print";
	private static final String								ONLYOFFICE_EDIT_CONFIG			= "edit";
	private static final String								ONLYOFFICE_DOWNLOAD_CONFIG		= "download";
	private static final String								ONLYOFFICE_URL_CONFIG			= "url";
	private static final String								ONLYOFFICE_TITLE_CONFIG			= "title";
	private static final String								ONLYOFFICE_KEY_CONFIG			= "key";
	private static final String								ONLYOFFICE_FILETYPE_CONFIG		= "filetype";
	private static final String								ONLYOFFICE_DOCUMENT_TYPE_CONFIG	= "documentType";
	private static final String								ONLYOFFICE_INFO_CONFIG			= "info";
	private static final String								ONLYOFFICE_CREATED_CONFIG		= "created";
	private static final String								ONLYOFFICE_AUTHOR_CONFIG		= "author";


	private static Log										log								= LogFactory.getLog(OnlyofficeConfig.class);

	@XNode("algorithm") private String						algorithmId;

	@XNode("url") private String							onlyofficeUrl;
	@XNode("callbak-baseurl") private String				callbackBaseUrl;
	@XNode("prefix") private String							prefix;

	@XNode("@gobackClass") private Class<GobackResolver>	gobackresolver;

	private JWTPayloadPluginService							jwtPayloadPluginService;

	private JWTPayloadPluginService getPayloadPluginService()
	{
		if (jwtPayloadPluginService == null)
		{
			jwtPayloadPluginService = Framework.getService(JWTPayloadPluginService.class);
		}

		return jwtPayloadPluginService;
	}

	private GobackResolver getGobackResolver()
	{
		if (gobackresolver == null)
		{
			return new GobackDefaultResolver();
		}

		try
		{
			return gobackresolver.newInstance();
		} catch (InstantiationException | IllegalAccessException e)
		{
			log.error("impossible d'instancier le gobackResolver", e);
		}

		return null;
	}

	private String getToken(String payload)
	{
		return getPayloadPluginService().getSignedToken(payload, algorithmId);
	}

	private String getSessionToken(Principal principal)
	{
		return getPayloadPluginService().getSessionToken(principal, algorithmId);
	}

	public String getDocumentServerUrl()
	{
		return onlyofficeUrl;
	}

	private JSONObject getDocumentConfig(OnlyOfficeDocument onlyOfficeDocument)
	{
		JSONObject document = new JSONObject();
		document.accumulate(ONLYOFFICE_FILETYPE_CONFIG, onlyOfficeDocument.getFiletype());
		document.accumulate(ONLYOFFICE_KEY_CONFIG, onlyOfficeDocument.getKey());
		document.accumulate(ONLYOFFICE_TITLE_CONFIG, onlyOfficeDocument.getTitle());
		document.accumulate(ONLYOFFICE_URL_CONFIG, onlyOfficeDocument.getDocumentUrl(getSessionToken(onlyOfficeDocument.getSessionPrincipal())));

		document.accumulate(ONLYOFFICE_INFO_CONFIG, getInfos(onlyOfficeDocument));
		document.accumulate(ONLYOFFICE_PERMISSIONS_CONFIG, getPermissions(onlyOfficeDocument));

		return document;
	}

	private JSONObject getEditorConfig(OnlyOfficeDocument onlyOfficeDocument)
	{
		JSONObject editor = new JSONObject();
		editor.accumulate(ONLYOFFICE_CALLBACK_URL_CONFIG, onlyOfficeDocument.getCallbackUrl(getSessionToken(onlyOfficeDocument.getSessionPrincipal())));
		if (onlyOfficeDocument.hasSharingEditPermission()) {
		    // rely on apache conf: 
		    //   ProxyPass /nuxeo/embed/ ajp://localhost:8009/nuxeo/
		    // plus some tweaks to keep only the TAB_PERMISSIONS part
		    editor.accumulate("sharingSettingsUrl", onlyOfficeDocument.getGobackUrl(getGobackResolver()).replace("/nuxeo/", "/nuxeo/embed/") + "?tabIds=:TAB_PERMISSIONS");
		}
		editor.accumulate(ONLYOFFICE_LANG_CONFIG, "fr");
		editor.accumulate(ONLYOFFICE_MODE_CONFIG, onlyOfficeDocument.isEditModeEnabled() ? "edit" : "view");

		editor.accumulate(ONLYOFFICE_USER_CONFIG, getUser(onlyOfficeDocument));
		editor.accumulate(ONLYOFFICE_CUSTOMIZATION_CONFIG, getCustomization(onlyOfficeDocument));

		return editor;
	}

	private JSONObject getCustomization(OnlyOfficeDocument onlyOfficeDocument)
	{
		JSONObject customization = new JSONObject();
		customization.accumulate(ONLYOFFICE_AUTOSAVE_CONFIG, onlyOfficeDocument.isAutosaveEnabled());

		String goback;
		if (onlyOfficeDocument.isEditModeEnabled() && (goback = onlyOfficeDocument.getGobackUrl(getGobackResolver()))!=null)
		{
			JSONObject gobackObj = new JSONObject();
			gobackObj.accumulate(ONLYOFFICE_URL_CONFIG, goback);
			gobackObj.accumulate("blank", false);
			gobackObj.accumulate("text", "Aller au document");
			customization.accumulate(ONLYOFFICE_GOBACK_CONFIG, gobackObj);
		}

		// si l'autosave est activé, co-édition -> chat activé
		customization.accumulate(ONLYOFFICE_CHAT_CONFIG, onlyOfficeDocument.isAutosaveEnabled());

		return customization;
	}

	private JSONObject getUser(OnlyOfficeDocument onlyOfficeDocument)
	{
		NuxeoPrincipal principal = onlyOfficeDocument.getSessionPrincipal();

		JSONObject user = new JSONObject();
		user.accumulate(ONLYOFFICE_USERID_CONFIG, principal.getName());
		user.accumulate(ONLYOFFICE_USERNAME_CONFIG, AbstractOnlyOfficeDocument.getRealName(principal));

		return user;
	}

	private JSONObject getInfos(OnlyOfficeDocument onlyOfficeDocument)
	{
		JSONObject infos = new JSONObject();
		infos.accumulate(ONLYOFFICE_AUTHOR_CONFIG, onlyOfficeDocument.getAuthor());
		infos.accumulate(ONLYOFFICE_CREATED_CONFIG, onlyOfficeDocument.getCreationDate());

		return infos;
	}

	private JSONObject getPermissions(OnlyOfficeDocument onlyOfficeDocument)
	{
		JSONObject permissions = new JSONObject();
		permissions.accumulate(ONLYOFFICE_DOWNLOAD_CONFIG, true);
		permissions.accumulate(ONLYOFFICE_EDIT_CONFIG, onlyOfficeDocument.hasUserEditPermission());
		permissions.accumulate(ONLYOFFICE_PRINT_CONFIG, true);

		return permissions;
	}

	private String getCallbackBaseUrl(ServletRequest request)
	{
		return callbackBaseUrl==null ? getgobackBaseUrl(request):callbackBaseUrl;
	}

	private String getgobackBaseUrl(ServletRequest request)
	{
		return VirtualHostHelper.getBaseURL(request);
	}
	
	public String getPrefix() {
		return prefix;
	}

	public String getConfig(CoreSession session, ServletRequest request, DocumentModel document, Map<String, Serializable> options)
	{
		boolean create = Boolean.TRUE.equals(options.get("createMode"));

		OnlyOfficeContext context=new OnlyOfficeContext(options);
		context.addRequestParameters(request);
		context.setCallbackBaseUrl(getCallbackBaseUrl(request));
		context.setGobackBaseUrl(getgobackBaseUrl(request));

		OnlyOfficeDocument onlyOfficeDocument=create ? new OnlyOfficeCreateDocument(session, document, context):new OnlyOfficeEditDocument(session, document, context);

		JSONObject json = new JSONObject();

		json.accumulate(ONLYOFFICE_DOCUMENT_TYPE_CONFIG, onlyOfficeDocument.getDocumentType());
		json.accumulate(ONLYOFFICE_DOCUMENT_CONFIG, getDocumentConfig(onlyOfficeDocument));
		json.accumulate(ONLYOFFICE_EDITOR_CONFIG, getEditorConfig(onlyOfficeDocument));

		json.accumulate(ONLYOFFICE_TOKEN_CONFIG, getToken(json.toString()));

		return json.toString();
	}
}
