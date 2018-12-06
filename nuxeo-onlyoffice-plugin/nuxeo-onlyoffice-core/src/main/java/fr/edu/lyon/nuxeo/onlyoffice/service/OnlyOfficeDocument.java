package fr.edu.lyon.nuxeo.onlyoffice.service;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Données associées au document à éditer dans un environnement OnlyOffice
 * @author ftorchet
 *
 */
public interface OnlyOfficeDocument
{
	/**
	 * Utilisateur associé à la connexion au référentiel
	 * @return
	 */
	NuxeoPrincipal getSessionPrincipal();

	/**
	 * Type du fichier édité
	 * @return
	 */
	String getFiletype();

	/**
	 * Nom du fichier édité
	 * @return
	 */
	String getFilename();

	/**
	 * Clé associé au document
	 * @return
	 */
	String getKey();

	/**
	 * Titre du document
	 * @return
	 */
	String getTitle();

	/**
	 * Auteur du document
	 * @return
	 */
	String getAuthor();

	/**
	 * DocumentType associé au document (text, spreadsheet, presentation)
	 * @return
	 */
	String getDocumentType();

	/**
	 * Date de création du document
	 * @return
	 */
	String getCreationDate();

	/**
	 * Le document peut-il être ouvert en édition
	 * @return
	 */
	boolean hasUserEditPermission();

	/**
	 * Les droits sur le document peuvent-ils être modifiés
	 * @return
	 */
	boolean hasSharingEditPermission();

	/**
	 * URL d'accès au document avec intégration d'un token JWT
	 * @param sessionToken
	 * @return
	 */
	String getDocumentUrl(String sessionToken);

	/**
	 * URL de tracking
	 * @return
	 */
	String getCallbackUrl(String sessionToken);

	/**
	 * Le mode édition est-il requis ?
	 * @return
	 */
	boolean isEditModeEnabled();

	/**
	 * URL de retour vers l'application appelante
	 * @param gobackResolver
	 * @return
	 */
	String getGobackUrl(GobackResolver gobackResolver);

	/**
	 * Le mode autosave est-il requis ?
	 * @return
	 */
	boolean isAutosaveEnabled();

}