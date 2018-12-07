package fr.edu.lyon.nuxeo.onlyoffice.view;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

import fr.edu.lyon.nuxeo.onlyoffice.util.OnlyOfficeUserHelper;
import fr.edu.lyon.onlyoffice.api.FileUtility;

@Name("onlyOfficeActions")
@Scope(ScopeType.PAGE)
public class OnlyOfficeActionsBean implements Serializable
{
	private static final long serialVersionUID = 3974852079862380294L;

	@In(create = true, required = false) protected transient NavigationContext	navigationContext;
	@In(create = true, required = false) protected transient CoreSession		documentManager;

	private enum Mode{
		VIEW, EDIT, COEDIT;

		@Override
		public String toString()
		{
			return name().toLowerCase();
		}
	}

	private String getLink(Mode mode)
	{
		return getLink(mode, navigationContext.getCurrentDocument());
	}

	private String getLink(Mode mode, DocumentModel document)
	{
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		return new StringBuilder(VirtualHostHelper.getBaseURL(request)).append("site/onlyoffice/").append(isEditable() ? mode.toString() : Mode.VIEW.toString()).append("/")
				.append(document.getId()).toString();
	}

	public String getEditLink()
	{
		return getLink(Mode.EDIT);
	}

	public String getCoEditLink()
	{
		return getLink(Mode.COEDIT);
	}

	public String getCoEditLink(DocumentModel document)
	{
		return getLink(Mode.COEDIT, document);
	}

	public String getPreviewLink()
	{
		return getLink(Mode.VIEW);
	}

	public boolean isEditable()
	{
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		return documentManager.hasPermission(currentDocument.getRef(), SecurityConstants.WRITE);
	}

	private Blob getDocumentBlob(DocumentModel document)
	{
		if (document==null)
		{
			return null;
		}

		BlobHolder bh = document.getAdapter(BlobHolder.class);

		if (bh!=null)
		{
			return bh.getBlob();
		}

		return null;
	}

	public boolean isManaged()
	{
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		if (currentDocument==null)
		{
			return false;
		}

		Blob blob=getDocumentBlob(currentDocument);
		boolean isFile = !currentDocument.hasFacet("Folderish") && blob!=null && blob.getFilename() != null;

		if (isFile)
		{
			return FileUtility.isManaged(blob.getFilename());
		}

		return false;
	}

              public boolean canManaged(DocumentModel doc)
        {
               
                if (doc==null)
                {
                        return false;
                }

                Blob blob=getDocumentBlob(doc);
                boolean isFile = !doc.hasFacet("Folderish") && blob!=null && blob.getFilename() != null;

                if (isFile)
                {
                        return FileUtility.isManaged(blob.getFilename());
                }

                return false;
        }




	public boolean needConfirmation()
	{
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		if (currentDocument==null)
		{
			return false;
		}

		return !FileUtility.isValidMimetype(getDocumentBlob(currentDocument));
	}

	public boolean isAvailableForUser()
	{
		return OnlyOfficeUserHelper.canUseOnlyOffice(documentManager);
	}

	private static ServletRequest getRequest() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }
        return (ServletRequest) facesContext.getExternalContext().getRequest();
    }

	public String getCreateUrl()
	{
		DocumentModel parentDocument = navigationContext.getCurrentDocument();
		DocumentModel currentDocument = navigationContext.getChangeableDocument();

		String title=currentDocument.getTitle();
        String filetype = currentDocument.getType().toLowerCase();
        String filename = title + "." + filetype;

        try { 
            Blob blob = Blobs.createBlob(getClass().getResourceAsStream("/templates/empty." + filetype));
            blob.setFilename(filename);
            blob.setMimeType(FileUtility.getOnlyofficeMimeType(filename));

            DocumentModel documentModel=documentManager.createDocumentModel(parentDocument.getPathAsString(), filename, "File");
            documentModel.setPropertyValue("dc:title", title);
            documentModel.getAdapter(BlobHolder.class).setBlob(blob);
            DocumentModel createdDoc = documentManager.createDocument(documentModel);

            return VirtualHostHelper.getBaseURL(getRequest()) + "site/onlyoffice/coedit/" + createdDoc.getId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}

}
